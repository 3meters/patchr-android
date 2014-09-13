package com.aircandi.catalina.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.catalina.R;
import com.aircandi.catalina.ui.components.AirClusterRenderer;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Colors;
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
import com.google.maps.android.ui.IconGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class MapListFragment extends MapFragment implements ClusterManager.OnClusterClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterInfoWindowClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemClickListener<MapListFragment.EntityItem>
		, ClusterManager.OnClusterItemInfoWindowClickListener<MapListFragment.EntityItem> {

	public static final int ZOOM_NEARBY  = 14;
	public static final int ZOOM_CITY    = 11;
	public static final int ZOOM_COUNTY  = 10;
	public static final int ZOOM_STATE   = 6;
	public static final int ZOOM_COUNTRY = 5;
	public static final int ZOOM_DEFAULT = ZOOM_NEARBY;

	protected GoogleMap                  mMap;
	protected ClusterManager<EntityItem> mClusterManager;
	protected List<Entity>               mEntities;
	protected Integer                    mTitleResId;
	protected Integer       mZoomLevel  = ZOOM_DEFAULT;
	protected List<Integer> mMenuResIds = new ArrayList<Integer>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		mMap = getMap();
		mClusterManager = new ClusterManager<EntityItem>(getActivity(), mMap);
		mClusterManager.setRenderer(new EntityRenderer(getActivity()));

		mMap.setOnCameraChangeListener(mClusterManager);
		mMap.setOnMarkerClickListener(mClusterManager);
		mMap.setOnInfoWindowClickListener(mClusterManager);

		mClusterManager.setOnClusterClickListener(this);
		mClusterManager.setOnClusterInfoWindowClickListener(this);
		mClusterManager.setOnClusterItemClickListener(this);
		mClusterManager.setOnClusterItemInfoWindowClickListener(this);

		// Check if we were successful in obtaining the map.
		if (checkReady()) {
			setUpMap();
			draw();
		}
		return root;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void draw() {
		Logger.d(this, "Map draw");
		for (Entity entity : mEntities) {
			if (entity.getLocation() != null) {
				AirLocation location = entity.getLocation();
				EntityItem marker = new EntityItem(location.lat.doubleValue(), location.lng.doubleValue(), entity);
				mClusterManager.addItem(marker);
			}
		}
		Location location = LocationManager.getInstance().getLocationLast();
		if (location != null) {
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), mZoomLevel));
		}
	}

	private boolean checkReady() {
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
			View singleMarker = getActivity().getLayoutInflater().inflate(R.layout.widget_map_marker, null);
			mIconGenerator.setContentView(singleMarker);
			mLabel = (TextView) singleMarker.findViewById(R.id.entity_text);
			mLabel.setBackground(new ColorDrawable(Colors.getColor(R.color.brand_primary)));
			mImage = (AirImageView) singleMarker.findViewById(R.id.entity_photo);
			mImage.setSizeHint(UI.getRawPixelsForDisplayPixels(50f));
		}

		@Override
		protected void onBeforeClusterItemRendered(final EntityItem entityItem, final MarkerOptions markerOptions) {

			final Place place = (Place) entityItem.mEntity;
			if (!TextUtils.isEmpty(place.name)) {
				markerOptions.title(place.name);
			}

			if (place.category != null && !TextUtils.isEmpty(place.category.name)) {

				mLabel.setText(place.category.name.substring(0, 1).toUpperCase(Locale.US));
				markerOptions.snippet(place.category.name);

				final Photo photo = (place.category.photo != null) ? place.category.photo : place.getDefaultPhoto();
				markerOptions.title(place.id);

				new AsyncTask() {

					@Override
					protected void onPreExecute() {}

					@Override
					protected Object doInBackground(Object... params) {

						Thread.currentThread().setName("AsyncGetEntity");
						ModelResult result = new ModelResult();
						try {
							Bitmap bitmap = DownloadManager.with(Aircandi.applicationContext)
							                               .load(photo.getUri())
							                               .centerInside()
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
							reloadMarker(entityItem, (Bitmap) result.data);
						}
					}

				}.execute();
			}

			mImage.getImageView().setImageBitmap(null);
			Bitmap icon = mIconGenerator.makeIcon();
			markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
		}

		public void reloadMarker(EntityItem item, Bitmap bitmap) {

			Collection<Marker> markers = mClusterManager.getMarkerCollection().getMarkers();
			for (Marker marker : markers) {
				if (item.mEntity.id.equals(marker.getTitle())) {
					mImage.getImageView().setImageBitmap(bitmap);
					Bitmap icon = mIconGenerator.makeIcon();
					marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
					if (!TextUtils.isEmpty(item.mEntity.name)) {
						marker.setTitle(item.mEntity.name);
					}
					break;
				}
			}
		}
	}
}
