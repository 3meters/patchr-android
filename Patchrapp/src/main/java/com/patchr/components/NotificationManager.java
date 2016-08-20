package com.patchr.components;

import android.content.Intent;
import android.content.SharedPreferences;

import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.service.RestClient;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import org.json.JSONObject;

@SuppressWarnings("ucd")
public class NotificationManager implements OneSignal.IdsAvailableHandler, OneSignal.NotificationOpenedHandler {

	public static String installId;
	public static Long   installDate;

	private static NotificationManager instance = new NotificationManager();

	public static NotificationManager getInstance() {
		return instance;
	}

	private NotificationManager() {
		installDate = Patchr.settings.getLong(StringManager.getString(R.string.setting_unique_id_date), 0);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void notificationOpened(OSNotificationOpenResult result) {

		Logger.d(this, "Notification opened");

		JSONObject data = result.notification.payload.additionalData;
		if (data != null) {
			String targetId = data.optString("targetId");
			String parentId = data.optString("parentId");
			String eventType = data.optString("event");

			Intent intent = UI.browseEntity(targetId, Patchr.applicationContext, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
			Patchr.applicationContext.startActivity(intent);
		}
	}

	@Override public void idsAvailable(String installId, String registrationId) {

		Logger.d(this, String.format("OneSignal installId: %1$s", installId));
		NotificationManager.installId = installId;

		if (installDate == 0) {
			installDate = DateTime.nowDate().getTime();
			SharedPreferences.Editor editor = Patchr.settings.edit();
			editor.putLong(StringManager.getString(R.string.setting_unique_id_date), installDate);
			editor.apply();
		}

		RestClient.getInstance().preflight()
			.flatMap(response -> {
				return RestClient.getInstance().registerInstall();
			})
			.subscribe(
				response -> {
				},
				error -> {
					Errors.handleError(Patchr.applicationContext, error);
				});
	}
}
