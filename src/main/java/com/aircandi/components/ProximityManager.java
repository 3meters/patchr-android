package com.aircandi.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
import com.aircandi.objects.Document;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.objects.ServiceData;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.widgets.ListPreferenceMultiSelect;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
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
import java.util.Locale;

public class ProximityManager {

	public  Date mLastWifiUpdate;
	private Long mLastBeaconLockedDate;
	private Long mLastBeaconLoadDate;
	private Long mLastBeaconInstallUpdate;

	private WifiManager mWifiManager;
	private EntityCache mEntityCache;

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

	private ProximityManager() {
		if (!Patchr.usingEmulator) {
			mWifiManager = (WifiManager) Patchr.applicationContext.getSystemService(Context.WIFI_SERVICE);
		}
		mEntityCache = EntityManager.getEntityCache();
		register();
	}

	private static class ProxiManagerHolder {
		public static final ProximityManager instance = new ProximityManager();
	}

	public static ProximityManager getInstance() {
		return ProxiManagerHolder.instance;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@SuppressWarnings("ucd")
	public void onActivityStateEvent(final ActivityStateEvent event) {
		/*
		 * Radar is in the foreground so we can be more aggressive. If the user is moving, we should
		 * perform updates more frequently and increase the check interval. When they are still,
		 * we should not perform updates and our activity check interval should be longer.
		 * 
		 * Activity manager is checking for activity every thirty seconds and filters
		 * out tilting and unknowns.
		 */
		if (event.activityState == ActivityState.ARRIVING) {
			Logger.d(this, "Beacon push: activity state = arriving");
			ProximityManager.getInstance().scanForWifi(ScanReason.MONITORING);
			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Beacon push: activity state = arriving", Toast.LENGTH_SHORT);
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
				Thread.currentThread().setName("AsyncUpdateInstallBeacons");
				ServiceResponse serviceResponse = updateInstallBeacons(event.wifiList);
				return serviceResponse;
			}

		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void scanForWifi(final ScanReason reason) {
		/*
		 * If context is null then we probably crashed and the scan service is still calling.
		 */
		if (Patchr.applicationContext == null) return;

		synchronized (mWifiList) {

			if (!Patchr.usingEmulator) {

				Patchr.applicationContext.registerReceiver(new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {

						Patchr.applicationContext.unregisterReceiver(this);
						Patchr.stopwatch1.segmentTime("Wifi scan received from system: reason = " + reason.toString());
						Logger.v(ProximityManager.this, "Received wifi scan results for " + reason.name());

						/* get the latest scan results */
						mWifiList.clear();

						for (ScanResult scanResult : mWifiManager.getScanResults()) {
							/*
							 * Dev/test could trigger a mock access point and we filter for it
							 * just to prevent confusion. We add our own below if emulator is active.
							 */
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

						final String testingBeacon = Patchr.settings.getString(StringManager.getString(R.string.pref_demo_beacons), "{\"name\":\"natural\"}");
						Document demoBeacon = (Document) Json.jsonToObject(testingBeacon, Json.ObjectType.DOCUMENT);

						if (!demoBeacon.name.toLowerCase(Locale.US).equals("natural")) {
							mWifiList.add(new WifiScanResult((String) demoBeacon.data.get("bssid"), (String) demoBeacon.data.get("ssid"), -30, true));
						}

						Collections.sort(mWifiList, new WifiScanResult.SortWifiBySignalLevel());

						mLastWifiUpdate = DateTime.nowDate();
						if (reason == ScanReason.MONITORING) {
							BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
						}
						else if (reason == ScanReason.QUERY) {
							BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
						}

					}
				}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

				Reporting.updateCrashKeys();
				mWifiManager.startScan();
			}
			else {
				mWifiList.clear();
				Logger.d(ProximityManager.this, "Emulator enabled so using dummy scan results");
				mWifiList.add(mWifiMassenaUpper);
				if (reason == ScanReason.MONITORING) {
					BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
				}
				else if (reason == ScanReason.QUERY) {
					BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
				}
			}
		}
	}

	public void lockBeacons() {
		/*
		 * Makes sure that the beacon collection is an accurate representation
		 * of the latest wifi scan.
		 */
		mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
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
				mEntityCache.upsertEntity(beacon);
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
		List<Beacon> beacons = (List<Beacon>) mEntityCache.getCacheEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null);

		for (Beacon beacon : beacons) {
			beaconIds.add(beacon.id);
		}

		Integer removeCount = mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_PLACE, null, true);
		Logger.v(this, "Removed proximity places from cache: count = " + String.valueOf(removeCount));

		/*
		 * Early exit if there aren't any beacons around
		 */
		if (beaconIds.size() == 0) {
			mLastBeaconLoadDate = DateTime.nowDate().getTime();

			/* All cached place entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) Patchr.getInstance().getEntityManager().getPlaces(null, null);
			Patchr.stopwatch1.segmentTime("Entities for beacons: no beacons to process - exiting");

			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesByProximity"));
			BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
			return serviceResponse;

		}

		/* Add current registrationId */
		String installId = Patchr.getinstallId();

		/* Cursor */
		Cursor cursor = new Cursor()
				.setLimit(Patchr.applicationContext.getResources().getInteger(R.integer.limit_places_radar))
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(0);

		serviceResponse = mEntityCache.loadEntitiesByProximity(beaconIds
				, Patchr.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_BEACONS)
				, cursor
				, installId
				, Patchr.stopwatch1);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconLoadDate = ((ServiceData) serviceResponse.data).date.longValue();

			/* All cached place entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) Patchr.getInstance().getEntityManager().getPlaces(null, null);
			Patchr.stopwatch1.segmentTime("Entities for beacons: objects processed");
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesByProximity"));
			BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
		}
		else {
			Patchr.stopwatch1.segmentTime("Entities for beacons: service call failed");
			BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
		}

		return serviceResponse;
	}

	public synchronized ServiceResponse getEntitiesNearLocation(AirLocation location) {

		/* Clean out all synthetics */
		Integer removeCount = mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_PLACE, null, false);
		Logger.v(this, "Removed synthetic places from cache: count = " + String.valueOf(removeCount));

		/*
		 * We find all aircandi place entities in the cache via proximity that are active based
		 * on the current search parameters (beacons and search radius) and could be supplied by the place provider. We
		 * create an array of the provider place id's and pass them so they can be excluded from the places
		 * that get returned.
		 */
		final List<String> excludePlaceIds = new ArrayList<String>();
		for (Entity entity : Patchr.getInstance().getEntityManager().getPlaces(false, true)) {
			Place place = (Place) entity;
			excludePlaceIds.add(place.id);
		}

		ServiceResponse serviceResponse = mEntityCache.loadEntitiesNearLocation(location
				, Patchr.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_PLACE)
				, excludePlaceIds);

