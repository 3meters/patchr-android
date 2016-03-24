package com.patchr.ui.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.patchr.ui.components.AirClusterRenderer;
import com.patchr.utilities.Dialogs;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapListFragment extends SupportMapFragment implements ClusterManager.OnClusterClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterInfoWindowClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemInfoWindowClickListener<MapListFragment.EntityItem>
		, GoogleMap.OnMyLocationButtonClickListener {

	public    List<Entity>               entities;
	public    Integer                    titleResId;
	public    String                     relatedListFragment;
	public    Integer                    zoomLevel;
	public    boolean                    showIndex;
	protected GoogleMap                  map;
	protected ClusterManager<EntityItem> clusterManager;
	protected AirClusterRenderer         clusterRenderer;
	protected Bitmap                     markerBitmap;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		/*
		 * Any objects obtained from GoogleMap are associated with the view and will be
		 * leaked if held beyond view lifetime.
		 */
		getMapAsync(new OnMapReadyCallback() {

			@Override
			public void onMapReady(@NonNull GoogleMap googleMap) {

				if (getActivity() == null) return;

				map = googleMap;

				clusterManager = new ClusterManager<>(getActivity(), map);
				clusterRenderer = new EntityRenderer(getActivity());
				clusterRenderer.setMinClusterSize(10);
				clusterManager.setRenderer(clusterRenderer);

				map.setOnCameraChangeListener(clusterManager);
				map.setOnMarkerClickListener(clusterManager);
				map.setOnInfoWindowClickListener(clusterManager);

				clusterManager.setOnClusterClickListener(MapListFragment.this);
				clusterManager.setOnClusterInfoWindowClickListener(MapListFragment.this);
				clusterManager.setOnClusterItemClickListener(MapListFragment.this);
				clusterManager.setOnClusterItemInfoWindowClickListener(MapListFragment.this);

				map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

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
			}
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
		Patchr.router.browse(getActivity(), entityItem.mEntity.id, null, true);
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
				for (Entity entity : entities) {
					if (entity.getLocation() != null) {
						entity.index = entities.indexOf(entity) + 1;
						AirLocation location = entity.getLocation();
						EntityItem entityItem = new EntityItem(location.lat.doubleValue(), location.lng.doubleValue(), entity);
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
				Patch patch = (Patch) entities.get(0);
				AirLocation location = patch.getLocation();
				if (location != null && location.lat != null && location.lng != null) {
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()), zoomLevel));
				}
			}
			/*
			 * Multiple entities, center on grouping.
			 */
			else {
				if (zoomLevel != null) {
					Location location = LocationManager.getInstance().getLocationLocked();
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
						map.moveCamera(CameraUpdateFactory.newLatLngZoom(MapManager.LATLNG_USA, MapManager.ZOOM_SCALE_USA));
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

		private IconGenerator iconGenerator;
		private View          markerView;

		public EntityRenderer(Context context) {
			super(context, map, clusterManager);
			iconGenerator = new IconGenerator(context);
			markerView = LayoutInflater.from(context).inflate(R.layout.widget_marker_view, null, false);
			iconGenerator.setBackground(null);
			iconGenerator.setContentView(markerView);
		}

		@Override protected void onBeforeClusterItemRendered(final EntityItem entityItem, final MarkerOptions markerOptions) {

			if (entityItem.mEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				final Patch patch = (Patch) entityItem.mEntity;
				if (showIndex && entityItem.mEntity.index != null) {
					String label = (entityItem.mEntity.index.intValue() <= 99) ? String.valueOf(entityItem.mEntity.index) : "+";
					markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(label)));
				}
				else {
					markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker));
				}
				markerOptions.title(!(TextUtils.isEmpty(patch.name)) ? patch.name : StringManager.getString(R.string.container_singular));
				markerOptions.snippet((!TextUtils.isEmpty(patch.type)) ? patch.type : null);
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
