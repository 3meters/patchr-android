package com.patchr.components;

import android.location.Location;
import android.net.wifi.WifiManager;

import com.bugsnag.android.Bugsnag;
import com.patchr.Patchr;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.WifiApState;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Utils;

import java.util.Locale;

public class ReportingManager {

	private static ReportingManager  instance;
	private static AnalyticsProvider analyticsProvider;

	private ReportingManager() {}

	public static ReportingManager init(AnalyticsProvider provider) {
		if (instance == null) {
			analyticsProvider = provider;
			instance = new ReportingManager();
			Bugsnag.init(Patchr.applicationContext);
		}
		return instance;
	}

	public static ReportingManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException("ReportingManager is not initialised - first invocation must use parameterised init");
		}
		return instance;
	}

	public static void updateCrashKeys() {

		Bugsnag.addToTab("network", "airplane_mode", NetworkManager.getInstance().isAirplaneMode(Patchr.applicationContext));
		Bugsnag.addToTab("network", "connected", NetworkManager.getInstance().isConnected());
		Bugsnag.addToTab("network", "network_type", NetworkManager.getInstance().getNetworkType().toLowerCase(Locale.US));
		Bugsnag.addToTab("network", "wifi_tethered", NetworkManager.getInstance().isWifiTethered());

		/* Identifies device/install combo */
		Bugsnag.addToTab("device", "patchr_install_id", NotificationManager.installId);

		/* Location info */
		Location location = LocationManager.getInstance().getAndroidLocationLocked();
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

		WifiApState wifiApState = NetworkManager.getInstance().getWifiApState();
		if (wifiApState != null) {
			if (wifiApState == WifiApState.WIFI_AP_STATE_DISABLED) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "disabled");
			}
			else if (wifiApState == WifiApState.WIFI_AP_STATE_ENABLED) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "enabled");
			}
			else if (wifiApState == WifiApState.WIFI_AP_STATE_ENABLING) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "enabling");
			}
			else if (wifiApState == WifiApState.WIFI_AP_STATE_DISABLING) {
				Bugsnag.addToTab("wifi", "wifi_ap_state", "disabling");
			}
		}

		Bugsnag.addToTab("memory", "memory_max_mb", Utils.maxMemoryMB());
		Bugsnag.addToTab("memory", "memory_total_mb", Utils.totalMemoryMB());
		Bugsnag.addToTab("memory", "memory_free_mb", Utils.freeMemoryMB());
	}

	public static void logException(Exception exception) {
		Bugsnag.notify(exception);
	}

	public static void breadcrumb(String message) {
		Bugsnag.leaveBreadcrumb(message);
	}

	public void updateUser(RealmEntity user) {

		if (user != null) {
			String userId = user.id;
			String userName = user.name != null ? user.name : "Provisional";
			String userAuth = user.email != null ? user.email : user.getPhone() != null ? user.getPhone().displayNumber() : "null";
			BranchProvider.setIdentity(userId);
			Bugsnag.setUser(user.id, userName, userAuth);
			analyticsProvider.updateUser(userId, userName, userAuth);
		}
		else {
			BranchProvider.logout();
			Bugsnag.setUser(NotificationManager.installId, null, "Anonymous");
			analyticsProvider.updateUser(null, null, null);
		}
	}

	public void userLoggedIn() {
		analyticsProvider.track(AnalyticsCategory.ACTION, "User logged in");
	}

	public void userLoggedOut() {
		analyticsProvider.track(AnalyticsCategory.ACTION, "User logged out");
	}

	public void track(String category, String event) {
		analyticsProvider.track(category, event);
	}

	public void screen(String category, String name) {
		analyticsProvider.screen(category, name);
	}
}