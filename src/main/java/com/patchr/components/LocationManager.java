package com.patchr.components;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.events.LocationUpdatedEvent;
import com.patchr.objects.AirLocation;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

	private AirLocation mAirLocationLocked;
	private Location    mLocationLastKnown;
	private Location    mLocationLocked;
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

	@Override
	public void onConnected(Bundle bundle) {
		Logger.d(this, "Google location api connected");
		if (mRequestingLocationUpdates) {
			requestLocationUpdates();
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		Logger.d(this, "Google location api connection suspended");
		/* Use flag to determine if connection shut down on purpose. */
		if (mRequestingLocationUpdates) {
			mGoogleApiClient.connect();
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
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
			else {
				/* Display a dialog to the user with the error. */
				AndroidManager.showPlayServicesErrorDialog(result.getErrorCode()
						, Patchr.getInstance().getCurrentActivity()
						, new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						mResolvingError = false;
					}
				});
				mResolvingError = true;
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		if (location == null) return;

		if (!mFirstAccept || mFirstAccepted) {

			/* Discard first location update if close to last known location */
			if (mLocationLastKnown != null && mLocationLastKnown != location) {
				Float distance = mLocationLastKnown.distanceTo(location);
				Logger.v(this, "Location distance from last known: " + distance);
				if (distance <= Constants.DIST_TWENTY_FIVE_METERS) {
					Logger.v(this, "Location skipped because close to last known: " + location.toString());
					mLocationLastKnown = null;
					return;
				}
			}

			/* Discard if location if close to last locked location - could be last known */
			if (mLocationLocked != null) {
				Float distance = mLocationLocked.distanceTo(location);
				Logger.v(this, "Location distance from last locked: " + distance);
				if (distance <= Constants.DIST_TWENTY_FIVE_METERS) {
					Logger.v(this, "Location skipped because close to last locked: " + location.toString());
					mLocationLastKnown = null;
					return;
				}
			}
		}

		Logger.d(this, "Location changed: " + location.toString());
		mFirstAccepted = true;
		if (mLocationLastKnown != null && mLocationLastKnown == location) {
			Logger.d(this, "Using last known: " + location.toString());
		}

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

		Dispatcher.getInstance().post(new LocationUpdatedEvent(location));
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
		if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Patchr.getInstance().getCurrentUser() != null
				&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer)
				&& Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_location_high_accuracy), false)) {
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
		Logger.d(this, "Stopping location updates");
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			mRequestingLocationUpdates = false;
		}
	}

	public void requestLocationUpdates() {
		Logger.d(this, "Starting location updates");

		/* Get last known location */
		mLocationLastKnown = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (mLocationLastKnown != null) {
			onLocationChanged(mLocationLastKnown);
		}

		/* Start updates */
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient
				, mLocationRequest
				, this);
	}

	/* Public */

	@NonNull
	public Boolean hasMoved(Location locationCandidate) {
		if (mLocationLocked == null) return true;
		final float distance = mLocationLocked.distanceTo(locationCandidate);
		return (distance >= mLocationRequest.getSmallestDisplacement());
	}

	@NonNull
	public Boolean isLocationAccessEnabled() {
		return (mLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
				|| mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
	}

	@NonNull
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

	@NonNull
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
		mAirLocationLocked = location;  // Only place this is being set
	}

	public AirLocation getAirLocationLocked() {
		return mAirLocationLocked;
	}
}
