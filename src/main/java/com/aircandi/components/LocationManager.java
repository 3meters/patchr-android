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
import com.aircandi.components.TrackerBase.TrackerCategory;
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
public class LocationManager {

	public static final Double RADIUS_EARTH_MILES      = 3958.75;
	public static final Double RADIUS_EARTH_KILOMETERS = 6371.0;
	public static final float  MetersToMilesConversion = 0.000621371192237334f;
	public static final float  MetersToFeetConversion  = 3.28084f;
	public static final float  MetersToYardsConversion = 1.09361f;
	public static final float  FeetToMetersConversion  = 0.3048f;

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	public final static int ACCURACY_PREFERRED                    = 50;

	protected android.location.LocationManager mLocationManager;
	protected LocationClient                   mLocationClient;
	protected LocationRequest                  mLocationRequest;
	protected LocationListener                 mLocationListener;
	private   Runnable                         mLocationTimeout;
	private   AirLocation                      mAirLocationLocked;
	private   Location                         mLocationLast;
	private   Location                         mLocationLocked;

	private LocationManager() {
		mLocationManager = (android.location.LocationManager) Patchr.applicationContext.getSystemService(Context.LOCATION_SERVICE);
		mLocationTimeout = new Runnable() {

			@Override
			public void run() {

				Logger.d(LocationManager.this, "Location fix attempt aborted: timeout: ** done **");
				Patchr.stopwatch2.segmentTime("Location fix attempt aborted: timeout");
				Patchr.mainThreadHandler.removeCallbacks(mLocationTimeout);

				Patchr.tracker.sendTiming(TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
						, "location_timeout"
						, NetworkManager.getInstance().getNetworkType());

				BusProvider.getInstance().post(new BurstTimeoutEvent());
			}
		};
	}

	private static class LocationManagerHolder {
		public static final LocationManager instance = new LocationManager();
	}

	public static LocationManager getInstance() {
		return LocationManagerHolder.instance;
	}

	public void requestLocation(final Context context) {

		mLocationLocked = null;

		/*
		 * For now we use high accuracy in all cases so we know how location is
		 * being determined.
		 */
		final int priority = NetworkManager.getInstance().isWifiEnabled()
		                     ? LocationRequest.PRIORITY_HIGH_ACCURACY
		                     : LocationRequest.PRIORITY_HIGH_ACCURACY;

		Reporting.updateCrashKeys();
		mLocationClient = new LocationClient(context,

				new GooglePlayServicesClient.ConnectionCallbacks() {

					@Override
					public void onConnected(Bundle bundle) {
						mLocationRequest = LocationRequest.create()
						                                         .setPriority(priority)
						                                         .setInterval(Constants.TIME_FIVE_SECONDS)
						                                         .setFastestInterval(Constants.TIME_FIVE_SECONDS)
						                                         .setNumUpdates(5)
						                                         .setExpirationDuration(Constants.TIME_THIRTY_SECONDS);
						mLocationListener = new LocationListener() {

							@Override
							public void onLocationChanged(Location location) {
								Logger.d(context, "Location changed: " + (location == null ? "null" : location.toString()));
								if (Patchr.stopwatch2.isStarted()) {
									Patchr.stopwatch2.segmentTime("Lock location: update: accuracy = " + (location.hasAccuracy() ? location.getAccuracy() : "none"));
								}
								if (location.hasAccuracy()) {
									if (Patchr.getInstance().getPrefEnableDev()) {
										UI.showToastNotification("Location accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT);
									}
									if (location.getAccuracy() <= ACCURACY_PREFERRED) {
										Patchr.tracker.sendTiming(TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
												, "location_accepted"
												, NetworkManager.getInstance().getNetworkType());
									}
								}
								mLocationLast = location;
								BusProvider.getInstance().post(new LocationChangedEvent(mLocationLast));
							}
						};

						mLocationClient.requestLocationUpdates(mLocationRequest, mLocationListener);

						/* We don't get a callback so setup a more official timeout */
						Patchr.mainThreadHandler.postDelayed(mLocationTimeout, Constants.TIME_THIRTY_SECONDS);
					}

					@Override
					public void onDisconnected() {}
				},

				new GooglePlayServicesClient.OnConnectionFailedListener() {

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
							}
							catch (IntentSender.SendIntentException e) {
								/* Thrown if Google Play services canceled the original PendingIntent */
								Reporting.logException(e);
							}
						}
						else {
							AndroidManager.showPlayServicesErrorDialog(connectionResult.getErrorCode()
									, Patchr.getInstance().getCurrentActivity());
						}
					}
				});

		mLocationClient.connect();
	}

	public void stop() {
		if (mLocationClient != null && mLocationListener != null && mLocationClient.isConnected()) {
			mLocationClient.removeLocationUpdates(mLocationListener);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

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
}
