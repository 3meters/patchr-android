package com.patchr.components;

import android.content.Intent;

import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;
import com.patchr.Patchr;
import com.patchr.service.RestClient;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import org.json.JSONObject;

@SuppressWarnings("ucd")
public class NotificationManager implements OneSignal.IdsAvailableHandler, OneSignal.NotificationOpenedHandler {

	public static String installId;

	private static NotificationManager instance = new NotificationManager();

	public static NotificationManager getInstance() {
		return instance;
	}

	private NotificationManager() {
		OneSignal.startInit(Patchr.applicationContext)
			.setNotificationOpenedHandler(this)
			.inFocusDisplaying(OneSignal.OSInFocusDisplayOption.None)
			.init();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void notificationOpened(OSNotificationOpenResult result) {

		Logger.d(this, "Notification opened");

		JSONObject data = result.notification.payload.additionalData;
		if (data != null) {
			String targetId = data.optString("targetId");
			Intent intent = UI.browseEntity(targetId, Patchr.applicationContext, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
			Patchr.applicationContext.startActivity(intent);
		}
	}

	@Override public void idsAvailable(String installId, String registrationId) {

		Logger.d(this, String.format("OneSignal installId: %1$s", installId));
		NotificationManager.installId = installId;

		RestClient.getInstance().registerInstall()
			.subscribe(
				response -> {
					Logger.i(this, String.format("Install registered or updated: %1$s", installId));
				},
				error -> {
					Logger.w(this, "Error during registerInstall");
					Errors.handleError(Patchr.applicationContext, error);
				});
	}

	public void activateUser() {
		/*
		 * Deactivation is handled by service logout which handles clearing user from install.
		 */
		Utils.guard(UserManager.userId != null, "Activating user for notifications requires a current user.");
		OneSignal.syncHashedEmail(UserManager.userId);
		OneSignal.idsAvailable(this);
	}
}
