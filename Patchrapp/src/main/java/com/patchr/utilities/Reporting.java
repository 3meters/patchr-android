package com.patchr.utilities;

import android.location.Location;
import android.net.wifi.WifiManager;

import com.bugsnag.android.Bugsnag;
import com.patchr.Patchr;
import com.patchr.components.BranchProvider;
import com.patchr.components.LocationManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.ProximityController;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.User;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

import java.util.Locale;

public class Reporting {

	public static void updateCrashKeys() {

		Bugsnag.addToTab("network", "airplane_mode", NetworkManager.isAirplaneMode(Patchr.applicationContext));
		Bugsnag.addToTab("network", "connected", NetworkManager.getInstance().isConnected());
		Bugsnag.addToTab("network", "network_type", NetworkManager.getInstance().getNetworkType().toLowerCase(Locale.US));
		Bugsnag.addToTab("network", "wifi_tethered", NetworkManager.getInstance().isWifiTethered());

		/* Identifies device/install combo */
		Bugsnag.addToTab("device", "patchr_install_id", Patchr.getInstance().getinstallId());

		/* Location info */
		Location location = LocationManager.getInstance().getLocationLocked();
		if (location != null) {
			Bugsnag.addToTab("location", "accuracy", location.getAccuracy());
			Bugsnag.addToTab("location", "age_in_secs", DateTime.secondsAgo(location.getTime()));
			Bugsnag.addToTab("location", "provider", location.getProvider());
		}
		else {
			Bugsnag.addToTab("location", "accuracy", "--");
			Bugsnag.addToTab("location", "age_in_secs", "--");
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

	public static void updateUser(User user) {
		if (user != null) {
			String userName = user.name != null ? user.name : "Provisional";
			String userAuth = user.email != null ? user.email : user.phone != null ? user.phone.displayNumber() : "null";
			BranchProvider.setIdentity(user.id);
			Bugsnag.setUser(user.id, userName, userAuth);
			Analytics.with(Patchr.applicationContext).alias(user.id);
			Analytics.with(Patchr.applicationContext).identify(user.id, new Traits().putName(userName).putEmail(userAuth), null);
		}
		else {
			BranchProvider.logout();
			Bugsnag.setUser(Patchr.getInstance().getinstallId(), null, "Anonymous");
			Analytics.with(Patchr.applicationContext).flush();  // Send queued events before clearing user id
			Analytics.with(Patchr.applicationContext).reset();  // Clear user id currently used by segmentio
		}
	}

	public static void logException(Exception exception) {
		Bugsnag.notify(exception);
	}

	public static void breadcrumb(String message) {
		Bugsnag.leaveBreadcrumb(message);
	}

	public static void track(String category, String event) {
		Analytics.with(Patchr.applicationContext).track(event, new Properties()
				.putValue("category", category));
	}

	public static void track(String category, String event, Properties properties) {
		properties.putValue("category", category);
		Analytics.with(Patchr.applicationContext).track(event, properties);
	}

	public static void track(String category, String event, String target, long value) {
		Analytics.with(Patchr.applicationContext).track(event, new Properties()
				.putValue("category", category)
				.putValue("target", target)
				.putValue("value", value));
	}

	public static void track(String category, String event, boolean nonInteratation) {
		Analytics.with(Patchr.applicationContext).track(event, new Properties()
				.putValue("category", category)
				.putValue("nonInteraction", nonInteratation));
	}

	public static void screen(String name) {
		Reporting.screen(AnalyticsCategory.VIEW, name);
	}

	public static void screen(String category, String name) {
		Analytics.with(Patchr.applicationContext).screen(category, name);
	}

	public static void sendTiming(String category, Long timing, String name, String label) {
		/*
		 * Stub right now. User timings require native calls using GoogleAnalytics
		 * segment bundle.
		 */
	}
}