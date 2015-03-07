package com.aircandi.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.ActivityRecognitionManager.ActivityState;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.ActivityStateEvent;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.EntitiesByProximityFinishedEvent;
import com.aircandi.events.EntitiesChangedEvent;
import com.aircandi.events.MonitoringWifiScanReceivedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Patch;
import com.aircandi.objects.ServiceData;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.widgets.ListPreferenceMultiSelect;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Maps;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ProximityManager {

	public  Date mLastWifiUpdate;
	private Long mLastBeaconLockedDate;
	private Long mLastBeaconLoadDate;
	private Long mLastBeaconInstallUpdate;

	private EntityStore       mEntityStore;
	private BroadcastReceiver mWifiReceiver;
	private ScanReason        mScanReason;

	/*
	 * Continuously updated as we perform wifi scans. Beacons are only build from the wifi info on demand.
	 */
	private List<WifiScanResult> mWifiList = Collections.synchronizedList(new ArrayList<WifiScanResult>());

	private static final WifiScanResult mWifiMassenaUpper       = new WifiScanResult("00:1c:b3:ae:bf:f0", "test_massena_upper", -50, true);
	private static final WifiScanResult mWifiMassenaLower       = new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower", -50, true);
	private static final WifiScanResult mWifiMassenaLowerStrong = new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_strong", -20, true);
	private static final WifiScanResult mWifiMassenaLowerWeak   = new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_weak", -100, true);
	private static final WifiScanResult mWifiEmpty              = new WifiScanResult("aa:aa:bb:bb:cc:cc", "test_empty", -50, true);
	private static final String         MockBssid               = "00:00:00:00:00:00";

	private static class ProxiManagerHolder {
		public static final ProximityManager instance = new ProximityManager();
	}

	public static ProximityManager getInstance() {
		return ProxiManagerHolder.instance;
	}

	private ProximityManager() {

		mEntityStore = EntityManager.getEntityCache();
		mWifiReceiver = new BroadcastReceiver() {
			/*
			 * Called from main thread.
			 */
			@Override
			public void onReceive(Context context, Intent intent) {

				Patchr.applicationContext.unregisterReceiver(this);
				Patchr.stopwatch1.segmentTime("Wifi scan received from system: reason = " + mScanReason.toString());
				Logger.v(ProximityManager.this, "Received wifi scan results for " + mScanReason.name());

				synchronized (mWifiList) {

					/* Rebuild wifi list with latest scan results */

					mWifiList.clear();
					for (ScanResult scanResult : NetworkManager.getInstance().getWifiManager().getScanResults()) {
						/* Dev/test could trigger a mock access point and we filter for it just to prevent confusion.*/
						if (!scanResult.BSSID.equals(MockBssid)) {
							mWifiList.add(new WifiScanResult(scanResult));
						}
					}

					final String testingBeacons = Patchr.settings.getString(StringManager.getString(R.string.pref_testing_beacons), "natural");

					if (!ListPreferenceMultiSelect.contains("natural", testingBeacons, null)) {
						mWifiList.clear();
					}

					if (ListPreferenceMultiSelect.contains("massena_upper", testingBeacons, null)) {
						mWifiList.add(mWifiMassenaUpper);
					}

					if (ListPreferenceMultiSelect.contains("massena_lower", testingBeacons, null)) {
						mWifiList.add(mWifiMassenaLower);
					}

					if (ListPreferenceMultiSelect.contains("massena_lower_strong", testingBeacons, null)) {
						mWifiList.add(mWifiMassenaLowerStrong);
					}

					if (ListPreferenceMultiSelect.contains("massena_lower_weak", testingBeacons, null)) {
						mWifiList.add(mWifiMassenaLowerWeak);
					}

					if (ListPreferenceMultiSelect.contains("empty", testingBeacons, null)) {
						mWifiList.add(mWifiEmpty);
					}

					Collections.sort(mWifiList, new WifiScanResult.SortWifiBySignalLevel());

					mLastWifiUpdate = DateTime.nowDate();
					if (mScanReason == ScanReason.MONITORING) {
						BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
					}
					else if (mScanReason == ScanReason.QUERY) {
						BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
					}
				}
			}
		};

		register();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@SuppressWarnings("ucd")
	public void onActivityStateEvent(final ActivityStateEvent event) {
		/*
		 * Activity manager is checking for activity every thirty seconds and filters
		 * out tilting and unknowns.
		 */
		if (event.activityState == ActivityState.ARRIVING) {
			Logger.d(this, "Proximity update: activity state = arriving");
			ProximityManager.getInstance().scanForWifi(ScanReason.MONITORING);
			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Proximity update: activity state = arriving", Toast.LENGTH_SHORT);
			}
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMonitoringWifiScanReceived(final MonitoringWifiScanReceivedEvent event) {
		/*
		 * Monitoring wifi scans are triggered when we detect that the device is still after walking.
		 */
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncUpdateProximity");
				ServiceResponse serviceResponse = updateProximity(event.wifiList);
				return serviceResponse;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void scanForWifi(final ScanReason reason) {
		/*
		 * If context is null then we probably crashed and the scan service is still calling.
		 */
		//noinspection ConstantConditions
		if (Patchr.applicationContext == null) return;

		mScanReason = reason;
		Patchr.applicationContext.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Reporting.updateCrashKeys();
		Logger.d(this, "Starting wifi scan");
		NetworkManager.getInstance().getWifiManager().startScan();
	}

	public void stop() {
		Logger.d(this, "Unregistering wifi scan receiver");
		try {
			Patchr.applicationContext.unregisterReceiver(mWifiReceiver);
		}
		catch (IllegalArgumentException e) { /* Ignored */}
	}

	public void lockBeacons() {
		/*
		 * Makes sure that the beacon collection is an accurate representation
		 * of the latest wifi scan.
		 */
		mEntityStore.removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
		/*
		 * insert beacons for the latest scan results.
		 */
		synchronized (mWifiList) {
			WifiScanResult scanResult;
			for (int i = 0; i < mWifiList.size(); i++) {
				scanResult = mWifiList.get(i);
				Beacon beacon = new Beacon(scanResult.BSSID
						, scanResult.SSID
						, scanResult.SSID
						, scanResult.level
						, scanResult.test);

				beacon.synthetic = true;
				beacon.schema = Constants.SCHEMA_ENTITY_BEACON;
				mEntityStore.upsertEntity(beacon);
			}
		}

		mLastBeaconLockedDate = DateTime.nowDate().getTime();
		BusProvider.getInstance().post(new BeaconsLockedEvent());
	}

	/*--------------------------------------------------------------------------------------------
	 * Load beacon related entities
	 *--------------------------------------------------------------------------------------------*/

	public synchronized ServiceResponse getEntitiesByProximity() {
		/*
		 * All current beacons ids are sent to the service. Previously discovered beacons are included in separate
		 * array along with a their freshness date.
		 * 
		 * To force a full rebuild of all entities for all beacons, clear the beacon collection.
		 * 
		 * The service returns all entities for new beacons and entities that have had activity since the freshness
		 * date for old beacons. Unchanged entities from previous scans will still be updated for local changes in
		 * visibility.
		 */
		Logger.d(this, "Processing beacons from scan");
		Patchr.stopwatch1.segmentTime("Entities for beacons (synchronized): processing started");

		/*
		 * Call the proxi service to see if the new beacons have been tagged with any entities. If call comes back
		 * null then there was a network or service problem. The user got a toast notification from the service. We
		 * are making synchronous calls inside an asynchronous thread.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Construct string array of the beacon ids */
		List<String> beaconIds = new ArrayList<String>();
		List<Beacon> beacons = (List<Beacon>) mEntityStore.getCacheEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null /* proximity required */);

		for (Beacon beacon : beacons) {
			beaconIds.add(beacon.id);
		}

		/* Early exit if there aren't any beacons around */
		if (beaconIds.size() == 0) {

			/* Clean out all patches found via proximity */
			Integer removeCount = mEntityStore.removeEntities(Constants.SCHEMA_ENTITY_PATCH, Constants.TYPE_ANY, true /* found by proximity */);
			Logger.v(this, "Removed proximity places from cache: count = " + String.valueOf(removeCount));

			mLastBeaconLoadDate = DateTime.nowDate().getTime();

			/* All cached patch entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) Patchr.getInstance().getEntityManager().getPatches(null /* proximity not required */);
			Patchr.stopwatch1.segmentTime("Entities for beacons: no beacons to process - exiting");

			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesByProximity"));
			BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
			return serviceResponse;
		}

		/* Add current registrationId */
		String installId = Patchr.getInstance().getinstallId();

		/* Cursor */
		Cursor cursor = new Cursor()
				.setLimit(Patchr.applicationContext.getResources().getInteger(R.integer.limit_places_radar))
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(0);

		serviceResponse = mEntityStore.loadEntitiesByProximity(beaconIds
				, Patchr.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_BEACONS)
				, cursor
				, installId
				, NetworkManager.SERVICE_GROUP_TAG_DEFAULT, Patchr.stopwatch1);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconLoadDate = ((ServiceData) serviceResponse.data).date.longValue();

			/* All cached patch entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) Patchr.getInstance().getEntityManager().getPatches(null /* proximity not required */);
			Patchr.stopwatch1.segmentTime("Entities for beacons: objects processed");
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesByProximity"));
		}
		else {
			Patchr.stopwatch1.segmentTime("Entities for beacons: service call failed");
		}

		BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());

		return serviceResponse;
	}

	public synchronized ServiceResponse getEntitiesNearLocation(AirLocation location) {
		/*
		 * We find all aircandi patch entities in the cache via proximity that are active based
		 * on the current search parameters (beacons and search radius) and could be supplied by the patch provider. We
		 * create an array of the provider patch id's and pass them so they can be excluded from the places
		 * that get returned.
		 */
		final List<String> excludePlaceIds = new ArrayList<String>();
		for (Entity entity : Patchr.getInstance().getEntityManager().getPatches(true /* proximity required */)) {
			Patch place = (Patch) entity;
			excludePlaceIds.add(place.id);
		}

		String installId = Patchr.getInstance().getinstallId();

		ServiceResponse serviceResponse = mEntityStore.loadEntitiesNearLocation(location
				, Patchr.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_PATCH)
				, installId
				, excludePlaceIds, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

		return serviceResponse;
	}

	public synchronized ServiceResponse updateProximity(final List<WifiScanResult> scanList) {

		ModelResult result = new ModelResult();
		List<String> beaconIds = new ArrayList<String>();

		/* Construct string array of the beacon ids */
		synchronized (scanList) {
			if (scanList.size() == 0) return result.serviceResponse;
			Logger.d(this, "Updating beacons for the current install");

			Iterator it = scanList.iterator();
			while (it.hasNext()) {
				WifiScanResult wifiResult = (WifiScanResult) it.next();
				beaconIds.add("be." + wifiResult.BSSID);
			}
		}

		AirLocation location = LocationManager.getInstance().getAirLocationLocked();
		String installId = Patchr.getInstance().getinstallId();

		result = Patchr.getInstance().getEntityManager().updateProximity(beaconIds, location, installId, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconInstallUpdate = DateTime.nowDate().getTime();
			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Location pushed: stopped after walking", Toast.LENGTH_SHORT);
			}
		}

		return result.serviceResponse;
	}

	public void register() {
		BusProvider.getInstance().register(this);
	}

	public void unregister() {
		BusProvider.getInstance().unregister(this);
	}

	public RefreshReason beaconRefreshNeeded(Location activeLocation) {

		if (mLastBeaconInstallUpdate != null) {
			return RefreshReason.MOVE_RECOGNIZED;
		}

		if (mLastBeaconLoadDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mLastBeaconLoadDate;
			if (interval > Constants.INTERVAL_REFRESH) {
				Logger.v(this, "Refresh needed: past interval");
				return RefreshReason.BEACONS_STALE;
			}
		}

		/* Do a coarse location check */
		Location locationLocked = LocationManager.getInstance().getLocationLocked();
		Location locationLast = LocationManager.getInstance().getLocationLast();
		if (locationLast != null && locationLocked != null) {
			Float distance = locationLocked.distanceTo(locationLast);
			if (distance >= Constants.DISTANCE_REFRESH) {
				return RefreshReason.MOVE_MEASURED;
			}
		}
		return RefreshReason.NONE;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public List<Beacon> getStrongestBeacons(int max) {

		final List<Beacon> beaconStrongest = new ArrayList<Beacon>();
		int beaconCount = 0;
		List<Beacon> beacons = (List<Beacon>) mEntityStore.getCacheEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null /* proximity required */);
		Collections.sort(beacons, new Beacon.SortBySignalLevel());

		for (Beacon beacon : beacons) {
			if (beacon.test) {
				continue;
			}
			beaconStrongest.add(beacon);
			beaconCount++;
			if (beaconCount >= max) {
				break;
			}
		}
		return beaconStrongest;
	}

	public Beacon getStrongestBeacon() {
		final List<Beacon> beacons = getStrongestBeacons(1);
		if (beacons.size() > 1) return beacons.get(0);
		return null;
	}

	public Number getLastBeaconLockedDate() {
		return mLastBeaconLockedDate;
	}

	@NonNull
	public List<WifiScanResult> getWifiList() {
		return mWifiList;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public Long getLastBeaconInstallUpdate() {
		return mLastBeaconInstallUpdate;
	}

	public void setLastBeaconInstallUpdate(Long lastBeaconInstallUpdate) {
		mLastBeaconInstallUpdate = lastBeaconInstallUpdate;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class WifiScanResult {

		public String BSSID;
		public String SSID;
		public int     level = 0;
		public Boolean test  = false;

		private WifiScanResult(String bssid, String ssid, int level, Boolean test) {
			this.BSSID = bssid;
			this.SSID = ssid;
			this.level = level;
			this.test = test;
		}

		private WifiScanResult(ScanResult scanResult) {
			this.BSSID = scanResult.BSSID;
			this.SSID = scanResult.SSID;
			this.level = scanResult.level;
		}

		private static class SortWifiBySignalLevel implements Comparator<WifiScanResult> {

			@Override
			public int compare(WifiScanResult object1, WifiScanResult object2) {
				if (object1.level > object2.level)
					return -1;
				else if (object1.level < object2.level)
					return 1;
				else
					return 0;
			}
		}
	}

	public static enum ScanReason {
		QUERY,
		MONITORING
	}

	public static enum RefreshReason {
		NONE,
		MOVE_RECOGNIZED,
		MOVE_MEASURED,
		BEACONS_STALE
	}
}