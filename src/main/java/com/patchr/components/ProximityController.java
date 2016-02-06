package com.patchr.components;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.ActivityRecognitionManager.ActivityState;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.events.ActivityStateEvent;
import com.patchr.events.BeaconsLockedEvent;
import com.patchr.events.EntitiesByProximityCompleteEvent;
import com.patchr.events.EntitiesUpdatedEvent;
import com.patchr.events.QueryWifiScanReceivedEvent;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Beacon;
import com.patchr.objects.Cursor;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.ServiceData;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.widgets.ListPreferenceMultiSelect;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Maps;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ProximityController {

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
		public static final ProximityController instance = new ProximityController();
	}

	public static ProximityController getInstance() {
		return ProxiManagerHolder.instance;
	}

	private ProximityController() {

		mEntityStore = DataController.getEntityCache();
		mWifiReceiver = new BroadcastReceiver() {
			/*
			 * Called from main thread.
			 */
			@Override
			public void onReceive(Context context, Intent intent) {

				Patchr.applicationContext.unregisterReceiver(this);
				Patchr.stopwatch1.segmentTime("Wifi scan received from system: reason = " + mScanReason.toString());
				Logger.v(ProximityController.this, "Received wifi scan results for " + mScanReason.name());

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
						/*
						 * Monitoring wifi scans are triggered when we detect that the device is still after walking.
						 */
						new Thread(new Runnable() {
							@Override
							public void run() {
								updateProximity(mWifiList);
							}
						}).start();
					}
					else if (mScanReason == ScanReason.QUERY) {
						Dispatcher.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
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
			if (PermissionUtil.hasSelfPermission(Patchr.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
				ProximityController.getInstance().scanForWifi(ScanReason.MONITORING);
			}
			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Proximity update: activity state = arriving", Toast.LENGTH_SHORT);
			}
		}
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
		DataController.getInstance().clearEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
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
		Dispatcher.getInstance().post(new BeaconsLockedEvent());
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
		List<Beacon> beacons = (List<Beacon>) DataController.getInstance().getBeacons();

		for (Beacon beacon : beacons) {
			beaconIds.add(beacon.id);
		}

		/* Early exit if there aren't any beacons around */
		if (beaconIds.size() == 0) {

			/* Clean out all patches found via proximity */
			Integer removeCount = DataController.getInstance().clearEntities(Constants.SCHEMA_ENTITY_PATCH, Constants.TYPE_ANY, true /* found by proximity */);
			Logger.v(this, "Removed proximity patches from cache: count = " + String.valueOf(removeCount));

			mLastBeaconLoadDate = DateTime.nowDate().getTime();

			/* All cached patch entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) DataController.getInstance().getPatches(null /* proximity not required */);
			Patchr.stopwatch1.segmentTime("Entities for beacons: no beacons to process - exiting");

			Dispatcher.getInstance().post(new EntitiesUpdatedEvent(entitiesForEvent, "getEntitiesByProximity"));
			Dispatcher.getInstance().post(new EntitiesByProximityCompleteEvent());
			return serviceResponse;
		}

		/* Add current registrationId */
		String installId = Patchr.getInstance().getinstallId();

		/* Cursor */
		Cursor cursor = new Cursor()
				.setLimit(Patchr.applicationContext.getResources().getInteger(R.integer.limit_patches_radar))
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(0);

		/* Only place in the code that calls loadEntitiesByProximity */
		serviceResponse = mEntityStore.loadEntitiesByProximity(beaconIds
				, LinkSpecFactory.build(LinkSpecType.LINKS_FOR_BEACONS)
				, cursor
				, installId
				, NetworkManager.SERVICE_GROUP_TAG_DEFAULT, Patchr.stopwatch1);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconLoadDate = ((ServiceData) serviceResponse.data).date.longValue();

			/* All cached patch entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) DataController.getInstance().getPatches(null /* proximity not required */);
			Patchr.stopwatch1.segmentTime("Entities for beacons: objects processed");
			Dispatcher.getInstance().post(new EntitiesUpdatedEvent(entitiesForEvent, "getEntitiesByProximity"));
		}
		else {
			Patchr.stopwatch1.segmentTime("Entities for beacons: service call failed");
		}

		Dispatcher.getInstance().post(new EntitiesByProximityCompleteEvent());

		return serviceResponse;
	}

	public synchronized ServiceResponse getEntitiesNearLocation(AirLocation location) {

		String installId = Patchr.getInstance().getinstallId();

		/* Only place in the code that calls loadEntitiesNearLocation */
		ServiceResponse serviceResponse = mEntityStore.loadEntitiesNearLocation(location
				, LinkSpecFactory.build(LinkSpecType.LINKS_FOR_PATCH)
				, installId, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

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

		result = DataController.getInstance().updateProximity(beaconIds, location, installId, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconInstallUpdate = DateTime.nowDate().getTime();
			if (Patchr.getInstance().getPrefEnableDev()) {
				UI.showToastNotification("Location pushed: stopped after walking", Toast.LENGTH_SHORT);
			}
		}

		return result.serviceResponse;
	}

	public void register() {
		try {
			Dispatcher.getInstance().register(this);
		}
		catch (IllegalArgumentException ignore) { /* ignore */ }
	}

	public void unregister() {
		Dispatcher.getInstance().unregister(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public List<Beacon> getStrongestBeacons(int max) {

		final List<Beacon> beaconStrongest = new ArrayList<Beacon>();
		int beaconCount = 0;
		List<Beacon> beacons = (List<Beacon>) DataController.getInstance().getBeacons();
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