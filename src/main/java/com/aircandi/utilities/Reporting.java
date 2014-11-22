package com.aircandi.utilities;

import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.aircandi.Patchr;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.objects.User;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;

import java.util.Locale;

public class Reporting {

	public static void updateCrashKeys() {

		Crashlytics.setBool("airplane_mode", NetworkManager.isAirplaneMode(Patchr.applicationContext));
		Crashlytics.setBool("connected", NetworkManager.getInstance().isConnected());
		Crashlytics.setString("network_type", NetworkManager.getInstance().getNetworkType().toLowerCase(Locale.US));
		Crashlytics.setBool("wifi_tethered", NetworkManager.getInstance().isWifiTethered());
		Crashlytics.setFloat("beacons_visible", ProximityManager.getInstance().getWifiList().size());
		Crashlytics.setString("device_name", AndroidManager.getInstance().getDeviceName());

		/* Memory info */
		Crashlytics.setFloat("memory_max_mb", Utilities.maxMemoryMB());
		Crashlytics.setFloat("memory_total_mb", Utilities.totalMemoryMB());
		Crashlytics.setFloat("memory_free_mb", Utilities.freeMemoryMB());

		/* Identifies device/install combo */
		Crashlytics.setApplicationInstallationIdentifier(Patchr.getinstallId());

		Location location = LocationManager.getInstance().getLocationLocked();
		if (location != null) {
			Crashlytics.setFloat("location_accurary", location.getAccuracy());
			Crashlytics.setString("location_provider", location.getProvider());
		}
		else {
			Crashlytics.setFloat("location_accurary", 0);
			Crashlytics.setString("location_provider", "no locked location");
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
		/*
		 * Gets sent to crashlytics as a non-fatal exception with all the
		 * standard logging info. Batched and sent only when the appliction
		 * restarts. Splash has init logic to restart crashlytics so that
		 * might force the send even though the app hasn't restarted.
		 */
		Crashlytics.logException(exception);
	}

	public static void logMessage(String message) {
		/*
		 * Will be included with the next crash report send to crashlytics.
		 */
		Crashlytics.log(message);
	}

	public static void logStacktrace(Exception e) {
		/*
		 * Will be included with the next crash report send to crashlytics.
		 */
		String stacktrace = Log.getStackTraceString(e);
		Crashlytics.log(stacktrace);
	}

	public static void sendEvent(String category, String action, String target, long value) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			Patchr.tracker.send(new HitBuilders.EventBuilder()
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

	public static void sendTiming(String category, Long timing, String name, String label) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			Patchr.tracker.send(new HitBuilders.TimingBuilder()
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

	public static void sendException(Exception exception) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			Patchr.tracker.send(new HitBuilders.ExceptionBuilder()
					.setDescription(new StandardExceptionParser(Patchr.applicationContext, null)
							.getDescription(Thread.currentThread().getName(), exception))
					.setFatal(false)
					.build());
		}
		catch (Exception e) {
			Reporting.logException(e);
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