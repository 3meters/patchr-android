package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.events.ViewLayoutEvent;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.ui.components.AirClusterRenderer;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.UI;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.ClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapListFragment extends MapFragment implements ClusterManager.OnClusterClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterInfoWindowClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemInfoWindowClickListener<MapListFragment.EntityItem>
		, GoogleMap.OnMyLocationButtonClickListener {

	public static final int    ZOOM_NEARBY  = 14;
	public static final int    ZOOM_CITY    = 11;
	public static final int    ZOOM_COUNTY  = 10;
	public static final int    ZOOM_STATE   = 6;
	public static final int    ZOOM_COUNTRY = 5;
	public static final int    ZOOM_USA     = 3;
	public static final int    ZOOM_WORLD   = 1;
	public static final int    ZOOM_DEFAULT = ZOOM_NEARBY;
	public static       LatLng LATLNG_USA   = new LatLng(39.8282, -98.5795);

	protected GoogleMap                  mMap;
	protected ClusterManager<EntityItem> mClusterManager;
	protected List<Entity>               mEntities;
	protected Integer                    mTitleResId;
	protected Integer       mZoomLevel   = null;
	protected List<Integer> mMenuResIds  = new ArrayList<Integer>();
	protected View          mProgressBar = null;
	protected ClusterRenderer mClusterRenderer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		mMap = getMap();
		mClusterManager = new ClusterManager<EntityItem>(getActivity(), mMap);
		mClusterRenderer = new EntityRenderer(getActivity());
		mClusterManager.setRenderer(mClusterRenderer);

		mMap.setOnCameraChangeListener(mClusterManager);
		mMap.setOnMarkerClickListener(mClusterManager);
		mMap.setOnInfoWindowClickListener(mClusterManager);

		mClusterManager.setOnClusterClickListener(this);
		mClusterManager.setOnClusterInfoWindowClickListener(this);
		mClusterManager.setOnClusterItemClickListener(this);
		mClusterManager.setOnClusterItemInfoWindowClickListener(this);
		mMap.setOnMyLocationButtonClickListener(this);

		if (root != null) {
			mProgressBar = new ProgressBar(root.getContext(), null, android.R.attr.progressBarStyleLarge);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
					UI.getRawPixelsForDisplayPixels(50f),
					UI.getRawPixelsForDisplayPixels(50f));
			params.gravity = Gravity.CENTER;
			mProgressBar.setLayoutParams(params);
			mProgressBar.setVisibility(View.INVISIBLE);

			((ViewGroup) root).addView(mProgressBar);
		}

		return root;
	}

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		if (mProgressBar != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mProgressBar.setVisibility(View.INVISIBLE);
				}
			});
		}
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (view != null && view.getViewTreeObserver().isAlive()) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				public void onGlobalLayout() {
					//noinspection deprecation
					view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					BusProvider.getInstance().post(new ViewLayoutEvent());
				}
			});
		}

		// Check if we were successful in obtaining the map.
		if (checkReady()) {
			setUpMap();
		}

		// We can draw if we already have entities
		if (mEntities != null) {
			draw();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onMyLocationButtonClick() {
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Dialogs.locationServicesDisabled(getActivity(), new AtomicBoolean(false));
			return true;
		}
		return false;
	}

	@Override
	public boolean onClusterClick(Cluster<EntityItem> cluster) {

		LatLngBounds.Builder builder = LatLngBounds.builder();
		for (EntityItem item : cluster.getItems()) {
			builder.include(item.getPosition());
		}
		LatLngBounds bounds = builder.build();

		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 150);
		mMap.animateCamera(cameraUpdate, 300, null);
		return true;
	}

	@Override
	public void onClusterInfoWindowClick(Cluster<EntityItem> entityItemCluster) {
		// Does nothing, but you could go to a list of the users.
	}

	@Override
	public boolean onClusterItemClick(EntityItem entityItem) {
		// Does nothing, but you could go into the user's profile page, for example.
		return false;
	}

	@Override
	public void onClusterItemInfoWindowClick(EntityItem entityItem) {
		Aircandi.dispatch.route(getActivity(), Route.BROWSE, entityItem.mEntity, null, null);
	}


	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void draw() {

		if (mEntities != null) {
			mProgressBar.setVisibility(View.VISIBLE);
			for (Entity entity : mEntities) {
				if (entity.getLocation() != null) {
					AirLocation location = entity.getLocation();
					EntityItem entityItem = new EntityItem(location.lat.doubleValue(), location.lng.doubleValue(), entity);
					mClusterManager.addItem(entityItem);
				}
			}
		}

		final View mapView = getView();

		if (mapView != null && mapView.getViewTreeObserver().isAlive()) {
			mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				public void onGlobalLayout() {
					mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					if (mEntities == null || mEntities.size() == 0) return;
					/*
					 * One only one entity then center on it.
					 */
					if (mEntities.size() == 1) {
						Place place = (Place) mEntities.get(0);
						AirLocation location = place.getLocation();
						if (location != null) {
							mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()), mZoomLevel));
						}
					}
					/*
					 * Multiple entities, center on grouping.
					 */
					else {
						if (mZoomLevel != null) {
							Location location = LocationManager.getInstance().getLocationLast();
							if (location != null) {
								LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
								mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mZoomLevel));
							}
							else {
								/*
								 * We have no idea where the user is. We don't use bounds because
								 * that could center the user out in the ocean or something else
								 * stupid.
								 */
								mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LATLNG_USA, ZOOM_USA));
							}
						}
						else {
							LatLngBounds bounds = getBounds(mEntities);
							if (bounds != null) {
								int padding = (int) (Math.min(mapView.getWidth(), mapView.getHeight()) * 0.2);
								CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
								mMap.moveCamera(cameraUpdate);
							}
						}
					}
				}
			});
		}
	}

	private boolean checkReady() {
		/*
		 * Parent activity performs play services check. We can't get to
		 * here if they are not available.
		 */
		if (mMap == null) {
			UI.showToastNotification("Map not ready", Toast.LENGTH_SHORT);
			return false;
		}
		return true;
	}

	private void setUpMap() {

		mMap.setMyLocationEnabled(true);
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.setLocationSource(null);
		mMap.setPadding(0, 0, 0, UI.getRawPixelsForDisplayPixels(80f));

		UiSettings uiSettings = mMap.getUiSettings();

		uiSettings.setZoomControlsEnabled(true);
		uiSettings.setMyLocationButtonEnabled(true);
		uiSettings.setAllGesturesEnabled(true);
		uiSettings.setCompassEnabled(true);
	}

	public LatLngBounds getBounds(List<Entity> entities) {
		LatLngBounds bounds = null;
		if (entities != null) {
			LatLngBounds.Builder builder = LatLngBounds.builder();
			for (Entity entity : entities) {
				if (entity.location != null) {
					builder.include(new LatLng(entity.location.lat.doubleValue(), entity.location.lng.doubleValue()));
				}
			}
			bounds = builder.build();
		}
		return bounds;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public MapListFragment setEntities(List<Entity> entities) {
		mEntities = entities;
		return this;
	}

	public MapListFragment setTitleResId(Integer titleResId) {
		mTitleResId = titleResId;
		return this;
	}

	public MapListFragment setZoomLevel(Integer zoomLevel) {
		mZoomLevel = zoomLevel;
		return this;
	}

	public List<Integer> getMenuResIds() {
		return mMenuResIds;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onResume() {
		BusProvider.getInstance().register(this);
		super.onResume();
	}

	@Override
	public void onPause() {
		BusProvider.getInstance().unregister(this);
		super.onPause();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class EntityItem implements ClusterItem {

		private final LatLng mPosition;
		private final Entity mEntity;

		public EntityItem(double lat, double lng, Entity entity) {
			mPosition = new LatLng(lat, lng);
			mEntity = entity;
		}

		@Override
		public LatLng getPosition() {
			return mPosition;
		}
	}

	/**
	 * Draws entity photos inside markers (using IconGenerator).
	 * When there are multiple entities in the cluster, draw multiple photos (using MultiDrawable).
	 */
	private class EntityRenderer extends AirClusterRenderer<EntityItem> {

		private IconGenerator mIconGenerator;
		private TextView      mLabel;
		private AirImageView  mImage;

		public EntityRenderer(Context context) {
			super(context, getMap(), mClusterManager);

			mIconGenerator = new IconGenerator(context);

			/* Single markers */
			@SuppressLint("InflateParams") View singleMarker = getActivity().getLayoutInflater().inflate(R.layout.widget_map_marker, null, false);
			mIconGenerator.setContentView(singleMarker);
			mLabel = (TextView) singleMarker.findViewById(R.id.entity_text);
			mLabel.setBackgroundDrawable(new ColorDrawable(Colors.getColor(R.color.brand_primary)));
			mImage = (AirImageView) singleMarker.findViewById(R.id.entity_photo);
			mImage.setSizeHint(UI.getRawPixelsForDisplayPixels(50f));
			mImage.setSizeType(AirImageView.SizeType.THUMBNAIL);
		}

		@Override
		protected void onBeforeClusterItemRendered(final EntityItem entityItem, final MarkerOptions markerOptions) {

			final Place place = (Place) entityItem.mEntity;
			markerOptions.title(place.id);

			new AsyncTask() {

				@Override
				protected void onPreExecute() {}

				@Override
				protected Object doInBackground(Object... params) {

					Thread.currentThread().setName("AsyncGetEntity");
					ModelResult result = new ModelResult();
					try {
						/*
						 * Picasso.get() will pull from memory/file/network but will only
						 * cache to a file. This is probably for the best so we don't consume
						 * memory for a bunch of little map icons.
						 */
						Photo photo = place.getPhoto();
						Bitmap bitmap = DownloadManager.with(Aircandi.applicationContext)
						                               .load(photo.getUri())
						                               .centerCrop()
						                               .resize(mImage.getSizeHint(), mImage.getSizeHint())
						                               .get();
						result.data = bitmap;
					}
					catch (IOException ignore) {}
					return result;
				}

				@Override
				protected void onPostExecute(Object modelResult) {
					ModelResult result = (ModelResult) modelResult;
					if (result.data != null) {
						reloadMarker(place, (Bitmap) result.data);
					}
				}
			}.execute();

			mImage.getImageView().setImageBitmap(null);
			Bitmap icon = mIconGenerator.makeIcon();
			markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
		}

		public void reloadMarker(Entity entity, Bitmap bitmap) {

			final Place place = (Place) entity;

			Collection<Marker> markers = mClusterManager.getMarkerCollection().getMarkers();
			for (Marker marker : markers) {
				if (place.id.equals(marker.getTitle())) {
					mImage.getImageView().setImageBitmap(bitmap);
					Bitmap icon = mIconGenerator.makeIcon();

					marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
					marker.setTitle(!(TextUtils.isEmpty(place.name)) ? place.name : StringManager.getString(R.string.container_singular));
					marker.setSnippet((place.category != null && !TextUtils.isEmpty(place.category.name)) ? place.category.name : null);
					if (mEntities.size() == 1) {
						marker.showInfoWindow();
					}
					break;
				}
			}
		}
	}
}
