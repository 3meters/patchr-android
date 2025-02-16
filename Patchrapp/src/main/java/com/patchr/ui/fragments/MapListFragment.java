package com.patchr.ui.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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
import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.model.Location;
import com.patchr.model.RealmEntity;
import com.patchr.ui.components.AirClusterRenderer;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.realm.RealmList;

public class MapListFragment extends SupportMapFragment implements ClusterManager.OnClusterClickListener<MapListFragment.EntityItem>
	, ClusterManager.OnClusterInfoWindowClickListener<MapListFragment.EntityItem>
	, ClusterManager.OnClusterItemClickListener<MapListFragment.EntityItem>
	, ClusterManager.OnClusterItemInfoWindowClickListener<MapListFragment.EntityItem>
	, GoogleMap.OnMyLocationButtonClickListener {

	public    RealmList<RealmEntity>     entities;
	public    Integer                    titleResId;
	public    String                     relatedListFragment;
	public    Integer                    zoomLevel;
	public    boolean                    showIndex;
	protected GoogleMap                  map;
	protected ClusterManager<EntityItem> clusterManager;
	protected AirClusterRenderer         clusterRenderer;
	protected Bitmap                     markerBitmap;
	public Integer bottomPadding = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		/*
		 * Any objects obtained from GoogleMap are associated with the view and will be
		 * leaked if held beyond view lifetime.
		 */
		getMapAsync(googleMap -> {

			if (getActivity() == null) return;

			map = googleMap;

			clusterManager = new ClusterManager<EntityItem>(getActivity(), map);
			clusterRenderer = new EntityRenderer(getActivity());
			clusterRenderer.setMinClusterSize(10);
			clusterManager.setRenderer(clusterRenderer);

			//noinspection deprecation
			map.setOnCameraIdleListener(clusterManager);
			map.setOnMarkerClickListener(clusterManager);
			map.setOnInfoWindowClickListener(clusterManager);

			clusterManager.setOnClusterClickListener(MapListFragment.this);
			clusterManager.setOnClusterInfoWindowClickListener(MapListFragment.this);
			clusterManager.setOnClusterItemClickListener(MapListFragment.this);
			clusterManager.setOnClusterItemInfoWindowClickListener(MapListFragment.this);

			map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			map.setPadding(0, 0, 0, bottomPadding);

			UiSettings uiSettings = map.getUiSettings();

			if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				map.setMyLocationEnabled(true);  // Causes connect() log error by gms client
				uiSettings.setMyLocationButtonEnabled(true);
				map.setOnMyLocationButtonClickListener(MapListFragment.this);
			}

			map.setLocationSource(null);

			uiSettings.setZoomControlsEnabled(true);
			uiSettings.setAllGesturesEnabled(true);
			uiSettings.setCompassEnabled(true);

			/* Entities to map are always set at start */
			bind();
		});

		return root;
	}

	@Override public void onDestroyView() {
		Logger.d(this, "Fragment destroy view");
		super.onDestroyView();
	}

	@Override public void onDestroy() {
		Logger.d(this, "Fragment destroy");
		super.onDestroy();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onMyLocationButtonClick() {
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Dialogs.locationServicesDisabled(getActivity(), new AtomicBoolean(false));
			return true;
		}
		return false;
	}

	@Override public boolean onClusterClick(Cluster<EntityItem> cluster) {

		LatLngBounds.Builder builder = LatLngBounds.builder();
		for (EntityItem item : cluster.getItems()) {
			builder.include(item.getPosition());
		}
		LatLngBounds bounds = builder.build();

		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 150);
		map.animateCamera(cameraUpdate, 300, null);
		return true;
	}

	@Override public void onClusterInfoWindowClick(Cluster<EntityItem> entityItemCluster) {
		// Does nothing, but you could go to a list of the users.
	}

	@Override public boolean onClusterItemClick(EntityItem entityItem) {
		// Does nothing, but you could go into the user's profile page, for example.
		return false;
	}

	@Override public void onClusterItemInfoWindowClick(EntityItem entityItem) {
		UI.browseEntity(entityItem.entity.id, getActivity());
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind() {
		/*
		 * Entities are injected into mEntities by the host activity.
		 */
		if (clusterManager != null) {
			map.clear();
			clusterManager.clearItems();
			if (entities != null) {
				for (RealmEntity entity : entities) {
					if (entity.getLocation() != null) {
						entity.index = entities.indexOf(entity) + 1;
						Location location = entity.getLocation();
						EntityItem entityItem = new EntityItem(location.lat, location.lng, entity);
						clusterManager.addItem(entityItem);
					}
				}
			}
		}

		final View mapView = getView();
		if (mapView != null) {
			/*
			 * We could get this call before mMap has been set.
			 */
			if (entities == null || entities.size() == 0 || map == null) return;
			/*
			 * One only one entity then center on it.
			 */
			if (entities.size() == 1 && zoomLevel != null) {
				RealmEntity entity = entities.get(0);
				Location location = entity.getLocation();
				if (location != null && location.lat != null && location.lng != null) {
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.lat, location.lng), zoomLevel));
				}
			}
			/*
			 * Multiple entities, center on grouping.
			 */
			else {
				if (zoomLevel != null) {
					android.location.Location location = LocationManager.getInstance().getAndroidLocationLocked();
					if (location != null) {
						LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
						map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
					}
					else {
						/*
						 * We have no idea where the user is. We don't use bounds because
						 * that could center the user out in the ocean or something else
						 * stupid.
						 */
						map.moveCamera(CameraUpdateFactory.newLatLngZoom(Constants.LATLNG_USA, Constants.ZOOM_SCALE_USA));
					}
				}
				else {
					LatLngBounds bounds = getBounds(entities);
					if (bounds != null) {
						int padding = (int) (Math.min(mapView.getWidth(), mapView.getHeight()) * 0.2);
						CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
						map.moveCamera(cameraUpdate);
					}
				}
			}
		}
	}

	public LatLngBounds getBounds(List<RealmEntity> entities) {
		LatLngBounds bounds = null;
		if (entities != null) {
			LatLngBounds.Builder builder = LatLngBounds.builder();
			for (RealmEntity entity : entities) {
				if (entity.getLocation() != null) {
					builder.include(new LatLng(entity.getLocation().lat, entity.getLocation().lng));
				}
			}
			bounds = builder.build();
		}
		return bounds;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class EntityItem implements ClusterItem {

		private final LatLng      position;
		private final RealmEntity entity;

		public EntityItem(double lat, double lng, RealmEntity entity) {
			this.position = new LatLng(lat, lng);
			this.entity = entity;
		}

		@Override
		public LatLng getPosition() {
			return position;
		}
	}

	/**
	 * Draws entity photos inside markers (using IconGenerator).
	 * When there are multiple entities in the cluster, draw multiple photos (using MultiDrawable).
	 */
	private class EntityRenderer extends AirClusterRenderer<EntityItem> {

		private IconGenerator iconGenerator;
		private View          markerView;

		public EntityRenderer(Context context) {
			super(context, map, clusterManager);
			iconGenerator = new IconGenerator(context);
			markerView = LayoutInflater.from(context).inflate(R.layout.view_map_marker, null, false);
			iconGenerator.setBackground(null);
			iconGenerator.setContentView(markerView);
		}

		@Override
		protected void onBeforeClusterItemRendered(final EntityItem entityItem, final MarkerOptions markerOptions) {

			if (entityItem.entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				if (showIndex && entityItem.entity.index != null) {
					String label = (entityItem.entity.index <= 99) ? String.valueOf(entityItem.entity.index) : "+";
					markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(label)));
				}
				else {
					markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker));
				}
				markerOptions.title(!(TextUtils.isEmpty(entityItem.entity.name)) ? entityItem.entity.name : StringManager.getString(R.string.container_singular));
				markerOptions.snippet((!TextUtils.isEmpty(entityItem.entity.type)) ? entityItem.entity.type : null);
			}
		}

		@Override protected void onClusterItemRendered(EntityItem clusterItem, Marker marker) {
			super.onClusterItemRendered(clusterItem, marker);

			marker.setAnchor(0.5f, 0.8f);
			if (entities != null && entities.size() == 1) {
				marker.showInfoWindow();
			}
		}
	}
}
