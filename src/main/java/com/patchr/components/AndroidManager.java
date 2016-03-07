package com.patchr.components;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Html;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.LobbyForm;
import com.patchr.utilities.UI;

import java.util.List;

@SuppressWarnings("ucd")
public class AndroidManager {

	public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private AndroidManager() {}

	private static class AndroidManagerHolder {
		public static final AndroidManager instance = new AndroidManager();
	}

	public static AndroidManager getInstance() {
		return AndroidManagerHolder.instance;
	}

	public static boolean checkPlayServices(Activity activity) {

		GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();

		int status = googleAPI.isGooglePlayServicesAvailable(Patchr.applicationContext);
		if (status != ConnectionResult.SUCCESS) {
			if (googleAPI.isUserResolvableError(status)) {
				showPlayServicesErrorDialog(status, activity, null);
			}
			else {
				Logger.w(activity, "This device is not supported by google play services");
				UI.showToastNotification(StringManager.getString(R.string.error_google_play_services_unsupported), Toast.LENGTH_LONG);
				activity.finish();
			}
			return false;
		}
		return true;
	}

	public static void showPlayServicesErrorDialog(final int status, final Activity activity, final DialogInterface.OnDismissListener dismissListener) {

		final Activity activityTemp = (activity != null) ? activity : Patchr.getInstance().getCurrentActivity();
		if (activityTemp != null) {

			activityTemp.runOnUiThread(new Runnable() {

				GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
				Dialog dialog = googleAPI.getErrorDialog(activityTemp, status, PLAY_SERVICES_RESOLUTION_REQUEST);

				@Override
				public void run() {
					dialog.setCancelable(true);
					dialog.setCanceledOnTouchOutside(false);
					dialog.setOnCancelListener(new OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							UI.showToastNotification(StringManager.getString(R.string.error_google_play_services_unavailable), Toast.LENGTH_LONG);
							if (!(activity instanceof LobbyForm)) {
								Patchr.router.route(activity, Route.SPLASH, null, null);
							}
							else {
								activity.finish();
							}
						}
					});
					if (dismissListener != null) {
						dialog.setOnDismissListener(dismissListener);
					}
					dialog.show();
				}
			});
		}
	}

	public static Boolean appExists(String app) {
		return (getPackageNameByAppName(app) != null);
	}

	public static String getPackageNameByAppName(String app) {
		String packageName = null;
		switch (app) {
			case Constants.TYPE_APP_FOURSQUARE:
				packageName = Constants.PACKAGE_NAME_FOURSQUARE;
				break;
			case Constants.TYPE_APP_TRIPADVISOR:
				packageName = Constants.PACKAGE_NAME_TRIPADVISOR;
				break;
			case Constants.TYPE_APP_TWITTER:
				packageName = Constants.PACKAGE_NAME_TWITTER;
				break;
			case Constants.TYPE_APP_YELP:
				packageName = Constants.PACKAGE_NAME_YELP;
				break;
		}
		return packageName;
	}

	public static String getAppNameByPackageName(String packageName) {
		String appName = null;
		switch (packageName) {
			case Constants.PACKAGE_NAME_FOURSQUARE:
				appName = Constants.TYPE_APP_FOURSQUARE;
				break;
			case Constants.PACKAGE_NAME_TRIPADVISOR:
				appName = Constants.TYPE_APP_TRIPADVISOR;
				break;
			case Constants.PACKAGE_NAME_TWITTER:
				appName = Constants.TYPE_APP_TWITTER;
				break;
			case Constants.PACKAGE_NAME_YELP:
				appName = Constants.TYPE_APP_YELP;
				break;
		}
		return appName;
	}

	public static Boolean isAppInstalled(String appName) {
		String packageName = getPackageNameByAppName(appName);
		return doesPackageExist(packageName);
	}

	public static Boolean hasIntentSupport(String app) {
		return (app.equals(Constants.TYPE_APP_FOURSQUARE)
				|| app.equals(Constants.TYPE_APP_TRIPADVISOR)
				|| app.equals(Constants.TYPE_APP_TWITTER)
				|| app.equals(Constants.TYPE_APP_YELP));
	}

	public static boolean doesPackageExist(String targetPackage) {
		final List<ApplicationInfo> packages;
		packages = Patchr.packageManager.getInstalledApplications(0);
		for (ApplicationInfo packageInfo : packages) {
			if (packageInfo.packageName.equals(targetPackage)) return true;
		}
		return false;
	}

	public static boolean isIntentAvailable(Context context, String action) {
		final Intent intent = new Intent(action);
		final List<ResolveInfo> list = Patchr.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	protected boolean getIsLowBattery() {
	    /*
		 * Returns battery status. TRUE if less than 15% remaining.
		 */
		final IntentFilter batIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		final Intent battery = Patchr.applicationContext.registerReceiver(null, batIntentFilter);
		if (battery != null) {
			final float pctLevel = (float) battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 1) /
					battery.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
			return (pctLevel < 0.15);
		}
		return false;
	}

	public void startMapNavigationNow(Context context, Double latitude, Double longitude, String address, String label) {
		String uri;
		if (address != null) {
			uri = "google.navigation:q=" + address + " (" + label + ")";
		}
		else {
			uri = "google.navigation:q="
					+ String.valueOf(latitude)
					+ "," + String.valueOf(longitude)
					+ "(" + label + ")";
		}
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		if (doesPackageExist("com.google.android.apps.maps")) {
			intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
		}
		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callMapNavigation(Context context, Double latitude, Double longitude, String address, String label) {
		String uri;
		if (address != null) {
			uri = "http://maps.google.com/maps?" + "daddr=" + address + " (" + label + ")";
		}
		else {
			uri = "http://maps.google.com/maps?" + "daddr=" + String.valueOf(latitude) + "," + String.valueOf(longitude) + " (" + label + ")";
		}

		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		if (doesPackageExist("com.google.android.apps.maps")) {
			intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
		}

		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callMapLocalActivity(Context context, String latitude, String longitude, String label) {
		final String uri = "google.local:q="
				+ latitude
				+ "," + longitude
				+ "(" + label + ")";
		final Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		context.startActivity(searchAddress);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callDialerActivity(Context context, String phoneNumber) {
		final String number = "tel:" + phoneNumber.trim();
		final Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(number));
		context.startActivity(callIntent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callBrowserActivity(Context context, String uri) {
		Intent intent = findBrowserApp(context, uri);
		if (intent != null) {
			intent.setData(Uri.parse(uri));
			context.startActivity(intent);
		}
		else {
			intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(uri));
			context.startActivity(intent);
		}
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callSendToActivity(Context context, String placeName, String emailAddress, @NonNull String subject, @NonNull String body) {
		final String uriText = String.format("?subject=%1$s&body=%2$s", subject, body);
		final Intent intent = new Intent(android.content.Intent.ACTION_SENDTO, Uri.fromParts("mailto", emailAddress, uriText.toString()));
		context.startActivity(Intent.createChooser(intent, "Send invite..."));
	}

	public void callSendActivity(Context context, String subject, String bodyHtml) {
		/*
		 * Activity Action: Deliver some data to someone else. Who the data is being delivered to is not specified; it
		 * is up to the receiver of this action to ask the user where the data should be sent.
		 * When launching a SEND intent, you should usually wrap it in a chooser (through createChooser), which will
		 * give the proper interface for the user to pick how to send your data and allow you to specify a prompt
		 * indicating what they are doing.
		 * 
		 * Input: getType is the MIME type of the data being sent. get*Extra can have either a EXTRA_TEXT or
		 * EXTRA_STREAM field, containing the data to be sent. If using EXTRA_TEXT, the MIME type should be
		 * "text/plain"; otherwise it should be the MIME type of the data in EXTRA_STREAM. If using EXTRA_TEXT, you can
		 * also optionally supply EXTRA_HTML_TEXT for clients to retrieve your text with html formatting.
		 * 
		 * As of android.os.Build.VERSION_CODES.JELLY_BEAN, the data being sent can be supplied through
		 * setClipData(ClipData). This allows you to use FLAG_GRANT_READ_URI_PERMISSION when sharing content: URIs and
		 * other advanced features of ClipData. If using this approach, you still must supply the same data through the
		 * EXTRA_TEXT or EXTRA_STREAM fields described below for compatibility with old applications. If you don't set a
		 * ClipData, it will be copied there for you when calling Context.startActivity(Intent).
		 * 
		 * Optional standard extras, which may be interpreted by some recipients as appropriate, are: EXTRA_EMAIL,
		 * EXTRA_CC, EXTRA_BCC, EXTRA_SUBJECT.
		 * 
		 * Output: nothing.
		 */
		final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/html");
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(bodyHtml));
		context.startActivity(Intent.createChooser(intent, "Send invite..."));
	}

	public void callTwitterActivity(Context context, String twitterHandle) {

		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse("https://www.twitter.com/" + twitterHandle));
		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callFoursquareActivity(Context context, String venueId, String sourceUri) {

		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(sourceUri));
		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callOpentableActivity(Context context, String sourceId, String sourceUri) {
		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(sourceUri));
		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callYelpActivity(Context context, String sourceId, String sourceUri) {

		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		String uriFixup = sourceUri.replace("//m.yelp.com", "//www.yelp.com");
		intent.setData(Uri.parse(uriFixup));
		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	public void callGenericActivity(Context context, String sourceId) {
		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(sourceId));
		context.startActivity(intent);
		AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.EXTERNAL_TO);
	}

	private Intent findBrowserApp(Context context, String uri) {
		final String[] browserApps = {
				"com.android.browser",
				"com.android.chrome",
				"com.google.android.browser"};

		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		final List<ResolveInfo> list = Patchr.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		String p;
		for (String browserApp : browserApps) {
			for (ResolveInfo resolveInfo : list) {
				p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(browserApp)) {
					intent.setPackage(p);
					return intent;
				}
			}
		}
		return null;
	}

	public boolean isAviaryInstalled() {
		Intent intent = new Intent("aviary.intent.action.EDIT");
		intent.setType("image/*");
		List<ResolveInfo> list = Patchr.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		}
		else {
			return capitalize(manufacturer) + " " + model;
		}
	}

	private String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		}
		else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}
}
