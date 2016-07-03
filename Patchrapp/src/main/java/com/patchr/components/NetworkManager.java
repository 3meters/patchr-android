package com.patchr.components;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.exceptions.ClientVersionException;
import com.patchr.exceptions.NoNetworkException;
import com.patchr.objects.ServiceData;
import com.patchr.service.OkHttp;
import com.patchr.service.RequestType;
import com.patchr.service.ResponseFormat;
import com.patchr.service.ServiceRequest;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.MainScreen;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import okhttp3.Response;

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
	 * Exceptions when executing HTTP methods using HttpClient
	 * 
	 * - IOException: Generic transport exceptions (unreliable connection, socket timeout, generally non-fatal.
	 * ClientProtocolException, SocketException and InterruptedIOException are sub classes of IOException.
	 * 
	 * - HttpException: Protocol exceptions. These tend to be fatal and suggest something fundamental is wrong with the
	 * request such as a violation of the http protocol.
	 */

	/* monitor platform changes */
	private IntentFilter      mNetworkStateChangedFilter;
	private BroadcastReceiver mNetworkStateIntentReceiver;

	/* Opportunistically used for crash reporting but not current state */
	private Integer             mWifiState;
	private Integer             mWifiApState;
	private WifiManager         mWifiManager;
	private ConnectivityManager mConnectivityManager;
	private ConnectedState mConnectedState = ConnectedState.NORMAL;
	private OkHttp mOkClient;

	public static final String EXTRA_WIFI_AP_STATE          = "wifi_state";
	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final int    WIFI_AP_STATE_ENABLED        = 3;
	public static final String SERVICE_GROUP_TAG_DEFAULT    = "service";

	private NetworkManager() {
		mOkClient = new OkHttp();
	}

	private static class NetworkManagerHolder {
		public static final NetworkManager instance = new NetworkManager();
	}

	public static NetworkManager getInstance() {
		return NetworkManagerHolder.instance;
	}

	public void initialize() {

		mWifiManager = (WifiManager) Patchr.applicationContext.getSystemService(Context.WIFI_SERVICE);
		mConnectivityManager = (ConnectivityManager) Patchr.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		/*
		 * Setting system properties. Okhttp picks these up for its connection pooling unless
		 * we have passed in a custom connection pool object.
		 */
		System.setProperty("http.maxConnections", String.valueOf(Constants.DEFAULT_MAX_CONNECTIONS));
		System.setProperty("http.keepAlive", "true");

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		/*
		 * Enables registration for changes in network status from http stack
		 */
		mNetworkStateChangedFilter = new IntentFilter();
		mNetworkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mNetworkStateIntentReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(final Context context, final Intent intent) {
				if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

					boolean noConnection = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

					if (noConnection) {
						UI.toast("Lost network connection");
					}
				}
			}
		};
	}

	public ServiceResponse request(final ServiceRequest serviceRequest) {
		/*
		 * This is always being called from a background (non main) thread.
		 */
		if (checkConnectedState() != ConnectedState.NORMAL) {
			return new ServiceResponse(ResponseCode.FAILED, null, new NoNetworkException());
		}

		if (UserManager.shared().authenticated()
				&& (serviceRequest.getRequestType() != RequestType.GET
				&& serviceRequest.getRequestType() != RequestType.DELETE)) {
			serviceRequest.getParameters().putString("user", UserManager.userId);
			serviceRequest.getParameters().putString("session", UserManager.sessionKey);
		}

		ServiceResponse serviceResponse = mOkClient.request(serviceRequest);

		/* Check for valid client version and also capture service status code */
		serviceResponse = clientVersionCheck(serviceRequest, serviceResponse);

		/* Single point to handle request failures. */
		if (serviceResponse.responseCode == ResponseCode.FAILED && serviceRequest.getErrorCheck()) {
			serviceResponse.errorResponse = Errors.getErrorResponse(Patchr.applicationContext, serviceResponse);
			if (serviceRequest.getStopwatch() != null) {
				serviceRequest.getStopwatch().segmentTime("Service call failed");
			}
			if (serviceResponse.exception != null) {
				Logger.w(this, "Service exception: " + serviceResponse.exception.getClass().getSimpleName());
				Logger.w(this, "Service exception: " + serviceResponse.exception.getLocalizedMessage());
			}
			else {
				Logger.w(this, "Service error: (code: " + String.valueOf(serviceResponse.statusCode) + ") " + serviceResponse.statusMessage);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse clientVersionCheck(ServiceRequest serviceRequest, ServiceResponse serviceResponse) {
		if (serviceRequest.getResponseFormat() == ResponseFormat.JSON
				&& !serviceRequest.getIgnoreResponseData()
				&& serviceResponse.exception == null
				&& serviceResponse.data != null) {
			/*
			 * We think anything json is coming from the Aircandi service (except Bing)
			 */
			ServiceData serviceData = (ServiceData) Json.jsonToObject((String) serviceResponse.data, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);

			if (serviceData.error != null && serviceData.error.code != null) {
				serviceResponse.statusCodeService = serviceData.error.code.floatValue();
			}

			if (serviceData.clientMinVersions != null && serviceData.clientMinVersions.containsKey(Patchr.applicationContext.getPackageName())) {
				Integer clientVersionCode = Patchr.getVersionCode(Patchr.applicationContext, MainScreen.class);
				if ((Integer) serviceData.clientMinVersions.get(Patchr.applicationContext.getPackageName()) > clientVersionCode) {
					serviceResponse = new ServiceResponse(ResponseCode.FAILED, null, new ClientVersionException());
				}
			}
		}
		return serviceResponse;
	}

	public Response get(String path, String query) {
		return mOkClient.get(path, query);
	}

	/*--------------------------------------------------------------------------------------------
	 * Connectivity routines
	 *--------------------------------------------------------------------------------------------*/

	public ConnectedState checkConnectedState() {
		int attempts = 0;

		/*
		 * We create a little time for a connection process to complete
		 * Max attempt time = CONNECT_TRIES * CONNECT_WAIT
		 */
		ConnectedState connectedState = ConnectedState.NORMAL;
		while (!NetworkManager.getInstance().isConnected()) {
			attempts++;
			Logger.v(this, "No network connection: attempt: " + String.valueOf(attempts));

			if (attempts >= Constants.CONNECT_TRIES) {
				connectedState = ConnectedState.NONE;
				break;
			}
			try {
				Thread.sleep(Constants.CONNECT_WAIT);
			}
			catch (InterruptedException exception) {
				connectedState = ConnectedState.NONE;
				break;
			}
		}

		synchronized (mConnectedState) {
			mConnectedState = connectedState;
		}
		return mConnectedState;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi") // We check which build version we are using.
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) public static boolean isAirplaneMode(Context context) {
		ContentResolver cr = context.getContentResolver();
		if (Constants.SUPPORTS_JELLY_BEAN_MR1)
			return Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		else
			return Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}

	public boolean isConnected() {
		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
	}

	public Boolean isMobileNetwork() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		Boolean isMobileNetwork = null;
		if (mConnectivityManager != null) {
			final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
			isMobileNetwork = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
		}
		return isMobileNetwork;
	}

	public String getNetworkType() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		if (mConnectivityManager != null) {
			final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
			if (activeNetwork != null) {
				return getNetworkTypeLabel(activeNetwork.getType(), activeNetwork.getSubtype());
			}
		}
		return "none";
	}

	public boolean isWalledGardenConnection() {

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URI_WALLED_GARDEN)
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.NONE)
				.setErrorCheck(false);

		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		if (serviceResponse.responseCode == ResponseCode.SUCCESS)
			return serviceResponse.statusCode != 204;
		else {
			/* We assume a failure means most likely not a walled garden */
			String message = "Walled garden check: failed with exception " + serviceResponse.exception;
			Reporting.breadcrumb(message);
			return false;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Wifi routines
	 *--------------------------------------------------------------------------------------------*/

	public Boolean isWifiEnabled() {
		Boolean wifiEnabled = null;
		if (mWifiManager != null) {
			wifiEnabled = (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
		}
		return wifiEnabled;
	}

	public boolean isWifiTethered() {
		/*
		 * We use reflection because the method is hidden and unpublished.
		 */
		Boolean isTethered = false;
		if (mWifiManager != null) {
			final Method[] wmMethods = mWifiManager.getClass().getDeclaredMethods();
			for (Method method : wmMethods) {
				if (method.getName().equals("isWifiApEnabled")) {
					try {
						isTethered = (Boolean) method.invoke(mWifiManager);
					}
					catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
						Reporting.logException(e);
					}
				}
			}
		}
		return isTethered;
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public Integer getWifiState() {
		return mWifiManager.getWifiState();
	}

	public WIFI_AP_STATE getWifiApState() {
		try {
			Method method = mWifiManager.getClass().getMethod("getWifiApState");
			int tmp = ((Integer) method.invoke(mWifiManager));
			if (tmp > 10) {
				tmp = tmp - 10;
			}
			return WIFI_AP_STATE.class.getEnumConstants()[tmp];
		}
		catch (Exception ignore) {}
		return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
	}

	public ConnectedState getConnectedState() {
		return mConnectedState;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public WifiManager getWifiManager() {
		return mWifiManager;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public enum ResponseCode {
		SUCCESS,
		FAILED,
		INTERRUPTED
	}

	public enum ConnectedState {
		NONE,
		NORMAL,
	}

	public enum WIFI_AP_STATE {
		WIFI_AP_STATE_DISABLING,
		WIFI_AP_STATE_DISABLED,
		WIFI_AP_STATE_ENABLING,
		WIFI_AP_STATE_ENABLED,
		WIFI_AP_STATE_FAILED
	}
}
