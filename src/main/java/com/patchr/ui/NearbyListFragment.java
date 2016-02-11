package com.patchr.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import com.patchr.interfaces.IBusy;
import com.patchr.objects.AirLocation;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Patch;
import com.patchr.objects.Route;
import com.patchr.objects.ServiceData;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.objects.ViewHolder;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
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
	private LocationHandler mLocationHandler             = new LocationHandler();
	private Boolean         mAtLeastOneLocationProcessed = false;

	protected AtomicBoolean mLocationDialogShot = new AtomicBoolean(false);
	protected AsyncTask mTaskPatchesNearLocation;
	protected AsyncTask mTaskPatchesByProximity;

	private CacheStamp mCacheStamp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ensurePermissions();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			draw(view);
		}
		return view;
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
					Boolean proximityCapable = (NetworkManager.getInstance().isWifiEnabled()
							|| LocationManager.getInstance().isLocationAccessEnabled());
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
		if (!isResumed()) return;  // So we don't process a query scan while not visible

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
						, "patches_by_proximity_downloaded"
						, NetworkManager.getInstance().getNetworkType());

				Patchr.stopwatch1.stop("Search for patches by beacon complete");
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
											, "patches_near_location_downloaded"
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

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationAllowed(final LocationAllowedEvent event) {
		setListEmptyMessageResId(R.string.label_radar_empty);
		onRefresh();
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationDenied(final LocationDeniedEvent event) {
		/* Here but being used yet. */
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void bind(BindingMode mode) {
		Logger.d(this, "Bind called: " + mode.name().toString());
		/*
		 * Global cache stamp gets dirtied by delete, insert, update patch entity.
		 * Also when gcm notification is received and targetSchema == patch.
		 */
		Boolean stampDirty = !DataController.getInstance().getGlobalCacheStamp().equals(mCacheStamp);

		if (PermissionUtil.hasSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

			/* Start location processing */
			if (LocationManager.getInstance().isLocationAccessEnabled()) {
				/*
				 * If manual mode then first location available is used and not possibly
				 * optimized out. This ensures that the location based patches will be rebuilt
				 * even if you haven't moved an inch.
				 */
				if (mode == BindingMode.MANUAL || stampDirty) {
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
				if (!mLocationDialogShot.get()) {
					Dialogs.locationServicesDisabled(getActivity(), mLocationDialogShot);
				}
				else {
					UI.showToastNotification(StringManager.getString(R.string.alert_location_services_disabled), Toast.LENGTH_SHORT);
				}
			}

			/* Start a wifi scan */
			if (NetworkManager.getInstance().isWifiEnabled()) {
				searchForPatches(); // Chains from wifi to near (if location locked)
				return;
			}
		}
		else {
			showSnackbar();
		}

		/* Got nothing to work with so drop our pants */
		draw(getView());
		onProcessingComplete(ResponseCode.SUCCESS);
	}

	public void draw(final View view) {
		Activity activity = getActivity();
		if (activity != null && !activity.isFinishing()) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();
					drawButtons(view);
				}
			});
		}
	}

	public void drawButtons(View view) {


		ViewGroup alertGroup = (ViewGroup) view.findViewById(R.id.alert_group);
		UI.setVisibility(alertGroup, View.GONE);
		if (alertGroup != null) {

			TextView buttonAlert = (TextView) view.findViewById(R.id.button_alert);
			if (buttonAlert == null) return;

			View rule = view.findViewById(R.id.rule_alert);
			if (rule != null && Constants.SUPPORTS_KIT_KAT) {
				rule.setVisibility(View.GONE);
			}

			if (!UserManager.getInstance().authenticated()) {
				buttonAlert.setText(R.string.button_alert_radar_anonymous);
			}
			else {
				Count patched = UserManager.getInstance().getCurrentUser().getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);
				if (patched != null) {
					buttonAlert.setText(R.string.button_alert_radar);
				}
				else {
					buttonAlert.setText(R.string.button_alert_radar_no_patch);
				}
			}

			buttonAlert.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Patchr.router.route(getActivity(), Route.NEW_PLACE, null, null);
				}
			});
			UI.setVisibility(alertGroup, View.VISIBLE);
		}

		Boolean proximityCapable = (NetworkManager.getInstance().isWifiEnabled()
				|| LocationManager.getInstance().isLocationAccessEnabled());
		if (proximityCapable) {
			mListController.getFloatingActionController().fadeIn();
		}
		else {
			mListController.getFloatingActionController().fadeOut();
		}
	}

	private void searchForPatches() {
		/*
		 * Called because of a location update or push notification.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				Reporting.updateCrashKeys();
				if (mEntities.size() == 0) {
					mListController.getBusyController().show(IBusy.BusyAction.Scanning_Empty);
					mListController.getMessageController().fadeOut();
				}
				if (Patchr.getInstance().getPrefEnableDev()) {
					MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {

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
			//snackbar.getView().setBackgroundColor(Colors.getColor(R.color.brand_accent));
			snackbar.setActionTextColor(Colors.getColor(R.color.brand_primary));
			snackbar.setText(R.string.alert_location_permission_denied)
			        .setAction("Settings", new View.OnClickListener() {
				        @Override
				        public void onClick(View v) {
					        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
					        intent.setData(uri);
					        startActivityForResult(intent, 100);
				        }
			        })
			        .show();
		}
	}

	private void ensurePermissions() {

		if (!PermissionUtil.hasSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

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
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		super.onStart();

		Dispatcher.getInstance().register(mLocationHandler);

		/* Start foreground activity recognition - stop proximity manager from background recognition */
		try {
			ProximityController.getInstance().unregister();
		}
		catch (Exception ignore) { /* ignnore */}
	}

	@Override
	public void onStop() {
		super.onStop();

		try {
			Dispatcher.getInstance().unregister(mLocationHandler);
		}
		catch (Exception ignore) {/* ignore */}
		/*
		 * Parent could be restarting because of something like a theme change so
		 * don't need to stop location updates or start activity monitoring.
		 */
		if (!((BaseActivity) getActivity()).getRestarting()) {

			/* Stop location updates */
			LocationManager.getInstance().stop();

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
	}
}