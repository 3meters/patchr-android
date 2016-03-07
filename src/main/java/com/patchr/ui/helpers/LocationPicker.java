package com.patchr.ui.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

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
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.Json;
import com.patchr.utilities.UI;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class LocationPicker extends BaseActivity implements GoogleMap.OnMapClickListener
		, GoogleMap.OnMarkerDragListener {

	protected MapView     mMapView;
	protected GoogleMap   mMap;
	protected AirLocation mOriginalLocation;
	protected String      mTitle;
	protected Marker      mMarker;
	protected Boolean mDirty = false;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mTitle = extras.getString(Constants.EXTRA_TITLE);
			final String json = extras.getString(Constants.EXTRA_LOCATION);
			if (json != null) {
				mOriginalLocation = (AirLocation) Json.jsonToObject(json, Json.ObjectType.AIR_LOCATION);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.onCreate(savedInstanceState);
		mMapView.getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap googleMap) {
				mMap = googleMap;
				if (checkReady()) {
					setUpMap();
					if (mOriginalLocation != null) {
						mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mOriginalLocation.lat.doubleValue(), mOriginalLocation.lng.doubleValue()), 17));
					}
				}
			}
		});
	}

	public void draw(View view) {}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAccept() {
		save();
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
	}

	@Override
	public void onMapClick(LatLng latLng) {
		mMarker.setPosition(latLng);
		mDirty = true;
	}

	@Override
	public void onMarkerDragStart(Marker marker) {}

	@Override
	public void onMarkerDrag(Marker marker) {}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		mDirty = true;
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (mMapView != null) mMapView.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void save() {

		if (!mDirty) {
			onCancel(true);
			return;
		}

		final Intent intent = new Intent();
		AirLocation location = new AirLocation(mMarker.getPosition().latitude, mMarker.getPosition().longitude);
		location.accuracy = 1;
		String json = Json.objectToJson(location);
		intent.putExtra(Constants.EXTRA_LOCATION, json);
		setResultCode(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
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

		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.setLocationSource(null);

		UiSettings uiSettings = mMap.getUiSettings();

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			mMap.setMyLocationEnabled(true);
			uiSettings.setMyLocationButtonEnabled(true);
		}

		uiSettings.setZoomControlsEnabled(true);
		uiSettings.setAllGesturesEnabled(true);
		uiSettings.setCompassEnabled(true);

		MapsInitializer.initialize(this);
		mMap.setOnMapClickListener(this);
		mMap.setOnMarkerDragListener(this);

		if (mOriginalLocation != null) {
			mMarker = mMap.addMarker(new MarkerOptions()
					.position(new LatLng(mOriginalLocation.lat.doubleValue(), mOriginalLocation.lng.doubleValue()))
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
					.draggable(true));
			mMarker.setAnchor(0.5f, 0.8f);
			if (mTitle != null) {
				mMarker.setTitle(mTitle);
				mMarker.showInfoWindow();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onResume() {
		if (mMapView != null) mMapView.onResume();
		super.onResume();
	}

	@Override
	public void onPause() {
		if (mMapView != null) mMapView.onPause();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mMapView != null) mMapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if (mMapView != null) mMapView.onLowMemory();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.location_picker;
	}
}