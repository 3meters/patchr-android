package com.patchr.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.MapManager;
import com.patchr.components.StringManager;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Entity;
import com.patchr.objects.Patch;
import com.patchr.objects.Route;
import com.patchr.ui.components.AirClusterRenderer;
import com.patchr.utilities.Dialogs;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapListFragment extends SupportMapFragment implements ClusterManager.OnClusterClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterInfoWindowClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemInfoWindowClickListener<MapListFragment.EntityItem>
		, GoogleMap.OnMyLocationButtonClickListener {

	protected GoogleMap                  mMap;
	protected ClusterManager<EntityItem> mClusterManager;
	protected AirClusterRenderer         mClusterRenderer;
	protected List<Entity>               mEntities;
	protected Integer                    mTitleResId;
	protected String                     mRelatedListFragment;
	protected Bitmap                     mMarkerBitmap;
	protected Integer       mZoomLevel  = null;
	protected List<Integer> mMenuResIds = new ArrayList<Integer>();
	protected Boolean       mShowIndex  = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		/*
		 * Any objects obtained from GoogleMap are associated with the view and will be
		 * leaked if held beyond view lifetime.
		 */
		getMapAsync(new OnMapReadyCallback() {

			@Override
			public void onMapReady(@NonNull GoogleMap googleMap) {

				if (getActivity() == null) return;

				mMap = googleMap;

				mClusterManager = new ClusterManager<EntityItem>(getActivity(), mMap);
				mClusterRenderer = new EntityRenderer(getActivity());
				mClusterRenderer.setMinClusterSize(10);
				mClusterManager.setRenderer(mClusterRenderer);

				mMap.setOnCameraChangeListener(mClusterManager);
				mMap.setOnMarkerClickListener(mClusterManager);
				mMap.setOnInfoWindowClickListener(mClusterManager);

				mClusterManager.setOnClusterClickListener(MapListFragment.this);
				mClusterManager.setOnClusterInfoWindowClickListener(MapListFragment.this);
				mClusterManager.setOnClusterItemClickListener(MapListFragment.this);
				mClusterManager.setOnClusterItemInfoWindowClickListener(MapListFragment.this);

				mMap.setOnMyLocationButtonClickListener(MapListFragment.this);
				mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				mMap.setMyLocationEnabled(true);  // Causes connect() log error by gms client
				mMap.setLocationSource(null);

				UiSettings uiSettings = mMap.getUiSettings();

				uiSettings.setZoomControlsEnabled(true);
				uiSettings.setMyLocationButtonEnabled(true);
				uiSettings.setAllGesturesEnabled(true);
				uiSettings.setCompassEnabled(true);

				/* Entities to map are always set at start */
				draw();
			}
		});

		return root;
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
		Patchr.router.route(getActivity(), Route.BROWSE, entityItem.mEntity, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void draw() {
		/*
		 * Entities are injected into mEntities by the host activity.
		 */
		if (mClusterManager != null) {
			mMap.clear();
			mClusterManager.clearItems();
			if (mEntities != null) {
				for (Entity entity : mEntities) {
					if (entity.getLocation() != null) {
						entity.index = mEntities.indexOf(entity) + 1;
						AirLocation location = entity.getLocation();
						EntityItem entityItem = new EntityItem(location.lat.doubleValue(), location.lng.doubleValue(), entity);
						mClusterManager.addItem(entityItem);
					}
				}
			}
		}

		final View mapView = getView();
		if (mapView != null) {
			/*
			 * We could get this call before mMap has been set.
			 */
			if (mEntities == null || mEntities.size() == 0 || mMap == null) return;
			/*
			 * One only one entity then center on it.
			 */
			if (mEntities.size() == 1 && mZoomLevel != null) {
				Patch patch = (Patch) mEntities.get(0);
				AirLocation location = patch.getLocation();
				if (location != null && location.lat != null && location.lng != null) {
					mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()), mZoomLevel));
				}
			}
			/*
			 * Multiple entities, center on grouping.
			 */
			else {
				if (mZoomLevel != null) {
					Location location = LocationManager.getInstance().getLocationLocked();
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
						mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MapManager.LATLNG_USA, MapManager.ZOOM_SCALE_USA));
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

	public MapListFragment setRelatedListFragment(String relatedListFragment) {
		mRelatedListFragment = relatedListFragment;
		return this;
	}

	public MapListFragment setShowIndex(Boolean showIndex) {
		mShowIndex = showIndex;
		return this;
	}

	public String getRelatedListFragment() {
		return mRelatedListFragment;
	}

	public List<Integer> getMenuResIds() {
		return mMenuResIds;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		/*
		 * This is triggered by onCreate in some android versions
		 * so any dependencies must have already been created.
		 */
		Logger.d(this, "Creating fragment options menu");
		for (Integer menuResId : mMenuResIds) {
			inflater.inflate(menuResId, menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onDestroyView() {
		Logger.d(this, "Fragment destroy view");
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Logger.d(this, "Fragment destroy");
		super.onDestroy();
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
		private View          mMarkerView;

		public EntityRenderer(Context context) {
			super(context, mMap, mClusterManager);
			mIconGenerator = new IconGenerator(context);
			mMarkerView = LayoutInflater.from(context).inflate(R.layout.widget_marker_view, null, false);
			mIconGenerator.setBackground(null);
			mIconGenerator.setContentView(mMarkerView);
		}

		@Override
		protected void onBeforeClusterItemRendered(final EntityItem entityItem, final MarkerOptions markerOptions) {
			if (entityItem.mEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				final Patch patch = (Patch) entityItem.mEntity;
				if (mShowIndex && entityItem.mEntity.index != null) {
					String label = (entityItem.mEntity.index.intValue() <= 99) ? String.valueOf(entityItem.mEntity.index) : "+";
					markerOptions.icon(BitmapDescriptorFactory.fromBitmap(mIconGenerator.makeIcon(label)));
				}
				else {
					markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker));
				}
				markerOptions.title(!(TextUtils.isEmpty(patch.name)) ? patch.name : StringManager.getString(R.string.container_singular));
				markerOptions.snippet((!TextUtils.isEmpty(patch.type)) ? patch.type : null);
			}
		}

		@Override
		protected void onClusterItemRendered(EntityItem clusterItem, Marker marker) {
			super.onClusterItemRendered(clusterItem, marker);
			marker.setAnchor(0.5f, 0.8f);
			if (mEntities != null && mEntities.size() == 1) {
				marker.showInfoWindow();
			}
		}
	}
}
