package com.patchr.components;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.ui.LobbyScreen;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

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

		Utils.guard(activity != null);

		GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();

		int status = googleAPI.isGooglePlayServicesAvailable(Patchr.applicationContext);
		if (status != ConnectionResult.SUCCESS) {
			if (googleAPI.isUserResolvableError(status)) {
				showPlayServicesErrorDialog(status, activity, null);
			}
			else {
				Logger.w(activity, "This device is not supported by google play services");
				UI.toast(StringManager.getString(R.string.error_google_play_services_unsupported));
				if (activity != null) {
					activity.finish();
				}
			}
			return false;
		}
		return true;
	}

	public static void showPlayServicesErrorDialog(final int status, final Activity activity, final DialogInterface.OnDismissListener dismissListener) {

		activity.runOnUiThread(() -> {
			GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
			Dialog dialog = googleAPI.getErrorDialog(activity, status, PLAY_SERVICES_RESOLUTION_REQUEST);
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					UI.toast(StringManager.getString(R.string.error_google_play_services_unavailable));
					if (!(activity instanceof LobbyScreen)) {
						UI.routeLobby(activity);
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
		});
	}

	public static boolean doesPackageExist(String targetPackage) {
		final List<ApplicationInfo> packages;
		packages = Patchr.applicationContext.getPackageManager().getInstalledApplications(0);
		for (ApplicationInfo packageInfo : packages) {
			if (packageInfo.packageName.equals(targetPackage)) return true;
		}
		return false;
	}

	public static boolean isIntentAvailable(Context context, String action) {
		final Intent intent = new Intent(action);
		final List<ResolveInfo> list = Patchr.applicationContext.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
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
	}

	public String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return Utils.capitalize(model);
		}
		else {
			return Utils.capitalize(manufacturer) + " " + model;
		}
	}
}
