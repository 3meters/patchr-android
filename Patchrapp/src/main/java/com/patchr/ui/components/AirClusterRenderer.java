package com.patchr.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.patchr.R;
import com.patchr.utilities.Colors;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.ClusterRenderer;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.android.ui.SquareTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm.MAX_DISTANCE_AT_ZOOM;

/**
 * The default view for a ClusterManager. Markers are animated in and out of clusters.
 */
public class AirClusterRenderer<T extends ClusterItem> implements ClusterRenderer<T> {

	private static final int   MIN_CLUSTER_SIZE = 5;
	private static final int[] BUCKETS          = {5, 10, 20, 30, 40, 50, 100, 200, 500, 1000};

	/* If cluster size is less than this size, display individual markers. */
	private int     minClusterSize = MIN_CLUSTER_SIZE;
	private boolean animate        = true;
	private final GoogleMap         map;
	private final IconGenerator     iconGenerator;
	private final ClusterManager<T> clusterManager;
	private final float             density;
	private       ShapeDrawable     coloredCircleBackground;

	/* Markers that are currently on the map. */
	private Set<MarkerWithPosition>       markers         = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/* Icons for each bucket. */
	private SparseArray<BitmapDescriptor> icons           = new SparseArray<>();
	/* Markers for single ClusterItems.*/
	private MarkerCache<T>                markerCache     = new MarkerCache<>();
	/* Lookup between markers and the associated cluster. */
	private Map<Marker, Cluster<T>>       markerToCluster = new HashMap<>();
	private Map<Cluster<T>, Marker>       clusterToMarker = new HashMap<>();
	/* The currently displayed set of clusters. */
	private Set<? extends Cluster<T>> clusters;

	/* The target zoom level for the current set of clusters.*/
	private float zoom;

	private final ViewModifier viewModifier = new ViewModifier();

	private ClusterManager.OnClusterClickListener<T>               clickListener;
	private ClusterManager.OnClusterInfoWindowClickListener<T>     infoWindowClickListener;
	private ClusterManager.OnClusterItemClickListener<T>           itemClickListener;
	private ClusterManager.OnClusterItemInfoWindowClickListener<T> itemInfoWindowClickListener;

	public AirClusterRenderer(@NonNull Context context, GoogleMap map, ClusterManager<T> clusterManager) {
		this.map = map;
		density = context.getResources().getDisplayMetrics().density;
		iconGenerator = new IconGenerator(context);
		TextView text = makeSquareTextView(context);
		text.setTextColor(Colors.getColor(R.color.white));
		iconGenerator.setContentView(text);
		iconGenerator.setBackground(makeClusterBackground());
		this.clusterManager = clusterManager;
	}

	@Override public void onAdd() {
		clusterManager.getMarkerCollection().setOnMarkerClickListener(marker ->
			itemClickListener != null && itemClickListener.onClusterItemClick(markerCache.get(marker)));

		clusterManager.getMarkerCollection().setOnInfoWindowClickListener(marker -> {
			if (itemInfoWindowClickListener != null) {
				itemInfoWindowClickListener.onClusterItemInfoWindowClick(markerCache.get(marker));
			}
		});

		clusterManager.getClusterMarkerCollection().setOnMarkerClickListener(marker ->
			clickListener != null && clickListener.onClusterClick(markerToCluster.get(marker)));

		clusterManager.getClusterMarkerCollection().setOnInfoWindowClickListener(marker -> {
			if (infoWindowClickListener != null) {
				infoWindowClickListener.onClusterInfoWindowClick(markerToCluster.get(marker));
			}
		});
	}

	@Override public void onRemove() {
		clusterManager.getMarkerCollection().setOnMarkerClickListener(null);
		clusterManager.getClusterMarkerCollection().setOnMarkerClickListener(null);
	}

	private LayerDrawable makeClusterBackground() {
		coloredCircleBackground = new ShapeDrawable(new OvalShape());
		ShapeDrawable outline = new ShapeDrawable(new OvalShape());
		outline.getPaint().setColor(0x80ffffff); // Transparent white.
		LayerDrawable background = new LayerDrawable(new Drawable[]{outline, coloredCircleBackground});
		int strokeWidth = (int) (density * 3);
		background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
		return background;
	}

