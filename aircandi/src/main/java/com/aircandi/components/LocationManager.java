package com.aircandi.components;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.events.BurstTimeoutEvent;
import com.aircandi.events.LocationChangedEvent;
import com.aircandi.objects.AirLocation;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

@SuppressWarnings("ucd")
public class LocationManager implements
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

	protected android.location.LocationManager mLocationManager;
	protected LocationClient                   mLocationClient;
	protected LocationRequest                  mLocationRequest;
	protected LocationMode mLocationMode = LocationMode.NONE;
	private Runnable    mBurstTimeout;
	private AirLocation mAirLocationLocked;

	private Location mLocationLast;
	private Location mLocationLocked;

	private LocationManager() {
	}

	private static class LocationManagerHolder {
		public static final LocationManager instance = new LocationManager();
	}

	public static LocationManager getInstance() {
		return LocationManagerHolder.instance;
	}

	public void initialize(Context applicationContext) {

		Logger.d(this, "Initializing the LocationManager");

		/* Timeout handler */
		mBurstTimeout = new Runnable() {

			@Override
			public void run() {

				Logger.d(LocationManager.this, "Location fix attempt aborted: timeout: ** done **");
				Aircandi.stopwatch2.segmentTime("Location fix attempt aborted: timeout");
				Aircandi.mainThreadHandler.removeCallbacks(mBurstTimeout);

				Aircandi.tracker.sendTiming(TrackerCategory.PERFORMANCE, Aircandi.stopwatch2.getTotalTimeMills()
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
		mLocationManager = (android.location.LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);

		mLocationRequest = LocationRequest.create();
		mLocationClient = new LocationClient(applicationContext, this, this);
		mLocationClient.connect();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void onLocationChanged(Location location) {

		if (location == null) {
			Logger.d(this, "Location update: cleared");
			mLocationLast = null;
		}
		else {
			if (Aircandi.stopwatch2.isStarted()) {
				Aircandi.stopwatch2.segmentTime("Lock location: update: accuracy = " + (location.hasAccuracy() ? location.getAccuracy() : "none"));
			}

			if (mLocationMode == LocationMode.BURST) {
				if (location.hasAccuracy()) {
					if (Aircandi.getInstance().getPrefEnableDev()) {
						UI.showToastNotification("Location accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT);
					}
					if (location.getAccuracy() <= ACCURACY_PREFERRED) {
						Aircandi.tracker.sendTiming(TrackerCategory.PERFORMANCE, Aircandi.stopwatch2.getTotalTimeMills()
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
				mLocationClient = new LocationClient(Aircandi.applicationContext, LocationManager.this, LocationManager.this);
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
				connectionResult.startResolutionForResult((Activity) Aircandi.applicationContext
						, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			}
			catch (IntentSender.SendIntentException e) {
				e.printStackTrace();
			}
		}
		else {
			/*
			 * If no resolution is available, display a dialog to the
			 * user with the error.
			 */
			AndroidManager.showPlayServicesErrorDialog(connectionResult.getErrorCode(), Aircandi.getInstance().getCurrentActivity());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public void setLocationMode(LocationMode locationMode) {
		/*
		 * This is the external entry point that triggers use of the
		 * play services location client.
		 */

		Reporting.updateCrashKeys();
		Logger.d(LocationManager.this, "Location mode changed to: " + locationMode.name());
		mLocationMode = locationMode;
		processMode();

	}

	public void processMode() {

		if (!mLocationClient.isConnected()) {
			mLocationClient.connect();
			if (mLocationMode != LocationMode.NONE) {
				Aircandi.mainThreadHandler.postDelayed(mBurstTimeout, Constants.TIME_THIRTY_SECONDS);
			}
		}
		else {

			mLocationClient.removeLocationUpdates(this);

			if (mLocationMode == LocationMode.BURST) {
				Aircandi.stopwatch2.start("location_lock", "Lock location: start");
				Logger.d(LocationManager.this, "Lock location started");
				onLocationChanged(null);
				mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				mLocationRequest.setInterval(Constants.TIME_FIVE_SECONDS);
				mLocationRequest.setFastestInterval(Constants.TIME_FIVE_SECONDS);
				Aircandi.mainThreadHandler.postDelayed(mBurstTimeout, Constants.TIME_THIRTY_SECONDS);
				mLocationClient.requestLocationUpdates(mLocationRequest, this);
			}
			else if (mLocationMode == LocationMode.OFF) {
				if (Aircandi.stopwatch2.isStarted()) {
					Aircandi.stopwatch2.stop("Lock location: stopped");
				}
				Logger.d(LocationManager.this, "Lock location stopped: ** done **");
				Aircandi.mainThreadHandler.removeCallbacks(mBurstTimeout);
				mLocationMode = LocationMode.NONE;
			}
		}
	}

	public Boolean hasMoved(Location locationCandidate) {

		if (mLocationLocked == null) return true;
		final float distance = mLocationLocked.distanceTo(locationCandidate);
		return (distance >= mLocationRequest.getSmallestDisplacement());
	}

	private boolean isProviderEnabled(String provider) {
		return mLocationManager.isProviderEnabled(provider);
	}

	public boolean isLocationAccessEnabled() {
		return isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) || isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
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

	public void setLocationLocked(Location locationLocked) {
		mLocationLocked = locationLocked;
		if (locationLocked == null && mAirLocationLocked != null) {
			mAirLocationLocked.zombie = true;
		}
		else {
			mAirLocationLocked = getAirLocationForLockedLocation();
		}
	}

	public void setAirLocationLocked(AirLocation airLocationLocked) {
		mAirLocationLocked = airLocationLocked;
	}

	public AirLocation getAirLocationLocked() {
		return mAirLocationLocked;
	}

	public LocationMode getLocationMode() {
		return mLocationMode;
	}

	private AirLocation getAirLocationForLockedLocation() {

		AirLocation location = new AirLocation();

		if (mLocationLocked == null || !mLocationLocked.hasAccuracy()) return null;

		synchronized (mLocationLocked) {

			if (Aircandi.usingEmulator) {
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

		return location;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public enum LocationMode {
		BURST,
		OFF,
		NONE
	}
}
