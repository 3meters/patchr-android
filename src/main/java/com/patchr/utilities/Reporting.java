package com.patchr.utilities;

import android.location.Location;
import android.net.wifi.WifiManager;

import com.bugsnag.android.Bugsnag;
import com.google.android.gms.analytics.HitBuilders;
import com.patchr.Patchr;
import com.patchr.components.AndroidManager;
import com.patchr.components.LocationManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.ProximityController;
import com.patchr.objects.User;

import java.util.Locale;

public class Reporting {

	public static void updateCrashKeys() {

		Bugsnag.addToTab("network", "airplane_mode", NetworkManager.isAirplaneMode(Patchr.applicationContext));
		Bugsnag.addToTab("network", "connected", NetworkManager.getInstance().isConnected());
		Bugsnag.addToTab("network", "network_type", NetworkManager.getInstance().getNetworkType().toLowerCase(Locale.US));
		Bugsnag.addToTab("network", "wifi_tethered", NetworkManager.getInstance().isWifiTethered());

		/* Identifies device/install combo */
		Bugsnag.addToTab("device", "patchr_install_id", Patchr.getInstance().getinstallId());
		Bugsnag.addToTab("device", "name", AndroidManager.getInstance().getDeviceName());

		/* Location info */
		Location location = LocationManager.getInstance().getLocationLocked();
		if (location != null) {
			Bugsnag.addToTab("location", "accuracy", location.getAccuracy());
			Bugsnag.addToTab("location", "age_in_secs", NetworkManager.getInstance().isWifiTethered());
			Bugsnag.addToTab("location", "provider", location.getProvider());
		}
		else {
			Bugsnag.addToTab("location", "accuracy", null);
			Bugsnag.addToTab("location", "age_in_secs", null);
			Bugsnag.addToTab("location", "provider", "No locked location");
		}

		/* Proximity */
		Bugsnag.addToTab("proximity", "beacons_visible", ProximityController.getInstance().getWifiList().size());

		/* Wifi state */

		Integer wifiState = NetworkManager.getInstance().getWifiState();
		if (wifiState != null) {
			if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
				Bugsnag.addToTab("wifi", "wifi_state", "disabled");
			}
			else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
				Bugsnag.addToTab("wifi", "wifi_state", "enabled");
			}
			else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
				Bugsnag.addToTab("wifi", "wifi_state", "enabling");
			}
			else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
				Bugsnag.addToTab("wifi", "wifi_state", "disabling");
			}
		}

		/* Wifi access point state */

		NetworkManager.WIFI_AP_STATE wifiApState = NetworkManager.getInstance().getWifiApState();
		if (wifiApState != null) {
			if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_DISABLED) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "disabled");
			}
			else if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_ENABLED) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "enabled");
			}
			else if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_ENABLING) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "enabling");
			}
			else if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_DISABLING) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "disabling");
			}
		}

		Bugsnag.addToTab("memory", "memory_max_mb", Utils.maxMemoryMB());
		Bugsnag.addToTab("memory", "memory_total_mb", Utils.totalMemoryMB());
		Bugsnag.addToTab("memory", "memory_free_mb", Utils.freeMemoryMB());
	}

	public static void updateCrashUser(User user) {
		if (user != null) {
			Bugsnag.setUser(user.id, user.name, user.email);
		}
		else {
			Bugsnag.setUser(null, null, null);
		}
	}

	public static void logException(Exception exception) {
		Bugsnag.notify(exception);
	}

	public static void breadcrumb(String message) {
		Bugsnag.leaveBreadcrumb(message);
	}

	public static void sendEvent(String category, String action, String target, long value) {
		/*
		 * Arguments should be free of whitespace.
		 */
		if (Patchr.getInstance().getTracker() != null) {
			try {
				Patchr.getInstance().getTracker().send(new HitBuilders.EventBuilder()
						.setCategory(category)
						.setAction(action)
						.setLabel(target)
						.setValue(value)
						.build());
			}
			catch (Exception e) {
				Reporting.logException(e);
			}
		}
	}

	public static void sendTiming(String category, Long timing, String name, String label) {
		/*
		 * Arguments should be free of whitespace.
		 */
		if (Patchr.getInstance().getTracker() != null) {
			try {
				Patchr.getInstance().getTracker().send(new HitBuilders.TimingBuilder()
						.setCategory(category)
						.setValue(timing)
						.setVariable(name)
						.setLabel(label)
						.build());
			}
			catch (Exception e) {
				Reporting.logException(e);
			}
		}
	}

	public static class TrackerCategory {
		public static String UX          = "ux";
		public static String SYSTEM      = "system";
		public static String EDIT        = "editing";
		public static String LINK        = "linking";
		public static String USER        = "user";
		public static String PERFORMANCE = "performance";
	}
}