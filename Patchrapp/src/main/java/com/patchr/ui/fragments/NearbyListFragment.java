package com.patchr.ui.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.PermissionUtil;
import com.patchr.components.ProximityController;
import com.patchr.components.ProximityController.ScanReason;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.BeaconsLockedEvent;
import com.patchr.events.EntitiesByProximityCompleteEvent;
import com.patchr.events.EntitiesUpdatedEvent;
import com.patchr.events.LocationAllowedEvent;
import com.patchr.events.LocationDeniedEvent;
import com.patchr.events.LocationUpdatedEvent;
import com.patchr.events.QueryWifiScanReceivedEvent;
import com.patchr.model.RealmLocation;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.ServiceData;
import com.patchr.service.ServiceResponse;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NearbyListFragment extends EntityListFragment {

	private final Handler         handler;
	private       Number          entityModelBeaconDate;
	private       LocationHandler locationHandler;
	private       boolean         atLeastOneLocationProcessed;
	protected     AtomicBoolean   locationDialogShot;
	protected     AsyncTask       taskPatchesNearLocation;
	protected     AsyncTask       taskPatchesByProximity;
	private       CacheStamp      cacheStamp;

	public NearbyListFragment() {
		handler = new Handler();
		locationHandler = new LocationHandler();
		locationDialogShot = new AtomicBoolean(false);
	}

	@Override public void onStart() {
		bindActionButton(); // User might have logged in/out while gone
		Dispatcher.getInstance().register(locationHandler);
		Dispatcher.getInstance().unregister(ProximityController.getInstance()); /* Start foreground activity recognition - stop proximity manager from background recognition */
		super.onStart();    // Call chain fetch(FetchMode.AUTO) -> bind()
	}

	@Override public void onStop() {
		super.onStop();

		/* Stop location updates */
		Dispatcher.getInstance().unregister(locationHandler);
		LocationManager.getInstance().stop();

		/* Start background activity recognition with proximity manager as the listener. */
		ProximityController.getInstance().setLastBeaconInstallUpdate(null);
		Dispatcher.getInstance().register(ProximityController.getInstance());
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Fetching notification sequence
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (getActivity() == null || getActivity().isFinishing()) return;
		if (!isResumed()) return;  // So we don't process a query scan while not visible

		Reporting.updateCrashKeys();
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Patchr.stopwatch1.segmentTime("Wifi scan received event fired");

				Reporting.sendTiming(AnalyticsCategory.PERFORMANCE, Patchr.stopwatch1.getTotalTimeMills()
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

	@Subscribe public void onBeaconsLocked(BeaconsLockedEvent event) {

		if (getActivity() == null || getActivity().isFinishing()) return;
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				entityModelBeaconDate = ProximityController.getInstance().getLastBeaconLockedDate();
				taskPatchesByProximity = new AsyncTask() {

					@Override protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncGetPatchesForBeacons");
						return ProximityController.getInstance().getEntitiesByProximity();
					}

					@Override protected void onCancelled(Object result) {
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

					@Override protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.SUCCESS) {
							Errors.handleError(getActivity(), serviceResponse);
						}
					}
				}.executeOnExecutor(Constants.EXECUTOR);
			}
		});
	}

	@Subscribe public void onEntitiesByProximityFinished(EntitiesByProximityCompleteEvent event) {

		if (getActivity() == null || getActivity().isFinishing()) return;

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {

				Logger.d(getActivity(), "Entities for beacons finished event: ** done **");
				Patchr.stopwatch1.segmentTime("Entities by proximity finished event fired");
				Reporting.sendTiming(AnalyticsCategory.PERFORMANCE, Patchr.stopwatch1.getTotalTimeMills()
					, "patches_by_proximity_downloaded"
					, NetworkManager.getInstance().getNetworkType());

				Patchr.stopwatch1.stop("Search for patches by beacon complete");
				cacheStamp = DataController.getInstance().getGlobalCacheStamp();

				if (!LocationManager.getInstance().isLocationAccessEnabled()) {
					listController.busyController.hide(true);
				}
				else {

					final RealmLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null) {
						Reporting.updateCrashKeys();
						taskPatchesNearLocation = new AsyncTask() {

							@Override protected Object doInBackground(Object... params) {
								Thread.currentThread().setName("AsyncGetPatchesNearLocation");
								Logger.d(getActivity(), "Proximity finished event: Location locked so getting patches near location");
								Patchr.stopwatch2.start("location_processing", "Location processing: get patches near location");
								return ProximityController.getInstance().getEntitiesNearLocation(location);
							}

							@Override protected void onCancelled(Object result) {
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

							@Override protected void onPostExecute(Object result) {
								final ServiceResponse serviceResponse = (ServiceResponse) result;

								if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
									Patchr.stopwatch2.segmentTime("Location processing: service processing time: " + ((ServiceData) serviceResponse.data).time);
									final List<Entity> entitiesForEvent = (List<Entity>) DataController.getInstance().getPatches(null /* proximity not required */);

									Logger.d(getActivity(), "Patches near location finished event: ** done **");
									Patchr.stopwatch2.stop("Location processing: Patches near location complete");
									Reporting.sendTiming(AnalyticsCategory.PERFORMANCE, Patchr.stopwatch2.getTotalTimeMills()
										, "patches_near_location_downloaded"
										, NetworkManager.getInstance().getNetworkType());

									Dispatcher.getInstance().post(new EntitiesUpdatedEvent(entitiesForEvent, "onLocationChanged"));
									listController.busyController.hide(true);
								}
								else {
									Errors.handleError(getActivity(), serviceResponse);
								}
							}
						}.executeOnExecutor(Constants.EXECUTOR);
					}
					else {
						listController.busyController.hide(true);
					}
				}
			}
		});
	}

	@Subscribe public void onEntitiesChanged(final EntitiesUpdatedEvent event) {

		if (getActivity() == null || getActivity().isFinishing()) return;

		getActivity().runOnUiThread(new Runnable() {

			@Override public void run() {
				Logger.d(getActivity(), "Entities changed event: updating radar");

				if (event.source.equals("onLocationChanged")) {
					atLeastOneLocationProcessed = true;
				}

				/* Add some sparkle */
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncPlaySound");
						MediaManager.playSound(MediaManager.SOUND_PLACES_FOUND, 1.0f, 1);
						return null;
					}
				}.executeOnExecutor(Constants.EXECUTOR);
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onLocationAllowed(final LocationAllowedEvent event) {
		fetch(FetchMode.MANUAL);
		listController.emptyController.setText(StringManager.getString(R.string.empty_nearby));
	}

	@Subscribe public void onLocationDenied(final LocationDeniedEvent event) {
		listController.emptyController.setText("Location services disabled for Patchr");
		listController.emptyController.show(true);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void fetch(FetchMode mode) {
		Logger.d(this, "Fetch called: " + mode.name());

		if (!PermissionUtil.hasSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
			requestPermissions();
			return;
		}
		/*
		 * Global cache stamp gets dirtied by delete, insert, update patch entity.
		 * Also when gcm notification is received and targetSchema == patch.
		 */
		Boolean stampDirty = !DataController.getInstance().getGlobalCacheStamp().equals(cacheStamp);

		/* Start location processing */
		if (LocationManager.getInstance().isLocationAccessEnabled()) {
			/*
			 * If manual mode then first location available is used and not possibly
			 * optimized out. This ensures that the location based patches will be rebuilt
			 * even if you haven't moved an inch.
			 */
			if (mode == FetchMode.MANUAL || stampDirty) {
				LocationManager.getInstance().stop();
				LocationManager.getInstance().start(true);  // Location update triggers searchForPatches
			}
			else {
				LocationManager.getInstance().start(false); // Location update triggers searchForPatches
			}
			return;
		}
		else {
			/* Let them know that location services are disabled */
			if (!locationDialogShot.get()) {
				Dialogs.locationServicesDisabled(getActivity(), locationDialogShot);
			}
			else {
				UI.toast(StringManager.getString(R.string.alert_location_services_disabled));
				showSnackbar();
			}
		}

		/* Start a wifi scan */
		if (NetworkManager.getInstance().isWifiEnabled()) {
			searchForPatches(); // Chains from wifi to near (if location locked)
			return;
		}

		/* Location is dead to us so time for pants off */
		bind();
		listController.busyController.hide(true);
	}

	public void bind() {
		bindActionButton();
	}

	public void bindActionButton() {

		TextView alertButton = (TextView) header.findViewById(R.id.action_button);
		if (alertButton != null) {

			View rule = header.findViewById(R.id.action_rule);
			if (rule != null && Constants.SUPPORTS_KIT_KAT) {
				rule.setVisibility(View.GONE);
			}

			if (!UserManager.shared().authenticated()) {
				alertButton.setText(R.string.button_alert_radar_anonymous);
			}
			else {
				Boolean patched = (UserManager.currentRealmUser.patchesOwned != null && UserManager.currentRealmUser.patchesOwned > 0);
				alertButton.setText(patched ? R.string.button_alert_radar : R.string.button_alert_radar_no_patch);
			}
		}
	}

	private void searchForPatches() {
		/*
		 * Called because of a location update or push notification.
		 */
		new AsyncTask() {

			@Override protected void onPreExecute() {
				Reporting.updateCrashKeys();
//				if (listPresenter.entities.size() == 0) {
//					listPresenter.busyPresenter.show(BusyController.BusyAction.Scanning_Empty);
//					listPresenter.emptyPresenter.hide(true);
//				}
				if (Patchr.getInstance().prefEnableDev) {
					MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
				}
			}

			@Override protected Object doInBackground(Object... params) {

				Thread.currentThread().setName("AsyncSearchForPatches");
				Patchr.stopwatch1.start("beacon_search", "Search for patches by beacon");

				DataController.getInstance().clearEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
				if (NetworkManager.getInstance().isWifiEnabled()
					&& PermissionUtil.hasSelfPermission(Patchr.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
					ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
				}
				else {
					Dispatcher.getInstance().post(new EntitiesByProximityCompleteEvent());
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void showSnackbar() {
		if (getView() != null) {
			Snackbar snackbar = Snackbar.make(getView(), "Snackbar", Snackbar.LENGTH_LONG);
			snackbar.setActionTextColor(Colors.getColor(R.color.brand_primary));
			snackbar.setText(R.string.alert_location_permission_denied)
				.setAction("Settings", new View.OnClickListener() {
					@Override public void onClick(View view) {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
						intent.setData(uri);
						startActivityForResult(intent, 100);
					}
				})
				.show();
		}
	}

	private void requestPermissions() {

		if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final AlertDialog dialog = Dialogs.alertDialog(null
						, StringManager.getString(R.string.alert_permission_location_title)
						, StringManager.getString(R.string.alert_permission_location_message)
						, null
						, getActivity()
						, R.string.alert_permission_location_positive
						, R.string.alert_permission_location_negative
						, null
						, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_POSITIVE) {
									ActivityCompat.requestPermissions(getActivity()
										, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
										, Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
								}
								else {
									listController.busyController.hide(true);
									listController.emptyController.setText("Location services disabled for Patchr");
									listController.emptyController.show(true);
								}
							}
						}, null);
					dialog.setCanceledOnTouchOutside(false);
				}
			});
		}
		else {
			/*
			 * No explanation needed, we can request the permission.
			 * Parent activity will broadcast an event when permission request is complete.
			 */
			ActivityCompat.requestPermissions(getActivity()
				, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
				, Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class LocationHandler {

		@Subscribe public void onLocationChanged(final LocationUpdatedEvent event) {
			/*
			 * Location changes are a primary trigger for a patch query sequence.
			 */
			if (isResumed()) {
				if (event.location != null) {
					LocationManager.getInstance().setLocationLocked(event.location);
					final RealmLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null) {
						searchForPatches();
					}
					else {
						listController.busyController.hide(true);
					}
				}
			}
		}
	}
}