	private SquareTextView makeSquareTextView(Context context) {
		SquareTextView squareTextView = new SquareTextView(context);
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		squareTextView.setLayoutParams(layoutParams);
		squareTextView.setId(R.id.amu_text);
		int twelveDpi = (int) (12 * density);
		squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi);
		return squareTextView;
	}

	private int getColor() {
		return Colors.getColor(R.color.brand_primary_dark);
	}

	protected String getClusterText(int bucket) {
		if (bucket < BUCKETS[0]) {
			return String.valueOf(bucket);
		}
		return String.valueOf(bucket) + "+";
	}

	/**
	 * Gets the "bucket" for a particular cluster. By default, uses the number of points within the
	 * cluster, bucketed to some set points.
	 */
	protected int getBucket(@NonNull Cluster<T> cluster) {
		int size = cluster.getSize();
		if (size <= BUCKETS[0]) {
			return size;
		}
		for (int i = 0; i < BUCKETS.length - 1; i++) {
			if (size < BUCKETS[i + 1]) {
				return BUCKETS[i];
			}
		}
		return BUCKETS[BUCKETS.length - 1];
	}

	/**
	 * ViewModifier ensures only one re-rendering of the view occurs at a time, and schedules
	 * re-rendering, which is performed by the RenderTask.
	 */
	@SuppressLint("HandlerLeak")
	private class ViewModifier extends Handler {

		private static final int        RUN_TASK                   = 0;
		private static final int        TASK_FINISHED              = 1;
		private              boolean    viewModificationInProgress = false;
		private              RenderTask nextClusters               = null;

		@Override
		public void handleMessage(@NonNull Message msg) {
			if (msg.what == TASK_FINISHED) {
				viewModificationInProgress = false;
				if (nextClusters != null) {
					// Run the task that was queued up.
					sendEmptyMessage(RUN_TASK);
				}
				return;
			}
			removeMessages(RUN_TASK);

			if (viewModificationInProgress) {
				// Busy - wait for the callback.
				return;
			}

			if (nextClusters == null) {
				// Nothing to do.
				return;
			}

			RenderTask renderTask;
			synchronized (this) {
				renderTask = nextClusters;
				nextClusters = null;
				viewModificationInProgress = true;
			}

			renderTask.setCallback(() -> sendEmptyMessage(TASK_FINISHED));

			renderTask.setProjection(map.getProjection());
			renderTask.setMapZoom(map.getCameraPosition().zoom);

			new Thread(renderTask).start();
		}

		public void queue(Set<? extends Cluster<T>> clusters) {
			synchronized (this) {
				// Overwrite any pending cluster tasks - we don't care about intermediate states.
				nextClusters = new RenderTask(clusters);
			}
			sendEmptyMessage(RUN_TASK);
		}
	}

	/**
	 * Determine whether the cluster should be rendered as individual markers or a cluster.
	 */
	protected boolean shouldRenderAsCluster(@NonNull Cluster<T> cluster) {
		return cluster.getSize() > minClusterSize;
	}

	/**
	 * Transforms the current view (represented by DefaultClusterRenderer.mClusters and DefaultClusterRenderer.mZoom) to a
	 * new zoom level and set of clusters.
	 * <p>
	 * This must be run off the UI thread. Work is coordinated in the RenderTask, then queued up to
	 * be executed by a MarkerModifier.
	 * <p>
	 * There are three stages for the render:
	 * <p>
	 * 1. Markers are added to the map
	 * <p>
	 * 2. Markers are animated to their final position
	 * <p>
	 * 3. Any old markers are removed from the map
	 * <p>
	 * When zooming in, markers are animated out from the nearest existing cluster. When zooming
	 * out, existing clusters are animated to the nearest new cluster.
	 */
	private class RenderTask implements Runnable {
		final   Set<? extends Cluster<T>>   clusters;
		private Runnable                    callback;
		private Projection                  projection;
		private SphericalMercatorProjection sphericalMercatorProjection;
		private float                       mapZoom;

		private RenderTask(Set<? extends Cluster<T>> clusters) {
			this.clusters = clusters;
		}

		/**
		 * A callback to be run when all work has been completed.
		 */
		public void setCallback(Runnable callback) {
			this.callback = callback;
		}

		public void setProjection(Projection projection) {
			this.projection = projection;
		}

		public void setMapZoom(float zoom) {
			this.mapZoom = zoom;
			this.sphericalMercatorProjection = new SphericalMercatorProjection(256 * Math.pow(2, Math.min(zoom, AirClusterRenderer.this.zoom)));
		}

		@SuppressLint("NewApi")
		public void run() {
			if (clusters.equals(AirClusterRenderer.this.clusters)) {
				callback.run();
				return;
			}

			final MarkerModifier markerModifier = new MarkerModifier();

			final float zoom = mapZoom;
			final boolean zoomingIn = zoom > AirClusterRenderer.this.zoom;
			final float zoomDelta = zoom - AirClusterRenderer.this.zoom;

			final Set<MarkerWithPosition> markersToRemove = markers;
			final LatLngBounds visibleBounds = projection.getVisibleRegion().latLngBounds;
			// TODO: Add some padding, so that markers can animate in from off-screen.

			// Find all of the existing clusters that are on-screen. These are candidates for
			// markers to animate from.
			List<Point> existingClustersOnScreen = null;
			if (AirClusterRenderer.this.clusters != null && animate) {
				existingClustersOnScreen = new ArrayList<>();
				for (Cluster<T> c : AirClusterRenderer.this.clusters) {
					if (shouldRenderAsCluster(c) && visibleBounds.contains(c.getPosition())) {
						Point point = sphericalMercatorProjection.toPoint(c.getPosition());
						existingClustersOnScreen.add(point);
					}
				}
			}

			// Create the new markers and animate them to their new positions.
			final Set<MarkerWithPosition> newMarkers = Collections.newSetFromMap(new ConcurrentHashMap<>());
			for (Cluster<T> c : clusters) {
				boolean onScreen = visibleBounds.contains(c.getPosition());
				if (zoomingIn && onScreen && animate) {
					Point point = sphericalMercatorProjection.toPoint(c.getPosition());
					Point closest = findClosestCluster(existingClustersOnScreen, point);
					if (closest != null) {
						LatLng animateTo = sphericalMercatorProjection.toLatLng(closest);
						markerModifier.add(true, new CreateMarkerTask(c, newMarkers, animateTo));
					}
					else {
						markerModifier.add(true, new CreateMarkerTask(c, newMarkers, null));
					}
				}
				else {
					markerModifier.add(onScreen, new CreateMarkerTask(c, newMarkers, null));
				}
			}

			// Wait for all markers to be added.
			markerModifier.waitUntilFree();

			// Don't remove any markers that were just added. This is basically anything that had
			// a hit in the MarkerCache.
			markersToRemove.removeAll(newMarkers);

			// Find all of the new clusters that were added on-screen. These are candidates for
			// markers to animate from.
			List<Point> newClustersOnScreen = null;
			if (animate) {
				newClustersOnScreen = new ArrayList<>();
				for (Cluster<T> c : clusters) {
					if (shouldRenderAsCluster(c) && visibleBounds.contains(c.getPosition())) {
						Point p = sphericalMercatorProjection.toPoint(c.getPosition());
						newClustersOnScreen.add(p);
					}
				}
			}

			// Remove the old markers, animating them into clusters if zooming out.
			for (final MarkerWithPosition marker : markersToRemove) {
				boolean onScreen = visibleBounds.contains(marker.position);
				// Don't animate when zooming out more than 3 zoom levels.
				// TODO: drop animation based on speed of device & number of markers to animate.
				if (!zoomingIn && zoomDelta > -3 && onScreen && animate) {
					final Point point = sphericalMercatorProjection.toPoint(marker.position);
					final Point closest = findClosestCluster(newClustersOnScreen, point);
					if (closest != null) {
						LatLng animateTo = sphericalMercatorProjection.toLatLng(closest);
						markerModifier.animateThenRemove(marker, marker.position, animateTo);
					}
					else {
						markerModifier.remove(true, marker.marker);
					}
				}
				else {
					markerModifier.remove(onScreen, marker.marker);
				}
			}

			markerModifier.waitUntilFree();

			markers = newMarkers;
			AirClusterRenderer.this.clusters = clusters;
			AirClusterRenderer.this.zoom = zoom;

			callback.run();
		}
	}

	@Override public void onClustersChanged(Set<? extends Cluster<T>> clusters) {
		viewModifier.queue(clusters);
	}

	@Override public void setOnClusterClickListener(ClusterManager.OnClusterClickListener<T> listener) {
		clickListener = listener;
	}

	@Override
	public void setOnClusterInfoWindowClickListener(ClusterManager.OnClusterInfoWindowClickListener<T> listener) {
		infoWindowClickListener = listener;
	}

	@Override public void setOnClusterItemClickListener(ClusterManager.OnClusterItemClickListener<T> listener) {
		itemClickListener = listener;
	}

	@Override
	public void setOnClusterItemInfoWindowClickListener(ClusterManager.OnClusterItemInfoWindowClickListener<T> listener) {
		itemInfoWindowClickListener = listener;
	}

	private static double distanceSquared(@NonNull Point a, @NonNull Point b) {
		return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
	}

	private static Point findClosestCluster(List<Point> markers, @NonNull Point point) {
		if (markers == null || markers.isEmpty()) return null;

		// TODO: make this configurable.
		double minDistSquared = MAX_DISTANCE_AT_ZOOM * MAX_DISTANCE_AT_ZOOM;
		Point closest = null;
		for (Point candidate : markers) {
			double dist = distanceSquared(candidate, point);
			if (dist < minDistSquared) {
				closest = candidate;
				minDistSquared = dist;
			}
		}
		return closest;
	}

	/**
	 * Handles all markerWithPosition manipulations on the map. Work (such as adding, removing, or
	 * animating a markerWithPosition) is performed while trying not to block the rest of the app's
	 * UI.
	 */
	@SuppressLint("HandlerLeak")
	public class MarkerModifier extends Handler implements MessageQueue.IdleHandler {
		private static final int BLANK = 0;

		private final Lock      lock          = new ReentrantLock();
		private final Condition busyCondition = lock.newCondition();

		@NonNull
		private Queue<CreateMarkerTask> mCreateMarkerTasks         = new LinkedList<>();
		@NonNull
		private Queue<CreateMarkerTask> mOnScreenCreateMarkerTasks = new LinkedList<>();
		@NonNull
		private Queue<Marker>           mRemoveMarkerTasks         = new LinkedList<>();
		@NonNull
		private Queue<Marker>           mOnScreenRemoveMarkerTasks = new LinkedList<>();
		@NonNull
		private Queue<AnimationTask>    mAnimationTasks            = new LinkedList<>();

		/**
		 * Whether the idle listener has been added to the UI thread's MessageQueue.
		 */
		private boolean mListenerAdded;

		private MarkerModifier() {
			super(Looper.getMainLooper());
		}

		/**
		 * Creates markers for a cluster some time in the future.
		 *
		 * @param priority whether this operation should have priority.
		 */
		public void add(boolean priority, CreateMarkerTask c) {
			lock.lock();
			sendEmptyMessage(BLANK);
			if (priority) {
				mOnScreenCreateMarkerTasks.add(c);
			}
			else {
				mCreateMarkerTasks.add(c);
			}
			lock.unlock();
		}

		/**
		 * Removes a markerWithPosition some time in the future.
		 *
		 * @param priority whether this operation should have priority.
		 * @param m        the markerWithPosition to remove.
		 */
		public void remove(boolean priority, Marker m) {
			lock.lock();
			sendEmptyMessage(BLANK);
			if (priority) {
				mOnScreenRemoveMarkerTasks.add(m);
			}
			else {
				mRemoveMarkerTasks.add(m);
			}
			lock.unlock();
		}

		/**
		 * Animates a markerWithPosition some time in the future.
		 *
		 * @param marker the markerWithPosition to animate.
		 * @param from   the position to animate from.
		 * @param to     the position to animate to.
		 */
		public void animate(@NonNull MarkerWithPosition marker, LatLng from, LatLng to) {
			lock.lock();
			mAnimationTasks.add(new AnimationTask(marker, from, to));
			lock.unlock();
		}

		/**
		 * Animates a markerWithPosition some time in the future, and removes it when the animation
		 * is complete.
		 *
		 * @param marker the markerWithPosition to animate.
		 * @param from   the position to animate from.
		 * @param to     the position to animate to.
		 */
		public void animateThenRemove(@NonNull MarkerWithPosition marker, LatLng from, LatLng to) {
			lock.lock();
			AnimationTask animationTask = new AnimationTask(marker, from, to);
			animationTask.removeOnAnimationComplete(clusterManager.getMarkerManager());
			mAnimationTasks.add(animationTask);
			lock.unlock();
		}

		@Override
		public void handleMessage(Message msg) {
			if (!mListenerAdded) {
				Looper.myQueue().addIdleHandler(this);
				mListenerAdded = true;
			}
			removeMessages(BLANK);

			lock.lock();
			try {

				// Perform up to 10 tasks at once.
				// Consider only performing 10 remove tasks, not adds and animations.
				// Removes are relatively slow and are much better when batched.
				for (int i = 0; i < 10; i++) {
					performNextTask();
				}

				if (!isBusy()) {
					mListenerAdded = false;
					Looper.myQueue().removeIdleHandler(this);
					// Signal any other threads that are waiting.
					busyCondition.signalAll();
				}
				else {
					// Sometimes the idle queue may not be called - schedule up some work regardless
					// of whether the UI thread is busy or not.
					// TODO: try to remove this.
					sendEmptyMessageDelayed(BLANK, 10);
				}
			}
			finally {
				lock.unlock();
			}
		}

		/**
		 * Perform the next task. Prioritise any on-screen work.
		 */
		private void performNextTask() {
			if (!mOnScreenRemoveMarkerTasks.isEmpty()) {
				removeMarker(mOnScreenRemoveMarkerTasks.poll());
			}
			else if (!mAnimationTasks.isEmpty()) {
				mAnimationTasks.poll().perform();
			}
			else if (!mOnScreenCreateMarkerTasks.isEmpty()) {
				mOnScreenCreateMarkerTasks.poll().perform(this);
			}
			else if (!mCreateMarkerTasks.isEmpty()) {
				mCreateMarkerTasks.poll().perform(this);
			}
			else if (!mRemoveMarkerTasks.isEmpty()) {
				removeMarker(mRemoveMarkerTasks.poll());
			}
		}

		private void removeMarker(Marker m) {
			Cluster<T> cluster = markerToCluster.get(m);
			clusterToMarker.remove(cluster);
			markerCache.remove(m);
			markerToCluster.remove(m);
			clusterManager.getMarkerManager().remove(m);
		}

		/**
		 * @return true if there is still work to be processed.
		 */
		public boolean isBusy() {
			return !(mCreateMarkerTasks.isEmpty() && mOnScreenCreateMarkerTasks.isEmpty() &&
				mOnScreenRemoveMarkerTasks.isEmpty() && mRemoveMarkerTasks.isEmpty() &&
				mAnimationTasks.isEmpty()
			);
		}

		/**
		 * Blocks the calling thread until all work has been processed.
		 */
		public void waitUntilFree() {
			while (isBusy()) {
				// Sometimes the idle queue may not be called - schedule up some work regardless
				// of whether the UI thread is busy or not.
				// TODO: try to remove this.
				sendEmptyMessage(BLANK);
				lock.lock();
				try {
					if (isBusy()) {
						busyCondition.await();
					}
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				finally {
					lock.unlock();
				}
			}
		}

		@Override
		public boolean queueIdle() {
			// When the UI is not busy, schedule some work.
			sendEmptyMessage(BLANK);
			return true;
		}
	}

	/**
	 * A cache of markers representing individual ClusterItems.
	 */
	private static class MarkerCache<T> {
		@NonNull
		private Map<T, Marker> mCache        = new HashMap<>();
		@NonNull
		private Map<Marker, T> mCacheReverse = new HashMap<>();

		public Marker get(T item) {
			return mCache.get(item);
		}

		public T get(Marker m) {
			return mCacheReverse.get(m);
		}

		public void put(T item, Marker m) {
			mCache.put(item, m);
			mCacheReverse.put(m, item);
		}

		public void remove(Marker m) {
			T item = mCacheReverse.get(m);
			mCacheReverse.remove(m);
			mCache.remove(item);
		}
	}

	/**
	 * Called before the marker for a ClusterItem is added to the map.
	 */
	protected void onBeforeClusterItemRendered(T item, MarkerOptions markerOptions) { }

	/**
	 * Called before the marker for a Cluster is added to the map.
	 * The default implementation draws a circle with a rough count of the number of items.
	 */
	protected void onBeforeClusterRendered(@NonNull Cluster<T> cluster, @NonNull MarkerOptions markerOptions) {
		int bucket = getBucket(cluster);
		BitmapDescriptor descriptor = icons.get(bucket);
		if (descriptor == null) {
			coloredCircleBackground.getPaint().setColor(getColor());
			descriptor = BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(getClusterText(bucket)));
			icons.put(bucket, descriptor);
		}
		// TODO: consider adding anchor(.5, .5) (Individual markers will overlap more often)
		markerOptions.icon(descriptor);
	}

	/**
	 * Called after the marker for a ClusterItem has been added to the map.
	 */
	protected void onClusterItemRendered(T clusterItem, Marker marker) { }

	/**
	 * Get the marker from a ClusterItem
	 *
	 * @param clusterItem ClusterItem which you will obtain its marker
	 * @return a marker from a ClusterItem or null if it does not exists
	 */
	public Marker getMarker(T clusterItem) {
		return markerCache.get(clusterItem);
	}

	/**
	 * Get the ClusterItem from a marker
	 *
	 * @param marker which you will obtain its ClusterItem
	 * @return a ClusterItem from a marker or null if it does not exists
	 */
	public T getClusterItem(Marker marker) {
		return markerCache.get(marker);
	}

	/**
	 * Get the marker from a Cluster
	 *
	 * @param cluster which you will obtain its marker
	 * @return a marker from a cluster or null if it does not exists
	 */
	public Marker getMarker(Cluster<T> cluster) {
		return clusterToMarker.get(cluster);
	}

	/**
	 * Get the Cluster from a marker
	 *
	 * @param marker which you will obtain its Cluster
	 * @return a Cluster from a marker or null if it does not exists
	 */
	public Cluster<T> getCluster(Marker marker) {
		return markerToCluster.get(marker);
	}

	public void setMinClusterSize(int minClusterSize) {
		this.minClusterSize = minClusterSize;
	}

	public int getMinClusterSize() {
		return minClusterSize;
	}

	/**
	 * Creates markerWithPosition(s) for a particular cluster, animating it if necessary.
	 */
	private class CreateMarkerTask {
		private final Cluster<T>              cluster;
		private final Set<MarkerWithPosition> newMarkers;
		private final LatLng                  animateFrom;

		/**
		 * @param c            the cluster to render.
		 * @param markersAdded a collection of markers to append any created markers.
		 * @param animateFrom  the location to animate the markerWithPosition from, or null if no
		 *                     animation is required.
		 */
		public CreateMarkerTask(Cluster<T> c, Set<MarkerWithPosition> markersAdded, LatLng animateFrom) {
			this.cluster = c;
			this.newMarkers = markersAdded;
			this.animateFrom = animateFrom;
		}

		protected void perform(@NonNull MarkerModifier markerModifier) {
			// Don't show small clusters. Render the markers inside, instead.
			if (!shouldRenderAsCluster(cluster)) {
				for (T item : cluster.getItems()) {
					Marker marker = markerCache.get(item);
					MarkerWithPosition markerWithPosition;
					if (marker == null) {
						MarkerOptions markerOptions = new MarkerOptions();
						if (animateFrom != null) {
							markerOptions.position(animateFrom);
						}
						else {
							markerOptions.position(item.getPosition());
						}
						onBeforeClusterItemRendered(item, markerOptions);
						marker = clusterManager.getMarkerCollection().addMarker(markerOptions);
						markerWithPosition = new MarkerWithPosition(marker);
						markerCache.put(item, marker);
						if (animateFrom != null) {
							markerModifier.animate(markerWithPosition, animateFrom, item.getPosition());
						}
					}
					else {
						markerWithPosition = new MarkerWithPosition(marker);
					}
					onClusterItemRendered(item, marker);
					newMarkers.add(markerWithPosition);
				}
				return;
			}

			MarkerOptions markerOptions = new MarkerOptions().
				position(animateFrom == null ? cluster.getPosition() : animateFrom);

			onBeforeClusterRendered(cluster, markerOptions);

			Marker marker = clusterManager.getClusterMarkerCollection().addMarker(markerOptions);
			markerToCluster.put(marker, cluster);
			clusterToMarker.put(cluster, marker);
			MarkerWithPosition markerWithPosition = new MarkerWithPosition(marker);
			if (animateFrom != null) {
				markerModifier.animate(markerWithPosition, animateFrom, cluster.getPosition());
			}
			newMarkers.add(markerWithPosition);
		}
	}

	/**
	 * A Marker and its position. Marker.getPosition() must be called from the UI thread, so this
	 * object allows lookup from other threads.
	 */
	private static class MarkerWithPosition {
		@NonNull
		private final Marker marker;
		private       LatLng position;

		private MarkerWithPosition(@NonNull Marker marker) {
			this.marker = marker;
			position = marker.getPosition();
		}

		@Override
		public boolean equals(Object other) {
			return (other instanceof MarkerWithPosition && marker.equals(((MarkerWithPosition) other).marker));
		}

		@Override
		public int hashCode() {
			return marker.hashCode();
		}
	}

	private static final TimeInterpolator ANIMATION_INTERP = new DecelerateInterpolator();

	/**
	 * Animates a markerWithPosition from one position to another. TODO: improve performance for
	 * slow devices (e.g. Nexus S).
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private class AnimationTask extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
		@NonNull
		private final MarkerWithPosition markerWithPosition;
		@NonNull
		private final Marker             marker;
		private final LatLng             from;
		private final LatLng             to;
		private       boolean            mRemoveOnComplete;
		private       MarkerManager      mMarkerManager;

		private AnimationTask(@NonNull MarkerWithPosition markerWithPosition, LatLng from, LatLng to) {
			this.markerWithPosition = markerWithPosition;
			this.marker = markerWithPosition.marker;
			this.from = from;
			this.to = to;
		}

		public void perform() {
			ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
			valueAnimator.setInterpolator(ANIMATION_INTERP);
			valueAnimator.addUpdateListener(this);
			valueAnimator.addListener(this);
			valueAnimator.start();
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			if (mRemoveOnComplete) {
				Cluster<T> cluster = markerToCluster.get(marker);
				clusterToMarker.remove(cluster);
				markerCache.remove(marker);
				markerToCluster.remove(marker);
				mMarkerManager.remove(marker);
			}
			markerWithPosition.position = to;
		}

		public void removeOnAnimationComplete(MarkerManager markerManager) {
			mMarkerManager = markerManager;
			mRemoveOnComplete = true;
		}

		@Override
		public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
			float fraction = valueAnimator.getAnimatedFraction();
			double lat = (to.latitude - from.latitude) * fraction + from.latitude;
			double lngDelta = to.longitude - from.longitude;

			// Take the shortest path across the 180th meridian.
			if (Math.abs(lngDelta) > 180) {
				lngDelta -= Math.signum(lngDelta) * 360;
			}
			double lng = lngDelta * fraction + from.longitude;
			LatLng position = new LatLng(lat, lng);
			marker.setPosition(position);
		}
	}
}
