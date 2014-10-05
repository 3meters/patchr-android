package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.LocationManager;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class LocationPicker extends BaseActivity implements GoogleMap.OnMyLocationButtonClickListener
		, GoogleMap.OnMapClickListener
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.onCreate(savedInstanceState);
		mMap = mMapView.getMap();

		if (checkReady()) {
			setUpMap();
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mOriginalLocation.lat.doubleValue(), mOriginalLocation.lng.doubleValue()), 17));
		}
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
		Patch.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.BUILDER_TO_FORM);
	}

	@Override
	public void onMapClick(LatLng latLng) {
		mMarker.setPosition(latLng);
		mDirty = true;
	}

	@Override
	public boolean onMyLocationButtonClick() {
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Dialogs.locationServicesDisabled(this, new AtomicBoolean(false));
			return true;
		}
		return false;
	}

	@Override
	public void onMarkerDragStart(Marker marker) {}

	@Override
	public void onMarkerDrag(Marker marker) {}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		mDirty = true;
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
		Patch.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.BUILDER_TO_FORM);
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

		MapsInitializer.initialize(this);
		mMap.setOnMyLocationButtonClickListener(this);
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
		mMapView.onResume();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mMapView.onLowMemory();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.location_picker;
	}
}