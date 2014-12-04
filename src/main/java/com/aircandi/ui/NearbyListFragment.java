package com.aircandi.ui;

import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.BurstTimeoutEvent;
import com.aircandi.events.EntitiesByProximityFinishedEvent;
import com.aircandi.events.EntitiesChangedEvent;
import com.aircandi.events.LocationChangedEvent;
import com.aircandi.events.PatchesNearLocationFinishedEvent;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Route;
import com.aircandi.objects.ServiceData;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.objects.ViewHolder;
import com.aircandi.service.ServiceResponse;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NearbyListFragment extends EntityListFragment {

	private final Handler mHandler = new Handler();

	private MenuItem mMenuItemBeacons;
	private TextView mBeaconIndicator;
	private String   mDebugWifi;
	private String mDebugLocation = "--";

	private Number mEntityModelBeaconDate;
	private   Integer         mWifiStateLastSearch         = WifiManager.WIFI_STATE_UNKNOWN;
	private   LocationHandler mLocationHandler             = new LocationHandler();
	private   Boolean         mAtLeastOneLocationProcessed = false;
	protected AtomicBoolean   mLocationDialogShot          = new AtomicBoolean(false);

	private CacheStamp mCacheStamp;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view == null) return null;
		draw(view);
		return view;
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Only called in response to parent form receiving a push notification. Example
		 * is a new patch was created nearby and we want to show it.
		 */
		CacheStamp cacheStamp = Patchr.getInstance().getEntityManager().getCacheStamp();
		if (mCacheStamp != null && !mCacheStamp.equals(cacheStamp)) {
			searchForPatches();
		}
		else {
			mAdapter.notifyDataSetChanged();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View view) {
		final Patch entity = (Patch) ((ViewHolder) view.getTag()).data;
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
		Patchr.dispatch.route(getActivity(), Route.BROWSE, entity, extras);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (getActivity() == null || getActivity().isFinishing()) return;

		Reporting.updateCrashKeys();
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Patchr.stopwatch1.segmentTime("Wifi scan received event fired");

				Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch1.getTotalTimeMills()
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

		if (getActivity() == null || getActivity().isFinishing()) return;
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

		if (getActivity() == null || getActivity().isFinishing()) return;
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {

				Logger.d(getActivity(), "Entities for beacons finished event: ** done **");
				Patchr.stopwatch1.segmentTime("Entities by proximity finished event fired");
				Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch1.getTotalTimeMills()
						, "places_by_proximity_downloaded"
						, NetworkManager.getInstance().getNetworkType());

				Patchr.stopwatch1.stop("Search for places by beacon complete");
				mWifiStateLastSearch = NetworkManager.getInstance().getWifiState();
				mCacheStamp = Patchr.getInstance().getEntityManager().getCacheStamp();

				if (!LocationManager.getInstance().isLocationAccessEnabled()) {
					BusProvider.getInstance().post(new ProcessingFinishedEvent());
				}
				else {

					final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null) {

						new AsyncTask() {

							@Override
							protected void onPreExecute() {
								Reporting.updateCrashKeys();
							}

							@Override
							protected Object doInBackground(Object... params) {
								Logger.d(getActivity(), "Location changed event: getting places near location");
								Thread.currentThread().setName("AsyncGetPlacesNearLocation");
								Patchr.stopwatch2.start("location_processing", "Location processing: get places near location");
								ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesNearLocation(location);
								return serviceResponse;
							}

							@Override
							protected void onPostExecute(Object result) {
								final ServiceResponse serviceResponse = (ServiceResponse) result;

								if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
									Patchr.stopwatch2.segmentTime("Location processing: service processing time: " + ((ServiceData) serviceResponse.data).time);
									final List<Entity> entitiesForEvent = (List<Entity>) Patchr.getInstance().getEntityManager().getPlaces(null /* proximity not required */);
									BusProvider.getInstance().post(new PatchesNearLocationFinishedEvent()); // Just tracking
									BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "onLocationChanged"));
									BusProvider.getInstance().post(new ProcessingFinishedEvent());
								}
								else {
									onError();
									Errors.handleError(getActivity(), serviceResponse);
								}
							}
						}.execute();
					}
					else {
						BusProvider.getInstance().post(new ProcessingFinishedEvent());
					}
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings({"ucd"})
	public void onPlacesNearLocationFinished(final PatchesNearLocationFinishedEvent event) {
		/*
		 * No application logic here, just tracking.
		 */
		Logger.d(getActivity(), "Patches near location finished event: ** done **");
		Patchr.stopwatch2.stop("Location processing: Patches near location complete");
		Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
				, "places_near_location_downloaded"
				, NetworkManager.getInstance().getNetworkType());
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesChanged(final EntitiesChangedEvent event) {

		if (getActivity() == null || getActivity().isFinishing()) return;
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

				if (entities.size() >= 2) {
					BusProvider.getInstance().post(new ProcessingFinishedEvent());
				}

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

	@Override
	public void onRefresh() {
		/*
		 * Called by refresh action or swipe.
		 */
		Logger.d(getActivity(), "Starting refresh");
		if (LocationManager.getInstance().isLocationAccessEnabled()) {
			mBusy.showBusy(BusyAction.Scanning);
			LocationManager.getInstance().requestLocationUpdates(getActivity());  // Location triggers sequence
		}
		else {
			if (!mLocationDialogShot.get()) {
				Dialogs.locationServicesDisabled(getActivity(), mLocationDialogShot);
			}
			else {
				UI.showToastNotification(StringManager.getString(R.string.alert_location_services_disabled), Toast.LENGTH_SHORT);
			}
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mBusy.showBusy(BusyAction.Scanning);
				ProximityManager.getInstance().scanForWifi(ScanReason.QUERY);         // Still try proximity
			}
			else {
				BusProvider.getInstance().post(new ProcessingFinishedEvent());
			}
		}
	}

	@Override
	public void onAdd(Bundle extras) {
		/* Schema target is in the extras */
		Patchr.dispatch.route(getActivity(), Route.NEW, null, extras);
	}

	@Override
	public void onError() {
		/* Kill busy */
		BusProvider.getInstance().post(new ProcessingFinishedEvent());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mAdapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void draw(View view) {
		mAdapter.notifyDataSetChanged();
		drawButtons(view);
	}

	public void drawButtons(View view) {

		User currentUser = Patchr.getInstance().getCurrentUser();

		Boolean anonymous = currentUser.isAnonymous();
		Count patched = Patchr.getInstance().getCurrentUser().getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);

		ViewGroup alertGroup = (ViewGroup) view.findViewById(R.id.alert_group);
		UI.setVisibility(alertGroup, View.GONE);
		if (alertGroup != null) {

			TextView buttonAlert = (TextView) view.findViewById(R.id.button_alert);
			if (buttonAlert == null) return;

			View rule = view.findViewById(R.id.rule_alert);
			if (rule != null && Constants.SUPPORTS_KIT_KAT) {
				rule.setVisibility(View.GONE);
			}

			if (anonymous) {
				buttonAlert.setText(R.string.button_alert_radar_anonymous);
			}
			if (patched != null) {
				buttonAlert.setText(R.string.button_alert_radar);
			}
			else {
				buttonAlert.setText(R.string.button_alert_radar_no_patch);
			}

			buttonAlert.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Patchr.dispatch.route(getActivity(), Route.NEW_PLACE, null, null);
				}
			});
			UI.setVisibility(alertGroup, View.VISIBLE);
		}
	}

	private void searchForPatches() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.Scanning);
				mEmptyController.fadeOut();
				Reporting.updateCrashKeys();
				if (Patchr.getInstance().getPrefEnableDev()) {
					MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {

				Patchr.stopwatch1.start("beacon_search", "Search for places by beacon");
				mWifiStateLastSearch = NetworkManager.getInstance().getWifiState();
				EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
				if (NetworkManager.getInstance().isWifiEnabled()) {
					ProximityManager.getInstance().scanForWifi(ScanReason.QUERY);
				}
				else {
					BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
				}
				return null;
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	protected void start() {
		BusProvider.getInstance().register(mLocationHandler);
		onRefresh();

		/* Start foreground activity recognition - stop proximity manager from background recognition */
		try {
			ProximityManager.getInstance().unregister();
		}
		catch (Exception ignore) {}
	}

	protected void pause() {
		LocationManager.getInstance().stop();
	}

	protected void stop() {
		try {
			BusProvider.getInstance().unregister(mLocationHandler);
		}
		catch (Exception ignore){}

		 /* Start background activity recognition with proximity manager as the listener. */
		ProximityManager.getInstance().setLastBeaconInstallUpdate(null);
		ProximityManager.getInstance().register();

		/* Kill busy */
		mBusy.hideBusy(false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	/* Stub for future use because I hate bind() */
	@SuppressWarnings("ucd")
	public class RadarMonitor extends SimpleMonitor {}

	public class LocationHandler {

		@Subscribe
		@SuppressWarnings({"ucd"})
		public void onLocationChanged(final LocationChangedEvent event) {
			/*
			 * Location changes are a primary trigger for a patch query sequence.
			 */
			if (event.location != null) {

				LocationManager.getInstance().setLocationLocked(event.location);
				final AirLocation location = LocationManager.getInstance().getAirLocationLocked();

				if (location != null) {
					searchForPatches();
				}
				else {
					BusProvider.getInstance().post(new ProcessingFinishedEvent());
				}
			}
		}

		@Subscribe
		@SuppressWarnings({"ucd"})
		public void onBurstTimeout(final BurstTimeoutEvent event) {

			BusProvider.getInstance().post(new ProcessingFinishedEvent());

			/* We only show toast if we timeout without getting any location fix */
			if (LocationManager.getInstance().getLocationLocked() == null) {
				UI.showToastNotification(StringManager.getString(R.string.error_location_poor), Toast.LENGTH_SHORT);
			}
		}
	}
}