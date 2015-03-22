package com.aircandi.ui;

import android.app.Activity;
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
import com.aircandi.components.DataController;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityController;
import com.aircandi.components.ProximityController.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.EntitiesByProximityCompleteEvent;
import com.aircandi.events.EntitiesUpdatedEvent;
import com.aircandi.events.LocationTimeoutEvent;
import com.aircandi.events.LocationUpdatedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
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
import com.aircandi.ui.base.BaseActivity;
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
	protected AsyncTask mTaskPatchesNearLocation;
	protected AsyncTask mTaskPatchesByProximity;

	private CacheStamp mCacheStamp;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			draw(view);
		}
		return view;
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Only called in response to parent form receiving a push notification. Example
		 * is a new patch was created nearby and we want to show it.
		 */
		CacheStamp cacheStamp = DataController.getInstance().getGlobalCacheStamp();
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
	public void onViewLayout() {
		/* Stub to block default behavior */
	}

	public void onProcessingComplete(final ResponseCode responseCode) {
		super.onProcessingComplete(responseCode);

		Activity activity = getActivity();
		if (activity != null && !activity.isFinishing()) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Boolean proximityCapable = (NetworkManager.getInstance().isWifiEnabled() || LocationManager.getInstance().isLocationAccessEnabled());
					if (proximityCapable) {
						mListController.getFloatingActionController().fadeIn();
					}
					else {
						mListController.getFloatingActionController().fadeOut();
					}
				}
			});
		}
	}

	@Override
	public void onClick(View view) {
		final Patch entity = (Patch) ((ViewHolder) view.getTag()).data;
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
		Patchr.router.route(getActivity(), Route.BROWSE, entity, extras);
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
					ProximityController.getInstance().lockBeacons();
				}
				else {
					Dispatcher.getInstance().post(new EntitiesByProximityCompleteEvent());
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
				mEntityModelBeaconDate = ProximityController.getInstance().getLastBeaconLockedDate();
				mTaskPatchesByProximity = new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mListController.getBusyController().show(mEntities.size() == 0 ? BusyAction.Scanning_Empty : BusyAction.Scanning);
						mListController.getMessageController().fadeOut();
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncGetPatchesForBeacons");
						ServiceResponse serviceResponse = ProximityController.getInstance().getEntitiesByProximity();
						return serviceResponse;
					}

					@Override
					protected void onCancelled(Object result) {
						/*
						 * Called after exiting doInBackground() and task.cancel was called.
						 * If using task.cancel(true) and the task is running then AsyncTask
						 * will call interrupt on the thread which in turn will be picked up
						 * by okhttp before it begins the next blocking operation.
						 */
						if (result != null) {
							final ServiceResponse serviceResponse = (ServiceResponse) result;
							Logger.w(Thread.currentThread().getName(), "Proximity task cancelled: " + serviceResponse.responseCode.toString());
						}
					}

					@Override
					protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.SUCCESS) {
							onError();
							Errors.handleError(getActivity(), serviceResponse);
						}
					}
				}.executeOnExecutor(Constants.EXECUTOR);
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesByProximityFinished(EntitiesByProximityCompleteEvent event) {

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
				mCacheStamp = DataController.getInstance().getGlobalCacheStamp();

				if (!LocationManager.getInstance().isLocationAccessEnabled()) {
					onProcessingComplete(ResponseCode.SUCCESS);
				}
				else {

					final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null) {
						Reporting.updateCrashKeys();
						mTaskPatchesNearLocation = new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {
								Thread.currentThread().setName("AsyncGetPatchesNearLocation");
								Logger.d(getActivity(), "Proximity finished event: Location locked so getting patches near location");
								Patchr.stopwatch2.start("location_processing", "Location processing: get patches near location");
								ServiceResponse serviceResponse = ProximityController.getInstance().getEntitiesNearLocation(location);
								return serviceResponse;
							}

							@Override
							protected void onCancelled(Object result) {
								/*
								 * Called after exiting doInBackground() and task.cancel was called.
								 * If using task.cancel(true) and the task is running then AsyncTask
								 * will call interrupt on the thread which in turn will be picked up
								 * by okhttp before it begins the next blocking operation.
								 */
								if (result != null) {
									final ServiceResponse serviceResponse = (ServiceResponse) result;
									Logger.w(Thread.currentThread().getName(), "Near task cancelled: " + serviceResponse.responseCode.toString());
								}
							}

							@Override
							protected void onPostExecute(Object result) {
								final ServiceResponse serviceResponse = (ServiceResponse) result;

								if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
									Patchr.stopwatch2.segmentTime("Location processing: service processing time: " + ((ServiceData) serviceResponse.data).time);
									final List<Entity> entitiesForEvent = (List<Entity>) DataController.getInstance().getPatches(null /* proximity not required */);

									Logger.d(getActivity(), "Patches near location finished event: ** done **");
									Patchr.stopwatch2.stop("Location processing: Patches near location complete");
									Reporting.sendTiming(Reporting.TrackerCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
											, "places_near_location_downloaded"
											, NetworkManager.getInstance().getNetworkType());

									Dispatcher.getInstance().post(new EntitiesUpdatedEvent(entitiesForEvent, "onLocationChanged"));
									onProcessingComplete(ResponseCode.SUCCESS);
								}
								else {
									onError();
									Errors.handleError(getActivity(), serviceResponse);
								}
							}
						}.executeOnExecutor(Constants.EXECUTOR);
					}
					else {
						onProcessingComplete(ResponseCode.SUCCESS);
					}
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesChanged(final EntitiesUpdatedEvent event) {

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
					onProcessingComplete(ResponseCode.SUCCESS);
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
					}.executeOnExecutor(Constants.EXECUTOR);
				}
			}
		});
	}

	@Override
	public void onRefresh() {
		/*
		 * Called by BaseFragment.onStart(), refresh action or swipe.
		 * If wifi enabled then location processing uses the network which can be stuck because
		 * of poor network conditions.
		 */
		Logger.d(this, "Starting refresh");
		if (LocationManager.getInstance().isLocationAccessEnabled()) {
			mListController.getBusyController().show(mEntities.size() == 0 ? BusyAction.Scanning_Empty : BusyAction.Scanning);
			LocationManager.getInstance().requestLocationUpdates(getActivity());  // Location update triggers searchForPatches
		}
		else {
			if (!mLocationDialogShot.get()) {
				Dialogs.locationServicesDisabled(getActivity(), mLocationDialogShot);
			}
			else {
				UI.showToastNotification(StringManager.getString(R.string.alert_location_services_disabled), Toast.LENGTH_SHORT);
			}
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mListController.getBusyController().show(mEntities.size() == 0 ? BusyAction.Scanning_Empty : BusyAction.Scanning);
				ProximityController.getInstance().scanForWifi(ScanReason.QUERY);         // Still try proximity
			}
			else {
				onProcessingComplete(ResponseCode.SUCCESS);
			}
		}
	}

	@Override
	public void onAdd(Bundle extras) {
		/* Schema target is in the extras */
		Patchr.router.route(getActivity(), Route.NEW, null, extras);
	}

	@Override
	public void onError() {
		/* Kill busy */
		onProcessingComplete(ResponseCode.FAILED);
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
					Patchr.router.route(getActivity(), Route.NEW_PLACE, null, null);
				}
			});
			UI.setVisibility(alertGroup, View.VISIBLE);
		}
	}

	private void searchForPatches() {
		/*
		 * Called because of a location update or push notification.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mListController.getBusyController().show(mEntities.size() == 0 ? BusyAction.Scanning_Empty : BusyAction.Scanning);
				mListController.getMessageController().fadeOut();
				Reporting.updateCrashKeys();
				if (Patchr.getInstance().getPrefEnableDev()) {
					MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {

				Thread.currentThread().setName("AsyncSearchForPatches");
				Patchr.stopwatch1.start("beacon_search", "Search for places by beacon");
				mWifiStateLastSearch = NetworkManager.getInstance().getWifiState();
				DataController.getInstance().clearEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
				if (NetworkManager.getInstance().isWifiEnabled()) {
					ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
				}
				else {
					Dispatcher.getInstance().post(new EntitiesByProximityCompleteEvent());
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	protected void start() {
		super.start();
		Dispatcher.getInstance().register(mLocationHandler);
		onRefresh(); // Starts location updates if location services enabled.

		/* Start foreground activity recognition - stop proximity manager from background recognition */
		try {
			ProximityController.getInstance().unregister();
		}
		catch (Exception ignore) {}
	}

	protected void stop() {
		super.stop();
		try {
			Dispatcher.getInstance().unregister(mLocationHandler);
		}
		catch (Exception ignore) {}
		/*
		 * Parent could be restarting because of something like a theme change so
		 * don't need to stop location updates or start activity monitoring.
		 */
		if (!((BaseActivity) getActivity()).getRestarting()) {

			/* Stop location updates */
			LocationManager.getInstance().stop();

			/* Stop listening for wifi scan */
			ProximityController.getInstance().stop();

		    /* Start background activity recognition with proximity manager as the listener. */
			ProximityController.getInstance().setLastBeaconInstallUpdate(null);
			ProximityController.getInstance().register();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class LocationHandler {

		@Subscribe
		@SuppressWarnings({"ucd"})
		public void onLocationChanged(final LocationUpdatedEvent event) {
			/*
			 * Location changes are a primary trigger for a patch query sequence.
			 */
			if (isResumed()) {
				if (event.location != null) {

					LocationManager.getInstance().setLocationLocked(event.location);
					final AirLocation location = LocationManager.getInstance().getAirLocationLocked();

					if (location != null) {
						searchForPatches();
					}
					else {
						onProcessingComplete(ResponseCode.SUCCESS);
					}
				}
			}
		}

		@Subscribe
		@SuppressWarnings({"ucd"})
		public void onBurstTimeout(final LocationTimeoutEvent event) {

			onProcessingComplete(ResponseCode.SUCCESS);

			/* We only show toast if we timeout without getting any location fix */
			if (LocationManager.getInstance().getLocationLocked() == null) {
				UI.showToastNotification(StringManager.getString(R.string.error_location_poor), Toast.LENGTH_SHORT);
			}
		}
	}
}