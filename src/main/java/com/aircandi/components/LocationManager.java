package com.aircandi.components;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
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
public class LocationManager implements
                             GooglePlayServicesClient.ConnectionCallbacks,
                             GooglePlayServicesClient.OnConnectionFailedListener {

	public static final Double RADIUS_EARTH_MILES      = 3958.75;
	public static final Double RADIUS_EARTH_KILOMETERS = 6371.0;
	public static final float  MetersToMilesConversion = 0.000621371192237334f;
	public static final float  MetersToFeetConversion  = 3.28084f;
	public static final float  MetersToYardsConversion = 1.09361f;
	public static final float  FeetToMetersConversion  = 0.3048f;

	private final static int     CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	public final static  int     ACCURACY_PREFERRED                    = 50;
	public final static  float   MIN_DISPLACEMENT                      = 50.0f;
	public static final  Integer FUZZY_THRESHOLD                       = Constants.DIST_FIVE_HUNDRED_METERS;

	protected android.location.LocationManager mLocationManager;        // Just so we can get config info
	protected LocationClient                   mLocationClient;
	protected LocationRequest                  mLocationRequest;
	protected LocationListener                 mLocationListener;

	private Runnable    mLocationTimeout;
	private AirLocation mAirLocationLocked;
	private Location    mLocationLast;
	private Location    mLocationLocked;
	private Boolean mUseTimeout = false;

	private static class LocationManagerHolder {
		public static final LocationManager instance = new LocationManager();
	}

	public static LocationManager getInstance() {
		return LocationManagerHolder.instance;
	}

	private LocationManager() {

		mLocationClient = new LocationClient(Patchr.applicationContext, this, this);
		mLocationManager = (android.location.LocationManager) Patchr.applicationContext.getSystemService(Context.LOCATION_SERVICE);
		mLocationTimeout = new Runnable() {

			@Override
			public void run() {
				Logger.d(LocationManager.this, "Location fix attempt aborted: timeout: ** done **");
				Patchr.stopwatch2.segmentTime("Location fix attempt aborted: timeout");

				/* Shutdown all location processing */
				stop();

				Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
						, "location_timeout"
						, NetworkManager.getInstance().getNetworkType());

				BusProvider.getInstance().post(new BurstTimeoutEvent());
			}
		};
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onConnected(Bundle bundle) {

		/* Start updates */
		mLocationClient.requestLocationUpdates(mLocationRequest, mLocationListener);

		/* We don't get a callback so setup a more official timeout */
		if (mUseTimeout) {
			Patchr.mainThreadHandler.removeCallbacks(mLocationTimeout);
			Patchr.mainThreadHandler.postDelayed(mLocationTimeout, Constants.TIME_THIRTY_SECONDS);
		}
	}

	@Override
	public void onDisconnected() {
		/*
		 * When we disconnect on purpose, we also clear the listener. If listener
		 * still exists then try to reconnect and continue location processing.
		 */
		if (mLocationListener != null) {
			mLocationClient.connect();
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error has
		 * a resolution, try sending an Intent to start a Google Play services activity
		 * that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				/* Start an Activity that tries to resolve the error */
				connectionResult.startResolutionForResult((Activity) Patchr.applicationContext
						, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			}
			catch (IntentSender.SendIntentException e) {
				/* Thrown if Google Play services cancelled the original PendingIntent */
				Reporting.logException(e);
			}
		}
		else {
			AndroidManager.showPlayServicesErrorDialog(connectionResult.getErrorCode()
					, Patchr.getInstance().getCurrentActivity());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void requestLocationUpdates(final Context context) {

		setLocationLocked(null);
		Reporting.updateCrashKeys();

		/* Make sure we are starting from a disconnected state */
		Patchr.mainThreadHandler.removeCallbacks(mLocationTimeout);
		if (mLocationClient != null && (mLocationClient.isConnected() || mLocationClient.isConnecting())) {
			mLocationClient.disconnect();
		}

		/* Location request */
		mLocationRequest = LocationRequest.create()
		                                  .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
		                                  .setSmallestDisplacement(MIN_DISPLACEMENT)
		                                  .setInterval(Constants.TIME_FIFTEEN_SECONDS)
		                                  .setFastestInterval(Constants.TIME_FIFTEEN_SECONDS);

		/* Location listener */
		mLocationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {

				if (location == null) return;
				Logger.d(context, "Location changed: " + location.toString());
				if (Patchr.stopwatch2.isStarted()) {
					Patchr.stopwatch2.segmentTime("Lock location: update: accuracy = " + (location.hasAccuracy() ? location.getAccuracy() : "none"));
				}
				if (location.hasAccuracy()) {
					if (Patchr.getInstance().getPrefEnableDev()) {
						UI.showToastNotification("Location accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT);
					}
					if (location.getAccuracy() <= ACCURACY_PREFERRED) {
						Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
								, "location_accepted"
								, NetworkManager.getInstance().getNetworkType());
					}
				}

				mLocationLast = location;
				BusProvider.getInstance().post(new LocationChangedEvent(mLocationLast));
			}
		};

		mUseTimeout = false;
		mLocationClient.connect();
	}

	public void requestCurrentLocation(final Context context) {

		setLocationLocked(null);
		Reporting.updateCrashKeys();

		/* Make sure we are starting from a disconnected state */
		Patchr.mainThreadHandler.removeCallbacks(mLocationTimeout);
		if (mLocationClient != null && (mLocationClient.isConnected() || mLocationClient.isConnecting())) {
			mLocationClient.disconnect();
		}

		mLocationRequest = LocationRequest.create()
		                                  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
		                                  .setInterval(Constants.TIME_FIVE_SECONDS)
		                                  .setFastestInterval(Constants.TIME_FIVE_SECONDS)
		                                  .setNumUpdates(5)
		                                  .setExpirationDuration(Constants.TIME_THIRTY_SECONDS);

		mLocationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {

				if (location == null) return;
				Logger.d(context, "Location changed: " + location.toString());
				if (Patchr.stopwatch2.isStarted()) {
					Patchr.stopwatch2.segmentTime("Lock location: update: accuracy = " + (location.hasAccuracy() ? location.getAccuracy() : "none"));
				}
				if (location.hasAccuracy()) {
					if (Patchr.getInstance().getPrefEnableDev()) {
						UI.showToastNotification("Location accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT);
					}
					if (location.getAccuracy() <= ACCURACY_PREFERRED) {
						Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
								, "location_accepted"
								, NetworkManager.getInstance().getNetworkType());

						/* We self stop when getting a good enough location */
						stop();
					}
				}

				mLocationLast = location;
				BusProvider.getInstance().post(new LocationChangedEvent(mLocationLast));
			}
		};

		mUseTimeout = true;
		mLocationClient.connect();
	}

	public void stop() {
		Patchr.mainThreadHandler.removeCallbacks(mLocationTimeout);
		if (mLocationClient != null && (mLocationClient.isConnected() || mLocationClient.isConnecting())) {
			mLocationClient.disconnect();
		}
	}

	/* Public */

	public Boolean hasMoved(Location locationCandidate) {
		if (mLocationLocked == null) return true;
		final float distance = mLocationLocked.distanceTo(locationCandidate);
		return (distance >= mLocationRequest.getSmallestDisplacement());
	}

	public boolean isLocationAccessEnabled() {
		return (mLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
				|| mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
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

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Location getLocationLast() {
		return mLocationLast;
	}

	public Location getLocationLocked() {
		return mLocationLocked;
	}

	public synchronized void setLocationLocked(Location locationLocked) {

		mLocationLocked = locationLocked;  // Only place this is being set

		if (mLocationLocked == null || !mLocationLocked.hasAccuracy()) {
			mAirLocationLocked = null;
			return;
		}

		/* We set our version of location at the same time */

		AirLocation location = new AirLocation();
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
		mAirLocationLocked = location;  // Only place this is being set
	}

	public AirLocation getAirLocationLocked() {
		return mAirLocationLocked;
	}
}
