package com.patchr.components;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.events.LocationStatusEvent;
import com.patchr.model.Location;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.LocationStatus;
import com.patchr.objects.enums.Preference;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

@SuppressWarnings("ucd")
public class LocationManager implements
                             GoogleApiClient.ConnectionCallbacks,
                             GoogleApiClient.OnConnectionFailedListener,
                             LocationListener {

	public static final Double RADIUS_EARTH_MILES      = 3958.75;
	public static final Double RADIUS_EARTH_KILOMETERS = 6371.0;
	public static final float  MetersToMilesConversion = 0.000621371192237334f;
	public static final float  MetersToFeetConversion  = 3.28084f;
	public static final float  MetersToYardsConversion = 1.09361f;
	public static final float  FeetToMetersConversion  = 0.3048f;

	private static final int     REQUEST_RESOLVE_ERROR = 1001;
	private              boolean mResolvingError       = false;
	public final static  int     ACCURACY_PREFERRED    = 50;
	public final static  float   MIN_DISPLACEMENT      = 50.0f;
	public static final  Integer FUZZY_THRESHOLD       = Constants.DIST_FIVE_HUNDRED_METERS;

	protected android.location.LocationManager mLocationManager;        // Just so we can get config info
	protected GoogleApiClient                  mGoogleApiClient;
	protected LocationRequest                  mLocationRequest;

	private Location                  mLocationLocked;
	private android.location.Location mAndroidLocationLastKnown;
	private android.location.Location mAndroidLocationLocked;
	private Boolean mRequestingLocationUpdates = false;
	private Boolean mFirstAccept               = false;
	private Boolean mFirstAccepted             = false;

	private static class LocationManagerHolder {
		public static final LocationManager instance = new LocationManager();
	}

	public static LocationManager getInstance() {
		return LocationManagerHolder.instance;
	}

	private LocationManager() {

		mGoogleApiClient = new GoogleApiClient.Builder(Patchr.applicationContext)
			.addApi(LocationServices.API)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.build();

		mLocationManager = (android.location.LocationManager) Patchr.applicationContext.getSystemService(Context.LOCATION_SERVICE);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onConnected(Bundle bundle) {
		Logger.d(this, "Google location api connected");
		if (mRequestingLocationUpdates) {
			requestLocationUpdates();
		}
	}

	@Override public void onConnectionSuspended(int i) {
		Logger.d(this, "Google location api connection suspended");
		/* Use flag to determine if connection shut down on purpose. */
		if (mRequestingLocationUpdates) {
			mGoogleApiClient.connect();
		}
	}

	@Override public void onConnectionFailed(@NonNull ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects. If the error has a
		 * resolution, try sending an Intent to start a Google Play services activity
		 * that can resolve error.
		 */
		Logger.d(this, "Google location api connection failed");
		if (!mResolvingError) {
			if (result.hasResolution()) {
				try {
					mResolvingError = true;
					result.startResolutionForResult((Activity) Patchr.applicationContext, REQUEST_RESOLVE_ERROR);
				}
				catch (IntentSender.SendIntentException e) {
					/* There was an error with the resolution intent. Try again. */
					Reporting.logException(e);
					mGoogleApiClient.connect();
				}
			}
		}
	}

	@Override public void onLocationChanged(android.location.Location location) {

		if (location == null) return;

		if (!mFirstAccept || mFirstAccepted) {

			/* Discard first location update if close to last known location */
			if (mAndroidLocationLastKnown != null && mAndroidLocationLastKnown != location) {
				Float distance = mAndroidLocationLastKnown.distanceTo(location);
				Logger.v(this, "Location distance from last known: " + distance);
				if (distance <= Constants.DIST_TWENTY_FIVE_METERS) {
					Logger.v(this, "Location skipped because close to last known: " + location.toString());
					mAndroidLocationLastKnown = null;
					return;
				}
			}

			/* Discard if location if close to last locked location - could be last known */
			if (mAndroidLocationLocked != null) {
				Float distance = mAndroidLocationLocked.distanceTo(location);
				Logger.v(this, "Location distance from last locked: " + distance);
				if (distance <= Constants.DIST_TWENTY_FIVE_METERS) {
					Logger.v(this, "Location skipped because close to last locked: " + location.toString());
					mAndroidLocationLastKnown = null;
					return;
				}
			}
		}

		Logger.d(this, "Location changed: " + location.toString());
		mFirstAccepted = true;
		if (mAndroidLocationLastKnown != null && mAndroidLocationLastKnown == location) {
			Logger.d(this, "Using last known: " + location.toString());
		}

		if (Patchr.stopwatch2.isStarted()) {
			Patchr.stopwatch2.segmentTime("Lock location: update: accuracy = " + (location.hasAccuracy() ? location.getAccuracy() : "none"));
		}
		if (location.hasAccuracy()) {
			if (Patchr.getInstance().prefEnableDev) {
				UI.toast("Location accuracy: " + location.getAccuracy());
			}
			if (location.getAccuracy() <= ACCURACY_PREFERRED) {
				Reporting.sendTiming(AnalyticsCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
					, "location_accepted"
					, NetworkManager.getInstance().getNetworkType());
			}
		}

		Dispatcher.getInstance().post(new LocationStatusEvent(LocationStatus.UPDATED, location));
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void start(Boolean firstAccept) {

		if (mRequestingLocationUpdates) return;

		mRequestingLocationUpdates = true;
		mFirstAccept = firstAccept;
		mFirstAccepted = false;
		Reporting.updateCrashKeys();

		/* Location request */

		Boolean tethered = NetworkManager.getInstance().isWifiTethered();

		/* Developers can turn on high accuracy processing */
		if (Utils.devModeEnabled() && Patchr.settings.getBoolean(Preference.ENABLE_LOCATION_HIGH_ACCURACY, false)) {
			mLocationRequest = LocationRequest.create()
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
				.setSmallestDisplacement(MIN_DISPLACEMENT)
				.setInterval(Constants.TIME_FIFTEEN_SECONDS)
				.setFastestInterval(Constants.TIME_FIVE_SECONDS);
		}
		/*
		 * Balanced doesn't allow gps so if wifi isn't available then grind
		 * our teeth and opt for high accuracy (which supports gps).
		 */
		else if (tethered || (!NetworkManager.getInstance().isWifiEnabled())) {
			mLocationRequest = LocationRequest.create()
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
				.setSmallestDisplacement(MIN_DISPLACEMENT)
				.setInterval(Constants.TIME_FIFTEEN_SECONDS)
				.setFastestInterval(Constants.TIME_FIVE_SECONDS);
		}
		/* Normal request */
		else {
			mLocationRequest = LocationRequest.create()
				.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
				.setSmallestDisplacement(MIN_DISPLACEMENT)
				.setInterval(Constants.TIME_FIFTEEN_SECONDS)
				.setFastestInterval(Constants.TIME_FIFTEEN_SECONDS);
		}

		if (mGoogleApiClient.isConnected()) {
			requestLocationUpdates();
		}
		else {
			mGoogleApiClient.connect();
		}
	}

	public void stop() {
		Logger.v(this, "Stopping location updates");
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			mRequestingLocationUpdates = false;
		}
	}

	public void requestLocationUpdates() {
		Logger.v(this, "Starting location updates");

		/* Get last known location */
		if (ActivityCompat.checkSelfPermission(Patchr.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			mAndroidLocationLastKnown = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			if (mAndroidLocationLastKnown != null) {
				onLocationChanged(mAndroidLocationLastKnown);
			}

			/* Start updates */
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient
				, mLocationRequest
				, this);
		}
	}

	/* Public */

	@NonNull public Boolean hasMoved(android.location.Location locationCandidate) {
		if (mAndroidLocationLocked == null) return true;
		final float distance = mAndroidLocationLocked.distanceTo(locationCandidate);
		return (distance >= mLocationRequest.getSmallestDisplacement());
	}

	@NonNull public Boolean isLocationAccessEnabled() {
		return (mLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
			|| mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public android.location.Location getAndroidLocationLocked() {
		return mAndroidLocationLocked;
	}

	public synchronized void setAndroidLocationLocked(android.location.Location androidLocationLocked) {

		mAndroidLocationLocked = androidLocationLocked;  // Only place this is being set

		if (mAndroidLocationLocked == null || !mAndroidLocationLocked.hasAccuracy()) {
			mLocationLocked = null;
			return;
		}

		/* We set our version of location at the same time */

		Location location = new Location();
		location.lat = mAndroidLocationLocked.getLatitude();
		location.lng = mAndroidLocationLocked.getLongitude();

		if (mAndroidLocationLocked.hasAltitude()) {
			location.altitude = mAndroidLocationLocked.getAltitude();
		}
		if (mAndroidLocationLocked.hasAccuracy()) {
						/* In meters. */
			location.accuracy = mAndroidLocationLocked.getAccuracy();
		}
		if (mAndroidLocationLocked.hasBearing()) {
						/* Direction of travel in degrees East of true North. */
			location.bearing = mAndroidLocationLocked.getBearing();
		}
		if (mAndroidLocationLocked.hasSpeed()) {
						/* Speed of the device over ground in meters/second. */
			location.speed = mAndroidLocationLocked.getSpeed();
		}
		location.provider = mAndroidLocationLocked.getProvider();
		mLocationLocked = location;  // Only place this is being set
	}

	public Location getLocationLocked() {
		return mLocationLocked;
	}
}
