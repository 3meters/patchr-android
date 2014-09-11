package com.aircandi.ui;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.LocationManager.LocationMode;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.RefreshReason;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.ProximityManager.WifiScanResult;
import com.aircandi.components.StringManager;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.controllers.ViewHolder;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.BurstTimeoutEvent;
import com.aircandi.events.EntitiesByProximityFinishedEvent;
import com.aircandi.events.EntitiesChangedEvent;
import com.aircandi.events.LocationChangedEvent;
import com.aircandi.events.MonitoringWifiScanReceivedEvent;
import com.aircandi.events.PlacesNearLocationFinishedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.ServiceData;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.squareup.otto.Subscribe;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RadarListFragment extends EntityListFragment {

	private final Handler mHandler = new Handler();

	private MenuItem mMenuItemBeacons;
	private TextView mBeaconIndicator;
	private String   mDebugWifi;
	private String mDebugLocation = "--";

	private Number mEntityModelBeaconDate;
	private Integer         mWifiStateLastSearch         = WifiManager.WIFI_STATE_UNKNOWN;
	private LocationHandler mLocationHandler             = new LocationHandler();
	private Boolean         mAtLeastOneLocationProcessed = false;

	private CacheStamp mCacheStamp;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view == null) return view;
		return view;
	}

	@Override
	public void bind(BindingMode mode) {
		Logger.d(this, "Binding called: mode = " + mode.name().toLowerCase(Locale.US));
		/*
		 * Cases that trigger a search
		 * 
		 * - First time radar is run
		 * - Preference change
		 * - Didn't complete location fix before user switched away from radar
		 * - While away, user enabled wifi
		 * - Beacons we used for last fix have changed
		 * - Beacon fix is thirty minutes old or more
		 * 
		 * Cases that trigger a ui refresh
		 * 
		 * - Preference change
		 * - EntityModel has changed since last search
		 */

		String bindReason = null;
		//noinspection LoopStatementThatDoesntLoop
		while (true) {

			if (mEntities.size() == 0) {
				/*
				 * This is either our first run or our last search turned up zilch (which could happen if you were in
				 * the middle of the ocean for instance.
				 */
				bindReason = "Empty";
				searchForPlaces();
				break;
			}

			if (!mAtLeastOneLocationProcessed) {
				/*
				 * User navigated away before first location entities could be processed by
				 * radar and displayed. Location could have been locked.
				 */
				bindReason = "No location processed";
				mBusy.showBusy(BusyAction.Scanning);
				LocationManager.getInstance().setLocationLocked(null);
				LocationManager.getInstance().setLocationMode(LocationMode.BURST);
				break;
			}

			if (LocationManager.getInstance().getLocationLocked() == null) {
				/*
				 * Gets set everytime we accept a location change in onLocationChange. Means
				 * we didn't get an acceptable fix yet from either the network or gps providers.
				 */
				bindReason = "No locked location";
				mBusy.showBusy(BusyAction.Scanning);
				LocationManager.getInstance().setLocationMode(LocationMode.BURST);
				break;
			}

			RefreshReason reason = ProximityManager.getInstance().beaconRefreshNeeded(LocationManager.getInstance().getLocationLocked());
			if (reason != RefreshReason.NONE) {

				bindReason = reason.name().toLowerCase(Locale.US);
				searchForPlaces();
				break;
			}

			if (!NetworkManager.getInstance().getWifiState().equals(mWifiStateLastSearch)) {
				/*
				 * Wifi has enabled/disabled since our last search
				 */
				if (NetworkManager.getInstance().getWifiState() == WifiManager.WIFI_STATE_DISABLED) {

					bindReason = "Wifi switched off";
					NetworkManager.WIFI_AP_STATE wifiApState = NetworkManager.getInstance().getWifiApState();
					if (wifiApState != null && (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_ENABLED)) {
						Logger.d(getActivity(), "Wifi Ap enabled, clearing beacons");
						UI.showToastNotification("Hotspot or tethering enabled", Toast.LENGTH_SHORT);
					}
					else {
						UI.showToastNotification("Wifi disabled", Toast.LENGTH_SHORT);
					}
					ProximityManager.getInstance().getWifiList().clear();
					EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
					LocationManager.getInstance().setLocationLocked(null);
				}
				else if (NetworkManager.getInstance().getWifiState() == WifiManager.WIFI_STATE_ENABLED) {

					bindReason = "Wifi switched on";
					UI.showToastNotification("Wifi enabled", Toast.LENGTH_SHORT);
					searchForPlaces();
				}
				break;
			}

			if ((ProximityManager.getInstance().getLastBeaconLockedDate() != null && mEntityModelBeaconDate != null)
					&& (ProximityManager.getInstance().getLastBeaconLockedDate().longValue() > mEntityModelBeaconDate.longValue())) {
				/*
				 * The beacons we are locked to have changed while we were away so we need to
				 * search for new places linked to beacons.
				 */
				bindReason = "New locked beacons";
				mEntityModelBeaconDate = ProximityManager.getInstance().getLastBeaconLockedDate();
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncGetEntitiesForBeacons");
						final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.SUCCESS) {
							onError();
							Errors.handleError(getActivity(), serviceResponse);
						}
					}
				}.execute();
				break;
			}
			else {

				CacheStamp cacheStamp = Aircandi.getInstance().getEntityManager().getCacheStamp();
				if (mCacheStamp != null && !mCacheStamp.equals(cacheStamp)) {
					/*
					 * EntityManager stamp gets updated when places are inserted/updated/deleted
					 */
					bindReason = "Data changed";
					new AsyncTask() {

						@Override
						protected Object doInBackground(Object... params) {
							Thread.currentThread().setName("AsyncGetEntitiesForBeacons");
							final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
							return serviceResponse;
						}

						@Override
						protected void onPostExecute(Object result) {
							final ServiceResponse serviceResponse = (ServiceResponse) result;
							if (serviceResponse.responseCode != ResponseCode.SUCCESS) {
								onError();
								Errors.handleError(getActivity(), serviceResponse);
							}
						}
					}.execute();
				}
				else {
					mAdapter.notifyDataSetChanged();
				}
				break;
			}
		}

		if (bindReason != null) {
			Logger.d(getActivity(), "Radar bind: " + bindReason);

			if (Aircandi.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Radar bind: " + bindReason, Toast.LENGTH_SHORT);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View view) {
		final Place entity = (Place) ((ViewHolder) view.getTag()).data;
		Bundle extras = null;
		Aircandi.dispatch.route(getActivity(), Route.BROWSE, entity, null, extras);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		updateDevIndicator(event.wifiList, null);
		Reporting.updateCrashKeys();
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Wifi scan received event fired");

				Aircandi.tracker.sendTiming(TrackerCategory.PERFORMANCE, Aircandi.stopwatch1.getTotalTimeMills()
						, "wifi_scan_finished"
						, NetworkManager.getInstance().getNetworkType());

				Logger.d(getActivity(), "Query wifi scan received event: locking beacons");

				if (event.wifiList != null) {
					ProximityManager.getInstance().lockBeacons();
				}
				else {
					BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mEntityModelBeaconDate = ProximityManager.getInstance().getLastBeaconLockedDate();
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncGetEntitiesForBeacons");
						ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.SUCCESS) {
							onError();
							Errors.handleError(getActivity(), serviceResponse);
						}
					}

				}.execute();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesByProximityFinished(EntitiesByProximityFinishedEvent event) {

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {

				Logger.d(getActivity(), "Entities for beacons finished event: ** done **");
				Aircandi.stopwatch1.segmentTime("Entities by proximity finished event fired");
				Aircandi.tracker.sendTiming(TrackerCategory.PERFORMANCE, Aircandi.stopwatch1.getTotalTimeMills()
						, "places_by_proximity_downloaded"
						, NetworkManager.getInstance().getNetworkType());

				Aircandi.stopwatch1.stop("Search for places by beacon complete");
				mWifiStateLastSearch = NetworkManager.getInstance().getWifiState();
				mCacheStamp = Aircandi.getInstance().getEntityManager().getCacheStamp();
			}
		});
	}

	@Subscribe
	@SuppressWarnings({"ucd"})
	public void onPlacesNearLocationFinished(final PlacesNearLocationFinishedEvent event) {
		/*
		 * No application logic here, just tracking.
		 */
		Logger.d(getActivity(), "Places near location finished event: ** done **");
		Aircandi.stopwatch2.stop("Location processing: Places near location complete");
		Aircandi.tracker.sendTiming(TrackerCategory.PERFORMANCE, Aircandi.stopwatch2.getTotalTimeMills()
				, "places_near_location_downloaded"
				, NetworkManager.getInstance().getNetworkType());

	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesChanged(final EntitiesChangedEvent event) {

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Logger.d(getActivity(), "Entities changed event: updating radar");

				/* Point radar adapter at the updated entities */
				final int previousCount = mAdapter.getCount();
				final List<Entity> entities = event.entities;

				Logger.d(getActivity(), "Entities changed: source = " + event.source + ", count = " + String.valueOf(entities.size()));
				mEntities.clear();
				mEntities.addAll(entities);
				mAdapter.notifyDataSetChanged();
				
				/* No more updates are coming */
				if (LocationManager.getInstance().getLocationLocked() != null) {
					mBusy.hideBusy(false);
				}
				//				if (LocationManager.getInstance().getLocationMode() != LocationMode.BURST) {
				//					mBusy.hideBusy(false);
				//				}

				if (event.source.equals("onLocationChanged")) {
					mAtLeastOneLocationProcessed = true;
				}

				/* Add some sparkle */
				if (previousCount == 0 && entities.size() > 0) {
					new AsyncTask() {

						@Override
						protected Object doInBackground(Object... params) {
							Thread.currentThread().setName("AsyncPlaySound");
							MediaManager.playSound(MediaManager.SOUND_PLACES_FOUND, 1.0f, 1);
							return null;
						}

					}.execute();
				}
			}
		});

	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMonitoringWifiScanReceived(MonitoringWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Override
	public void onRefresh() {
		/*
		 * This only gets called by a user clicking the refresh button.
		 */
		Logger.d(getActivity(), "Starting refresh");
		searchForPlaces();
	}

	@Override
	public void onAdd(Bundle extras) {
		/* Schema target is in the extras */
		Aircandi.dispatch.route(getActivity(), Route.NEW, null, null, extras);
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.radar_help);
		Aircandi.dispatch.route(getActivity(), Route.HELP, null, null, extras);
	}

	@Override
	public void onError() {
		/*
		 * Location updates can trigger service calls. Gets restarted
		 * when the user manually triggers a refresh.
		 */
		LocationManager.getInstance().setLocationMode(LocationMode.OFF);

		/* Kill busy */
		mBusy.hideBusy(false);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mAdapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void searchForPlaces() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.Scanning);
				showButtonSpecial(false, null, null);
				Reporting.updateCrashKeys();
			}

			@Override
			protected Object doInBackground(Object... params) {

				Aircandi.stopwatch1.start("beacon_search", "Search for places by beacon");
				mWifiStateLastSearch = NetworkManager.getInstance().getWifiState();
				EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
				if (NetworkManager.getInstance().isWifiEnabled()) {
					ProximityManager.getInstance().scanForWifi(ScanReason.QUERY);
				}

				/* We give the beacon query a bit of a head start */
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						LocationManager.getInstance().setLocationLocked(null);
						LocationManager.getInstance().setLocationMode(LocationMode.BURST);
					}
				}, 500);
				return null;
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * UI
	 *--------------------------------------------------------------------------------------------*/

	private void doBeaconIndicatorClick() {
		if (mBeaconIndicator != null) {
			final StringBuilder beaconMessage = new StringBuilder(500);
			List<WifiScanResult> wifiList = ProximityManager.getInstance().getWifiList();
			synchronized (wifiList) {
				if (Aircandi.getInstance().getCurrentUser() != null
						&& Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
						&& Aircandi.getInstance().getCurrentUser().developer != null
						&& Aircandi.getInstance().getCurrentUser().developer) {
					if (Aircandi.wifiCount > 0) {
						for (WifiScanResult wifi : wifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage.append(wifi.SSID + ": (" + String.valueOf(wifi.level) + ") " + wifi.BSSID + System.getProperty("line.separator"));
							}
						}
						beaconMessage.append(System.getProperty("line.separator"));
						beaconMessage.append("Wifi fix: "
								+ DateTime.interval(ProximityManager.getInstance().mLastWifiUpdate.getTime(), DateTime.nowDate().getTime(),
								IntervalContext.PAST));
					}

					final Location location = LocationManager.getInstance().getLocationLocked();
					if (location != null) {
						final Date fixDate = new Date(location.getTime());
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: "
								+ DateTime.interval(fixDate.getTime(), DateTime.nowDate().getTime(), IntervalContext.PAST));
						beaconMessage.append(System.getProperty("line.separator") + "Location accuracy: " + String.valueOf(location.getAccuracy()));
						beaconMessage.append(System.getProperty("line.separator") + "Location provider: " + location.getProvider());
					}
					else {
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: none");
					}
				}
				else
					return;
			}
			Dialogs.alertDialog(R.drawable.ic_launcher
					, StringManager.getString(R.string.alert_beacons_title)
					, beaconMessage.toString()
					, null
					, getActivity()
					, android.R.string.ok
					, null
					, null
					, new
					DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}, null);
		}
	}

	public void updateDevIndicator(final List<WifiScanResult> scanList, Location location) {

		if (mBeaconIndicator == null) return;

		if (scanList != null) {

			synchronized (scanList) {
				/*
				 * In case we get called from a background thread.
				 */
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {

						WifiScanResult wifiStrongest = null;
						int wifiCount = 0;
						for (WifiScanResult wifi : scanList) {
							wifiCount++;
							if (wifiStrongest == null) {
								wifiStrongest = wifi;
							}
							else if (wifi.level > wifiStrongest.level) {
								wifiStrongest = wifi;
							}
						}

						Aircandi.wifiCount = wifiCount;
						mDebugWifi = String.valueOf(wifiCount);

					}
				});

			}
		}

		if (location != null) {
			Location locationLocked = LocationManager.getInstance().getLocationLocked();
			if (locationLocked != null) {
				if (location.getProvider().equals(locationLocked.getProvider()) && (int) location.getAccuracy() == (int) locationLocked.getAccuracy()) {
					mBeaconIndicator.setTextColor(Colors.getColor(R.color.brand_primary));
				}
				else {
					if (Aircandi.themeTone.equals(ThemeTone.DARK)) {
						mBeaconIndicator.setTextColor(Colors.getColor(R.color.text_dark));
					}
					else {
						mBeaconIndicator.setTextColor(Colors.getColor(R.color.text_light));
					}
				}
			}

			String debugLocation = location.getProvider().substring(0, 1).toUpperCase(Locale.US);
			if (location.hasAccuracy()) {
				debugLocation += String.valueOf((int) location.getAccuracy());
			}
			else {
				debugLocation += "--";
			}
			mDebugLocation = debugLocation;
		}

		mBeaconIndicator.setText(mDebugWifi + ":" + mDebugLocation);
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		/*
		 * Setup menu items local to radar.
		 */
		mMenuItemBeacons = menu.findItem(R.id.beacons);
		if (mMenuItemBeacons != null) {

			/* Only show beacon indicator if user is a developer */
			if (!Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
					|| !Type.isTrue(Aircandi.getInstance().getCurrentUser().developer)) {
				mMenuItemBeacons.setVisible(false);
			}
			else {
				mBeaconIndicator = (TextView) mMenuItemBeacons.getActionView().findViewById(R.id.beacon_indicator);
				mMenuItemBeacons.getActionView().findViewById(R.id.beacon_frame).setTag(mMenuItemBeacons);
				mMenuItemBeacons.getActionView().findViewById(R.id.beacon_frame).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						MenuItem item = (MenuItem) view.getTag();
						onOptionsItemSelected(item);
					}
				});
			}
		}

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.beacons) {
			doBeaconIndicatorClick();
			return true;
		}

		Aircandi.dispatch.route(getActivity(), Aircandi.dispatch.routeForMenuId(item.getItemId()), null, null, null);
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		/*
		 * Called everytime the fragment is started or restarted.
		 */
		super.onStart();
		/*
		 * Check for location service everytime we start.
		 */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			/* We won't continue if location services are disabled */
			Aircandi.dispatch.route(getActivity(), Route.SETTINGS_LOCATION, null, null, null);
			getActivity().finish();
		}

		BusProvider.getInstance().register(mLocationHandler);
		LocationManager.getInstance().setLocationMode(LocationMode.OFF);

		/* Start foreground activity recognition - stop proximity manager from background recognition */
		try {
			ProximityManager.getInstance().unregister();
		}
		catch (Exception ignore) {}
	}

	@Override
	public void onStop() {
		/*
		 * Fired when fragment is being deactivated.
		 * 
		 * Stop any location burst that might be active unless this activity is being restarted. We do this because
		 * there is a race condition that can stop location burst after it has been started by the reload.
		 */
		LocationManager.getInstance().setLocationMode(LocationMode.OFF);
		BusProvider.getInstance().unregister(mLocationHandler);
		/*
		 * Start background activity recognition with proximity manager as the listener.
		 */
		ProximityManager.getInstance().setLastBeaconInstallUpdate(null);
		ProximityManager.getInstance().register();

		/* Kill busy */
		mBusy.hideBusy(false);

		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	/* Stub for future use because I hate bind() */
	@SuppressWarnings("ucd")
	public class RadarMonitor extends SimpleMonitor {
	}

	public class LocationHandler {

		@Subscribe
		@SuppressWarnings({"ucd"})
		public void onLocationChanged(final LocationChangedEvent event) {
			/*
			 * LocationManager has a very low bar for passing along the first location fix.
			 */
			final Location locationCandidate = event.location;

			if (locationCandidate != null) {

				String reason = "first";

				final Location locationLocked = LocationManager.getInstance().getLocationLocked();

				if (locationLocked != null) {
					/*
					 * Filter out locations that are not a solid improvement in accuracy.
					 */
					final float accuracyImprovement = locationLocked.getAccuracy() / locationCandidate.getAccuracy();
					boolean isSignificantlyMoreAccurate = (accuracyImprovement >= 1.5);
					if (!isSignificantlyMoreAccurate) {
						if (LocationManager.getInstance().getLocationMode() != LocationMode.BURST) {
							onActivityComplete();
						}
						return;
					}
					reason = "accuracy";
				}

				String message = "Radar location lock:";
				message += " lat: " + locationCandidate.getLatitude();
				message += " lng: " + locationCandidate.getLongitude();
				message += " acc: " + locationCandidate.getAccuracy();

				if (Aircandi.getInstance().getPrefEnableDev()) {
					UI.showToastNotification(message, Toast.LENGTH_SHORT);
				}

				Logger.d(getActivity(), "Location changed event: location accepted: " + reason);
				LocationManager.getInstance().setLocationLocked(locationCandidate);
				updateDevIndicator(null, locationCandidate);

				final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
				if (location != null && !location.zombie) {

					new AsyncTask() {

						@Override
						protected void onPreExecute() {
							Reporting.updateCrashKeys();
							mBusy.showBusy(BusyAction.Update);
						}

						@Override
						protected Object doInBackground(Object... params) {

							Logger.d(getActivity(), "Location changed event: getting places near location");
							Thread.currentThread().setName("AsyncGetPlacesNearLocation");
							Aircandi.stopwatch2.start("location_processing", "Location processing: get places near location");

							final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesNearLocation(location);

							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
								Aircandi.stopwatch2.segmentTime("Location processing: service processing time: " + ((ServiceData) serviceResponse.data).time);
								final List<Entity> entitiesForEvent = (List<Entity>) Aircandi.getInstance().getEntityManager().getPlaces(null, null);
								BusProvider.getInstance().post(new PlacesNearLocationFinishedEvent());
								BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "onLocationChanged"));
							}

							return serviceResponse;
						}

						@Override
						protected void onPostExecute(Object result) {
							final ServiceResponse serviceResponse = (ServiceResponse) result;

							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
								if (LocationManager.getInstance().getLocationMode() != LocationMode.BURST) {
									onActivityComplete();
								}
							}
							else {
								onError();
								Errors.handleError(getActivity(), serviceResponse);
							}
						}

					}.execute();
				}
				else {
					if (LocationManager.getInstance().getLocationMode() != LocationMode.BURST) {
						onActivityComplete();
					}
				}
			}
		}

		@Subscribe
		@SuppressWarnings({"ucd"})
		public void onBurstTimeout(final BurstTimeoutEvent event) {

			mBusy.hideBusy(false);
			onActivityComplete();

			/* We only show toast if we timeout without getting any location fix */
			if (LocationManager.getInstance().getLocationLocked() == null) {
				UI.showToastNotification(StringManager.getString(R.string.error_location_poor), Toast.LENGTH_SHORT);
			}
		}
	}
}