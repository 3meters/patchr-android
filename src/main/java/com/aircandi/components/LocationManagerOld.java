package com.aircandi.components;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.events.BurstTimeoutEvent;
import com.aircandi.events.LocationChangedEvent;
import com.aircandi.objects.AirLocation;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ucd")
public class LocationManagerOld implements
                             GooglePlayServicesClient.ConnectionCallbacks,
                             GooglePlayServicesClient.OnConnectionFailedListener,
                             LocationListener {

	public static final Double RADIUS_EARTH_MILES      = 3958.75;
	public static final Double RADIUS_EARTH_KILOMETERS = 6371.0;
	public static final float  MetersToMilesConversion = 0.000621371192237334f;
	public static final float  MetersToFeetConversion  = 3.28084f;
	public static final float  MetersToYardsConversion = 1.09361f;
	public static final float  FeetToMetersConversion  = 0.3048f;

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private final static int ACCURACY_PREFERRED                    = 50;

	protected LocationMode mLocationMode = LocationMode.NONE;

	protected android.location.LocationManager mLocationManager;
	protected LocationClient                   mLocationClient;
	protected LocationRequest                  mLocationRequest;
	private   Runnable                         mBurstTimeout;
	private   AirLocation                      mAirLocationLocked;
	private   Location                         mLocationLast;
	private   Location                         mLocationLocked;

	private LocationManagerOld() {}

	private static class LocationManagerHolder {
		public static final LocationManagerOld instance = new LocationManagerOld();
	}

	public static LocationManagerOld getInstance() {
		return LocationManagerHolder.instance;
	}

	public void initialize(Context applicationContext) {
		Logger.d(this, "Initializing the LocationManager");
		/*
		 * initialize only called when the app is first initialized.
		 */
		mBurstTimeout = new Runnable() {

			@Override
			public void run() {

				Logger.d(LocationManagerOld.this, "Location fix attempt aborted: timeout: ** done **");
				Patchr.stopwatch2.segmentTime("Location fix attempt aborted: timeout");
				Patchr.mainThreadHandler.removeCallbacks(mBurstTimeout);

				Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
						, "location_timeout"
						, NetworkManager.getInstance().getNetworkType());

				setLocationMode(LocationMode.OFF);
				BusProvider.getInstance().post(new BurstTimeoutEvent());
			}
		};

		/* Reset */
		mLocationLast = null;
		mLocationLocked = null;
		mAirLocationLocked = null;
		mLocationMode = LocationMode.NONE;

		/* Only used to check if location services are enabled */
		//mLocationManager = (android.location.LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
		
		/* Google location services */
		mLocationClient = new LocationClient(applicationContext, this, this);
		mLocationClient.connect();
	}

	public void simple(final Context context) {
		final int priority = NetworkManager.getInstance().isWifiEnabled()
		                     ? LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
		                     : LocationRequest.PRIORITY_HIGH_ACCURACY;

		mLocationClient = new LocationClient(context, new GooglePlayServicesClient.ConnectionCallbacks() {

			@Override
			public void onConnected(Bundle bundle) {
				LocationRequest request = LocationRequest.create()
				                                         .setPriority(priority)
				                                         .setInterval(Constants.TIME_ONE_MINUTE)
				                                         .setNumUpdates(1)
				                                         .setExpirationDuration(10000)
				                                         .setFastestInterval(Constants.TIME_ONE_MINUTE);

				mLocationClient.requestLocationUpdates(request, new LocationListener() {
					@Override
					public void onLocationChanged(Location location) {
						Logger.d(context, "Location changed: " + (location == null ? "null" : location.toString()));
						mLocationLast = location;
						BusProvider.getInstance().post(new LocationChangedEvent(mLocationLast));
					}
				});
			}

			@Override
			public void onDisconnected() {}
		}, null);
		mLocationClient.connect();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onLocationChanged(Location location) {

		Logger.d(this, "Location changed: " + (location == null ? "null" : location.toString()));
		if (location == null) {
			mLocationLast = null;
		}
		else {
			if (Patchr.stopwatch2.isStarted()) {
				Patchr.stopwatch2.segmentTime("Lock location: update: accuracy = " + (location.hasAccuracy() ? location.getAccuracy() : "none"));
			}

			if (mLocationMode == LocationMode.BURST_BALANCED) {
				if (location.hasAccuracy()) {
					if (Patchr.getInstance().getPrefEnableDev()) {
						UI.showToastNotification("Location accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT);
					}
					if (location.getAccuracy() <= ACCURACY_PREFERRED) {
						Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
								, "location_accepted"
								, NetworkManager.getInstance().getNetworkType());

						setLocationMode(LocationMode.OFF);
					}
				}
			}
			mLocationLast = location;
			BusProvider.getInstance().post(new LocationChangedEvent(mLocationLast));
		}
	}

	@Override
	public void onConnected(Bundle extras) {
		try {
			mLocationLast = mLocationClient.getLastLocation();
			processMode();
		}
		catch (IllegalStateException ignore) {}
	}

	@Override
	public void onDisconnected() {
		/*
		 * We attempt to rebuild our connection to the play services.
		 */
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				mLocationClient = new LocationClient(Patchr.applicationContext, LocationManagerOld.this, LocationManagerOld.this);
				mLocationClient.connect();
			}
		});
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve
		 * error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				/* Start an Activity that tries to resolve the error */
				connectionResult.startResolutionForResult((Activity) Patchr.applicationContext
						, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			}
			catch (IntentSender.SendIntentException e) {
				Reporting.logException(e);
			}
		}
		else {
			/*
			 * If no resolution is available, display a dialog to the
			 * user with the error.
			 */
			AndroidManager.showPlayServicesErrorDialog(connectionResult.getErrorCode(), Patchr.getInstance().getCurrentActivity());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/* Public */

	public void setLocationMode(LocationMode locationMode) {
		return;
		/*
		 * This is the external entry point that triggers use of the
		 * play services location client.
		 */
//		Reporting.updateCrashKeys();
//		Logger.d(LocationManager.this, "Location mode changed to: " + locationMode.name());
//		mLocationMode = locationMode;
//		processMode();
	}

	public Boolean hasMoved(Location locationCandidate) {

		if (mLocationLocked == null) return true;
		final float distance = mLocationLocked.distanceTo(locationCandidate);
		return (distance >= mLocationRequest.getSmallestDisplacement());
	}

	public boolean isLocationAccessEnabled() {
		return isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) || isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
	}

	public ModelResult getAddressForLocation(final AirLocation location) {
		/*
		 * Can trigger network access so should be called on a background thread.
		 */
		Thread.currentThread().setName("AsyncAddressForLocation");
		ModelResult result = new ModelResult();
		Geocoder geocoder = new Geocoder(Patchr.applicationContext, Locale.getDefault());

		try {
			result.data = geocoder.getFromLocation(location.lat.doubleValue(), location.lng.doubleValue(), 1);
		}
		catch (IOException e) {
			result.serviceResponse.responseCode = NetworkManager.ResponseCode.FAILED;
			result.serviceResponse.exception = e;
			result.serviceResponse.errorResponse = Errors.getErrorResponse(Patchr.applicationContext, result.serviceResponse);
		}

		return result;
	}

	public ModelResult getLocationFromAddress(String address) {
		/*
		 * Can trigger network access so should be called on a background thread.
		 */
		ModelResult result = new ModelResult();
		try {
			Geocoder geocoder = new Geocoder(Patchr.applicationContext, Locale.getDefault());
			List<Address> addresses = geocoder.getFromLocationName(address, 1);
			if (addresses != null && addresses.size() > 0) {
				Address geolookup = addresses.get(0);
				if (geolookup.hasLatitude() && geolookup.hasLongitude()) {
					AirLocation location = new AirLocation(geolookup.getLatitude(), geolookup.getLongitude());
					location.accuracy = 25;
					result.data = location;
				}
				else {
					result.serviceResponse.responseCode = NetworkManager.ResponseCode.FAILED;
				}
			}
		}
		catch (IOException exception) {
			result.serviceResponse.responseCode = NetworkManager.ResponseCode.FAILED;
			result.serviceResponse.exception = exception;
			result.serviceResponse.errorResponse = Errors.getErrorResponse(Patchr.applicationContext, result.serviceResponse);
		}
		return result;
	}

	/* Private */

	private void processMode() {
		/*
		 * First location comes asap even though interval might be long.
		 */
		if (!mLocationClient.isConnected()) {
			mLocationClient.connect();
			if (mLocationMode != LocationMode.NONE) {
				Patchr.mainThreadHandler.postDelayed(mBurstTimeout, Constants.TIME_THIRTY_SECONDS);
			}
		}
		else {
			try {
				mLocationClient.removeLocationUpdates(this);

				if (mLocationMode == LocationMode.BURST_BALANCED) {
					Patchr.stopwatch2.start("location_lock", "Lock balanced location: start");
					Logger.d(LocationManagerOld.this, "Lock balanced location started");
					onLocationChanged(null);

					mLocationRequest = LocationRequest.create()
							.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)      // We get credit for low battery usage.
							.setInterval(Constants.TIME_ONE_MINUTE)                          // This is used when attributing battery usage.
							.setFastestInterval(Constants.TIME_ONE_MINUTE);                     // How fast we'll take them if available from other apps.

					Patchr.mainThreadHandler.postDelayed(mBurstTimeout, Constants.TIME_THIRTY_SECONDS);
					mLocationClient.requestLocationUpdates(mLocationRequest, this);
				}

				if (mLocationMode == LocationMode.BURST_GPS) {
					Patchr.stopwatch2.start("location_lock", "Lock accurate location: start");
					Logger.d(LocationManagerOld.this, "Lock accurate location started");
					onLocationChanged(null);

					mLocationRequest = LocationRequest.create()
							.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)                // We get credit for high battery usage.
							.setInterval(Constants.TIME_ONE_MINUTE)                          // This is used when attributing battery usage.
							.setFastestInterval(Constants.TIME_ONE_MINUTE);                     // How fast we'll take them if available from other apps.

					Patchr.mainThreadHandler.postDelayed(mBurstTimeout, Constants.TIME_THIRTY_SECONDS);
					mLocationClient.requestLocationUpdates(mLocationRequest, this);
				}

				if (mLocationMode == LocationMode.OFF) {
					if (Patchr.stopwatch2.isStarted()) {
						Patchr.stopwatch2.stop("Lock location: stopped");
					}
					Logger.d(LocationManagerOld.this, "Lock location stopped: ** done **");

					Patchr.mainThreadHandler.removeCallbacks(mBurstTimeout);
					mLocationMode = LocationMode.NONE;
				}
			}
			catch (Exception ignore) {}
		}
	}

	private boolean isProviderEnabled(String provider) {
		return true;
		//return mLocationManager.isProviderEnabled(provider);
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public LocationMode getLocationMode() {
		return mLocationMode;
	}

	public Location getLocationLast() {
		return mLocationLast;
	}

	public Location getLocationLocked() {
		return mLocationLocked;
	}

	public void setLocationLocked(Location locationLocked) {
		mLocationLocked = locationLocked;
		if (locationLocked == null && mAirLocationLocked != null) {
			mAirLocationLocked.zombie = true;
		}
		else {

			if (mLocationLocked == null || !mLocationLocked.hasAccuracy()) {
				mAirLocationLocked = null;
				return;
			}

			AirLocation location = new AirLocation();

			synchronized (mLocationLocked) {

				if (Patchr.usingEmulator) {
					location = new AirLocation(47.616245, -122.201645); // earls
					location.provider = "emulator_lucky";
				}
				else {
					location.lat = mLocationLocked.getLatitude();
					location.lng = mLocationLocked.getLongitude();

					if (mLocationLocked.hasAltitude()) {
						location.altitude = mLocationLocked.getAltitude();
					}
					if (mLocationLocked.hasAccuracy()) {
						/* In meters. */
						location.accuracy = mLocationLocked.getAccuracy();
					}
					if (mLocationLocked.hasBearing()) {
						/* Direction of travel in degrees East of true North. */
						location.bearing = mLocationLocked.getBearing();
					}
					if (mLocationLocked.hasSpeed()) {
						/* Speed of the device over ground in meters/second. */
						location.speed = mLocationLocked.getSpeed();
					}
					location.provider = mLocationLocked.getProvider();
				}
			}
			mAirLocationLocked = location;
		}
	}

	public AirLocation getAirLocationLocked() {
		return mAirLocationLocked;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public enum LocationMode {
		BURST_BALANCED,
		BURST_GPS,
		OFF,
		NONE
	}
}
