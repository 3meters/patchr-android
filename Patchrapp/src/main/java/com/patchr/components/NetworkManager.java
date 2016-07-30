package com.patchr.components;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.objects.enums.WifiApState;
import com.patchr.utilities.Reporting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
 */

@SuppressWarnings("unused")
public class NetworkManager {
    /*
     * Http 1.1 Status Codes (subset)
	 * 
	 * - 200: OK
	 * - 201: Created
	 * - 202: Accepted
	 * - 203: Non-authoritative information
	 * - 204: Request fulfilled but no content returned (message body empty).
	 * - 3xx: Redirection
	 * - 400: Bad request. Malformed syntax.
	 * - 401: Unauthorized. Request requires user authentication.
	 * - 403: Forbidden
	 * - 404: Not found
	 * - 405: Method not allowed
	 * - 408: Request timeout
	 * - 415: Unsupported media type
	 * - 500: Internal server error
	 * - 503: Service unavailable. Caused by temporary overloading or maintenance.
	 * 
	 * Notes:
	 * 
	 * - We get a 403 from amazon when trying to fetch something from S3 that isn't there.
	 */

	/*
	 * Timeouts
	 * 
	 * - Connection timeout is the max time allowed to make initial connection with the remote server.
	 * - Sockettimeout is the max inactivity time allowed between two consecutive data packets.
	 */

	/*
	 * Exceptions when executing HTTP methods using okhttp
	 * 
	 * - IOException: Generic transport exceptions (unreliable connection, socket timeout, generally non-fatal.
	 * ClientProtocolException, SocketException and InterruptedIOException are sub classes of IOException.
	 * 
	 * - HttpException: Protocol exceptions. These tend to be fatal and suggest something fundamental is wrong with the
	 * request such as a violation of the http protocol.
	 */

	/* Opportunistically used for crash reporting but not current state */
	private WifiManager         wifiManager;
	private ConnectivityManager connectivityManager;
	private BroadcastReceiver   networkChangeReceiver;

	private static NetworkManager instance = new NetworkManager();

	public static NetworkManager getInstance() {
		return instance;
	}

	private NetworkManager() {}

	private static class NetworkManagerHolder {
		public static final NetworkManager instance = new NetworkManager();
	}

	public void initialize() {

		wifiManager = (WifiManager) Patchr.applicationContext.getSystemService(Context.WIFI_SERVICE);
		connectivityManager = (ConnectivityManager) Patchr.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		networkChangeReceiver = new NetworkChangeReceiver();

		Patchr.applicationContext.registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		/* Setting system properties. Okhttp picks these up for its connection pooling unless
		   we have passed in a custom connection pool object. */
		System.setProperty("http.maxConnections", String.valueOf(Constants.DEFAULT_MAX_CONNECTIONS));
		System.setProperty("http.keepAlive", "true");
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public boolean isConnected() {
		NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
		return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
	}

	public boolean isAirplaneMode(Context context) {
		ContentResolver cr = context.getContentResolver();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		}
		else
			//noinspection deprecation
			return Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}

	public boolean isWifiEnabled() {
		boolean wifiEnabled = false;
		if (wifiManager != null) {
			wifiEnabled = (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
		}
		return wifiEnabled;
	}

	public boolean isWifiTethered() {
		/*
		 * We use reflection because the method is hidden and unpublished.
		 */
		Boolean isTethered = false;
		if (wifiManager != null) {
			final Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
			for (Method method : wmMethods) {
				if (method.getName().equals("isWifiApEnabled")) {
					try {
						isTethered = (Boolean) method.invoke(wifiManager);
					}
					catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
						Reporting.logException(e);
					}
				}
			}
		}
		return isTethered;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public String getNetworkType() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		if (connectivityManager != null) {
			final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
			if (activeNetwork != null) {
				return getNetworkTypeLabel(activeNetwork.getType(), activeNetwork.getSubtype());
			}
		}
		return "none";
	}

	public Integer getWifiState() {
		return wifiManager.getWifiState();
	}

	public WifiApState getWifiApState() {
		try {
			Method method = wifiManager.getClass().getMethod("getWifiApState");
			int tmp = ((Integer) method.invoke(wifiManager));
			if (tmp > 10) {
				tmp = tmp - 10;
			}
			return WifiApState.class.getEnumConstants()[tmp];
		}
		catch (Exception ignore) {}
		return WifiApState.WIFI_AP_STATE_FAILED;
	}

	private String getNetworkTypeLabel(Integer type, Integer subType) {

		String typeLabel = "none";
		if (type == null || subType == null) return typeLabel;

		if (type == ConnectivityManager.TYPE_WIFI) {
			return "wifi";
		}
		else if (type == ConnectivityManager.TYPE_MOBILE) {
			if (subType == TelephonyManager.NETWORK_TYPE_1xRTT) {        // ~50-100 kbps
				typeLabel = "1xrtt";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_CDMA) {    // ~14-64 kbps
				typeLabel = "cdma";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_EDGE) {    // ~50-100 kbps
				typeLabel = "edge";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_EVDO_0) { // ~ 400-1000 kbps
				typeLabel = "evdo_0";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_EVDO_A) { // ~ 600-1400 kbps
				typeLabel = "evdo_a";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_GPRS) {    // ~ 100 kbps
				typeLabel = "gprs";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_HSDPA) {    // ~ 2-14 Mbps
				typeLabel = "hsdpa";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_HSPA) {    // ~ 700-1700 kbps
				typeLabel = "hspa";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_HSUPA) {    // ~ 1-23 Mbps
				typeLabel = "hsupa";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_UMTS) {    // ~ 400-7000 kbps
				typeLabel = "umts";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_IDEN) {    // ~25 kbps
				typeLabel = "iden";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_EVDO_B) {    // ~5 Mbps
				typeLabel = "evdo_b";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_HSPAP) {    // ~10-20 Mbps
				typeLabel = "hspap";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_EHRPD) {    // ~1-2 Mbps
				typeLabel = "ehrpd";
			}
			else if (subType == TelephonyManager.NETWORK_TYPE_LTE) {    // ~10+ Mbps
				typeLabel = "lte";
			}
			else {
				typeLabel = "unknown";
			}
		}

		return typeLabel;
	}
}
