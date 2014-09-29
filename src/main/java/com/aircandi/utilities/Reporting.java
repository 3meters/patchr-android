package com.aircandi.utilities;

import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiManager;

import com.aircandi.Patch;
import com.aircandi.components.LocationManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.objects.User;
import com.crashlytics.android.Crashlytics;

import java.util.Locale;

public class Reporting {

	public static void updateCrashKeys() {

		Crashlytics.setBool("airplane_mode", NetworkManager.isAirplaneMode(Patch.applicationContext));
		Crashlytics.setBool("connected", NetworkManager.getInstance().isConnected());
		Crashlytics.setString("network_type", NetworkManager.getInstance().getNetworkType().toLowerCase(Locale.US));
		Crashlytics.setBool("wifi_tethered", NetworkManager.getInstance().isWifiTethered());
		Crashlytics.setFloat("beacons_visible", ProximityManager.getInstance().getWifiList().size());

		Location location = LocationManager.getInstance().getLocationLocked();
		if (location != null) {
			Crashlytics.setFloat("location_accurary", location.getAccuracy());
			Crashlytics.setString("location_provider", location.getProvider());
		}

		/* Wifi state */

		Integer wifiState = NetworkManager.getInstance().getWifiState();
		if (wifiState != null) {
			if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
				Crashlytics.setString("wifi_state", "disabled");
			}
			else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
				Crashlytics.setString("wifi_state", "enabled");
			}
			else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
				Crashlytics.setString("wifi_state", "enabling");
			}
			else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
				Crashlytics.setString("wifi_state", "disabling");
			}
		}

		/* Wifi access point state */

		NetworkManager.WIFI_AP_STATE wifiApState = NetworkManager.getInstance().getWifiApState();
		if (wifiApState != null) {
			if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_DISABLED) {
				Crashlytics.setString("wifi_ap_state", "disabled");
			}
			else if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_ENABLED) {
				Crashlytics.setString("wifi_ap_state", "enabled");
			}
			else if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_ENABLING) {
				Crashlytics.setString("wifi_ap_state", "enabling");
			}
			else if (wifiApState == NetworkManager.WIFI_AP_STATE.WIFI_AP_STATE_DISABLING) {
				Crashlytics.setString("wifi_ap_state", "disabling");
			}
		}
	}

	public static void updateCrashUser(User user) {
		if (user != null) {
			Crashlytics.setUserIdentifier(user.id);
			Crashlytics.setUserName(user.name);
			Crashlytics.setUserEmail(user.email);
		}
		else {
			Crashlytics.setUserIdentifier(null);
			Crashlytics.setUserName(null);
			Crashlytics.setUserEmail(null);
		}
	}

	@SuppressWarnings("ucd")
	public static void startCrashReporting(Context context) {
		Crashlytics.start(context);
	}

	public static void logException(Exception exception) {
		Crashlytics.logException(exception);
	}
}