		return serviceResponse;
	}

	public synchronized ServiceResponse updateInstallBeacons(final List<WifiScanResult> scanList) {
		/*
		 * This methods calls getEntitiesByProximity but only includes the info necessary
		 * to update the beacons currently associated with this device and install.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();
		List<String> beaconIds = new ArrayList<String>();

		/* Construct string array of the beacon ids */
		synchronized (scanList) {
			if (scanList.size() == 0) return serviceResponse;
			Logger.d(this, "Updating beacons for the current install");

			Iterator it = scanList.iterator();
			while (it.hasNext()) {
				WifiScanResult result = (WifiScanResult) it.next();
				beaconIds.add("be." + result.BSSID);
			}
		}

		/* Add current registrationId */
		String installId = Patchr.getinstallId();
		Cursor cursor = new Cursor().setLimit(0);

		serviceResponse = mEntityCache.loadEntitiesByProximity(beaconIds, null, cursor, installId, null);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconInstallUpdate = ((ServiceData) serviceResponse.data).date.longValue();
			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Beacons pushed (" + beaconIds.size() + "): stopped after walking", Toast.LENGTH_SHORT);
			}
		}

		return serviceResponse;
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

	public List<Beacon> getStrongestBeacons(int max) {

		final List<Beacon> beaconStrongest = new ArrayList<Beacon>();
		int beaconCount = 0;
		List<Beacon> beacons = (List<Beacon>) mEntityCache.getCacheEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null);
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