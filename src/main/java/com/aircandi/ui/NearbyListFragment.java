package com.aircandi.ui;

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
import android.widget.RelativeLayout;
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
import com.aircandi.components.ProximityManager.RefreshReason;
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
import com.aircandi.objects.User;
import com.aircandi.objects.ViewHolder;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.ToolTipView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.Locale;
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
				if (LocationManager.getInstance().isLocationAccessEnabled()) {
					bindReason = "No location processed";
					mBusy.showBusy(BusyAction.Scanning);
					LocationManager.getInstance().requestLocation(getActivity());
					break;
				}
			}

			if (LocationManager.getInstance().getLocationLocked() == null) {
				/*
				 * Gets set everytime we accept a location change in onLocationBroadcast. Means
				 * we didn't get an acceptable fix yet from the fused provider.
				 */
				if (LocationManager.getInstance().isLocationAccessEnabled()) {
					bindReason = "No locked location";
					mBusy.showBusy(BusyAction.Scanning);
					LocationManager.getInstance().requestLocation(getActivity());
					break;
				}
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

				CacheStamp cacheStamp = Patchr.getInstance().getEntityManager().getCacheStamp();
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
					BusProvider.getInstance().post(new ProcessingFinishedEvent());
				}
				break;
			}
		}

		if (bindReason != null) {
			Logger.d(getActivity(), "Radar bind: " + bindReason);

			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Radar bind: " + bindReason, Toast.LENGTH_SHORT);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View view) {
		final Patch entity = (Patch) ((ViewHolder) view.getTag()).data;
		Patchr.dispatch.route(getActivity(), Route.BROWSE, entity, null, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

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
						if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
							LocationManager.getInstance().requestLocation(getActivity());
						}
						else {
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

		BusProvider.getInstance().post(new ProcessingFinishedEvent());
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

				if (entities.size() >= 2) {
					BusProvider.getInstance().post(new ProcessingFinishedEvent());
				}
				
				/* No more updates are coming */
				if (LocationManager.getInstance().getLocationLocked() != null) {
					showTooltips(false);
				}

				/* Show map button if we have some entities to map */
				//handleFooter((entities.size() > 0), AnimationManager.DURATION_MEDIUM);

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
		searchForPlaces();
	}

	@Override
	public void onAdd(Bundle extras) {
		/* Schema target is in the extras */
		Patchr.dispatch.route(getActivity(), Route.NEW, null, null, extras);
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
		if (mEntities.size() > 0 || LocationManager.getInstance().getLocationLocked() != null) {
			MenuItem menuItem = ((BaseActivity) getActivity()).getMenu().findItem(R.id.search);
			final View searchView = menuItem.getActionView();
			searchView.post(new Runnable() {
				@Override
				public void run() {
					showTooltips(true);
				}
			});
		}
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
					Patchr.dispatch.route(getActivity(), Route.NEW_PLACE, null, null, null);
				}
			});
			UI.setVisibility(alertGroup, View.VISIBLE);
		}
	}

	private void searchForPlaces() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.Scanning);
				mBubbleButton.fadeOut();
				Reporting.updateCrashKeys();
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
					if (LocationManager.getInstance().isLocationAccessEnabled()) {
						LocationManager.getInstance().requestLocation(getActivity());
					}
				}

				return null;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (!LocationManager.getInstance().isLocationAccessEnabled()) {
					if (!mLocationDialogShot.get()) {
						Dialogs.locationServicesDisabled(getActivity(), mLocationDialogShot);
					}
					else {
						UI.showToastNotification(StringManager.getString(R.string.alert_location_services_disabled), Toast.LENGTH_SHORT);
					}
					if (!NetworkManager.getInstance().isWifiEnabled()) {
						BusProvider.getInstance().post(new ProcessingFinishedEvent());
					}
				}
			}
		}.execute();
	}

	public void showTooltips(boolean force) {

		ToolTipRelativeLayout tooltipLayer = ((AircandiForm) getActivity()).mTooltips;

		if ((force || tooltipLayer.getVisibility() != View.VISIBLE) && !tooltipLayer.hasShot()) {
			tooltipLayer.setClickable(true);
			tooltipLayer.setVisibility(View.VISIBLE);
			tooltipLayer.clear();
			tooltipLayer.requestLayout();

			ToolTipView tooltipView = tooltipLayer.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_list_nearby))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));

			tooltipView.addRule(RelativeLayout.CENTER_IN_PARENT);

			View searchView = getActivity().getWindow().getDecorView().findViewById(R.id.new_place);
			if (searchView != null) {
				tooltipLayer.showTooltipForView(new ToolTip()
						.withText(StringManager.getString(R.string.tooltip_action_item_patch_new))
						.withShadow(true)
						.withArrow(true)
						.setMaxWidth(UI.getRawPixelsForDisplayPixels(120f))
						.withAnimationType(ToolTip.AnimationType.FROM_TOP), searchView);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		/*
		 * SUPER HACK: runnable will only be posted once the view for search has
		 * gone through measure/layout. That's when it's safe to process tooltips
		 * for action bar items.
		 */
		//		MenuItem menuItem = menu.findItem(R.id.search);
		//		final View searchView = menuItem.getActionView();
		//		searchView.post(new Runnable() {
		//			@Override
		//			public void run() {
		//				showTooltips();
		//			}
		//		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Patchr.dispatch.route(getActivity(), Patchr.dispatch.routeForMenuId(item.getItemId()), null, null, null);
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

		BusProvider.getInstance().register(mLocationHandler);

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
		 */
		BusProvider.getInstance().unregister(mLocationHandler);
		LocationManager.getInstance().stop();
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
	public class RadarMonitor extends SimpleMonitor {}

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
						BusProvider.getInstance().post(new ProcessingFinishedEvent());
						return;
					}
					reason = "accuracy";
				}

				String message = "Radar location lock:";
				message += " lat: " + locationCandidate.getLatitude();
				message += " lng: " + locationCandidate.getLongitude();
				message += " acc: " + locationCandidate.getAccuracy();

				if (Patchr.getInstance().getPrefEnableDev()) {
					UI.showToastNotification(message, Toast.LENGTH_SHORT);
				}

				Logger.d(getActivity(), "Location changed event: location accepted: " + reason);
				LocationManager.getInstance().setLocationLocked(locationCandidate);

				if (locationCandidate.getAccuracy() <= LocationManager.ACCURACY_PREFERRED) {
					LocationManager.getInstance().stop();
				}

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
							Patchr.stopwatch2.start("location_processing", "Location processing: get places near location");

							final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesNearLocation(location);

							return serviceResponse;
						}

						@Override
						protected void onPostExecute(Object result) {
							final ServiceResponse serviceResponse = (ServiceResponse) result;

							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
								Patchr.stopwatch2.segmentTime("Location processing: service processing time: " + ((ServiceData) serviceResponse.data).time);
								final List<Entity> entitiesForEvent = (List<Entity>) Patchr.getInstance().getEntityManager().getPlaces(null, null);
								BusProvider.getInstance().post(new PatchesNearLocationFinishedEvent());
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