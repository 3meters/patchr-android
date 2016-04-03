package com.patchr.ui.edit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.objects.AirLocation;
import com.patchr.objects.TransitionType;
import com.patchr.ui.BaseScreen;
import com.patchr.utilities.Json;
import com.patchr.utilities.UI;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class LocationEdit extends BaseScreen implements GoogleMap.OnMapClickListener
		, GoogleMap.OnMarkerDragListener {

	protected MapView     mapView;
	protected GoogleMap   map;
	protected AirLocation originalLocation;
	protected String      title;
	protected Marker      marker;
	protected boolean     dirty;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Bind is called from map ready callback.
	}

	@Override public void onResume() {
		if (mapView != null) mapView.onResume();
		super.onResume();
	}

	@Override public void onPause() {
		if (mapView != null) mapView.onPause();
		super.onPause();
	}

	@Override public void onDestroy() {
		super.onDestroy();
		if (mapView != null) mapView.onDestroy();
	}

	@Override public void onLowMemory() {
		super.onLowMemory();
		if (mapView != null) mapView.onLowMemory();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_save, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			this.submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onMapClick(LatLng latLng) {
		marker.setPosition(latLng);
		dirty = true;
	}

	@Override public void onMarkerDragStart(Marker marker) {}

	@Override public void onMarkerDrag(Marker marker) {}

	@Override public void onMarkerDragEnd(Marker marker) {
		dirty = true;
	}

	@Override protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (mapView != null) mapView.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			title = extras.getString(Constants.EXTRA_TITLE);
			final String json = extras.getString(Constants.EXTRA_LOCATION);
			if (json != null) {
				originalLocation = (AirLocation) Json.jsonToObject(json, Json.ObjectType.AIR_LOCATION);
			}
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mapView = (MapView) findViewById(R.id.mapview);
		if (mapView != null) {
			mapView.onCreate(savedInstanceState);
			mapView.getMapAsync(new OnMapReadyCallback() {
				@Override public void onMapReady(GoogleMap googleMap) {
					map = googleMap;
					if (checkReady()) {
						setUpMap();
						if (originalLocation != null) {
							map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(originalLocation.lat.doubleValue(), originalLocation.lng.doubleValue()), 17));
						}
						bind();
					}
				}
			});
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_location;
	}

	@Override protected int getTransitionBack(int transitionType) {
		return super.getTransitionBack(TransitionType.BUILDER_BACK);
	}

	@Override public void submitAction() {
		save();
	}

	public void bind() {

		if (originalLocation != null) {
			marker = map.addMarker(new MarkerOptions()
					.position(new LatLng(originalLocation.lat.doubleValue(), originalLocation.lng.doubleValue()))
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
					.draggable(true));
			marker.setAnchor(0.5f, 0.8f);
			if (title != null) {
				marker.setTitle(title);
				marker.showInfoWindow();
			}
		}
	}

	private void save() {

		if (!dirty) {
			cancelAction(true);
			return;
		}

		final Intent intent = new Intent();
		AirLocation location = new AirLocation(marker.getPosition().latitude, marker.getPosition().longitude);
		location.accuracy = 1;
		String json = Json.objectToJson(location);
		intent.putExtra(Constants.EXTRA_LOCATION, json);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
	}

	private boolean checkReady() {
		/*
		 * Parent activity performs play services check. We can't get to
		 * here if they are not available.
		 */
		if (map == null) {
			UI.toast("Map not ready");
			return false;
		}
		return true;
	}

	private void setUpMap() {

		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		map.setLocationSource(null);

		UiSettings uiSettings = map.getUiSettings();

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			map.setMyLocationEnabled(true);
			uiSettings.setMyLocationButtonEnabled(true);
		}

		uiSettings.setZoomControlsEnabled(true);
		uiSettings.setAllGesturesEnabled(true);
		uiSettings.setCompassEnabled(true);

		MapsInitializer.initialize(this);
		map.setOnMapClickListener(this);
		map.setOnMarkerDragListener(this);
	}
}