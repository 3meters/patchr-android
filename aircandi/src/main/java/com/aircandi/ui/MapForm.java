package com.aircandi.ui;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.utilities.Errors;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapForm extends BaseEntityForm {

	SupportMapFragment mMapFragment;
	private static final int DEFAULT_ZOOM = 16;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;
		mMapFragment = new MapFragment();
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, mMapFragment).commit();
	}

	@Override
	public void draw() {
		mFirstDraw = false;
		if (!TextUtils.isEmpty(mEntity.name)) {
			setActivityTitle(mEntity.name);
		}
		/*
		 * Map requires OpenGL ES version 2. We specify this feature and version
		 * as required in the manifest so we assume that if we run then we have what
		 * we need.
		 */
		drawMap();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void drawMap() {

		if (mEntity.getLocation() != null) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					/* Show busy */
					mBusy.showBusy(BusyAction.Loading);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("AsyncMarkMapLocation");
					ModelResult result = new ModelResult();
					if (((Place) mEntity).fuzzy) {
						String address = ((Place) mEntity).getAddressString(true);
						if (!TextUtils.isEmpty(address)) {
							try {
								Geocoder geocoder = new Geocoder(Aircandi.applicationContext, Locale.getDefault());
								List<Address> addresses = geocoder.getFromLocationName(address, 1);
								if (addresses != null && addresses.size() > 0) {
									Address geolookup = addresses.get(0);
									if (geolookup.hasLatitude() && geolookup.hasLongitude()) {
										mEntity.location.lat = geolookup.getLatitude();
										mEntity.location.lng = geolookup.getLongitude();
										mEntity.location.accuracy = 25;
										mEntity.fuzzy = false;
									}
								}
							}
							catch (IOException exception) {
								result.serviceResponse.responseCode = ResponseCode.FAILED;
								result.serviceResponse.exception = exception;
								result.serviceResponse.errorResponse = Errors.getErrorResponse(Aircandi.applicationContext, result.serviceResponse);
							}
						}
					}
					return result;
				}

				@Override
				protected void onPostExecute(Object modelResult) {
					if (isFinishing()) return;

					final ModelResult result = (ModelResult) modelResult;
					mBusy.hideBusy(false);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

						AirLocation location = mEntity.getLocation();
						MarkerOptions options = new MarkerOptions().position(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()));
						Place place = (Place) mEntity;

						if (place.name != null) {
							options.title(mEntity.name);
						}

						if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
							options.snippet(place.category.name);
						}

						Marker marker = mMapFragment.getMap().addMarker(options);
						marker.showInfoWindow();
						mMapFragment.getMap().moveCamera(
								CameraUpdateFactory.newLatLngZoom(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()), DEFAULT_ZOOM));
					}
					else {
						Errors.handleError(MapForm.this, result.serviceResponse);
					}
				}

			}.execute();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.navigate) {
			AirLocation location = mEntity.getLocation();
			String address = ((Place) mEntity).getAddressString(true);

			if (!((Place) mEntity).fuzzy) {
				address = null;
			}

			AndroidManager.getInstance().callMapNavigation(this
					, location.lat.doubleValue()
					, location.lng.doubleValue()
					, address
					, mEntity.name);
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.map_form;
	}
}