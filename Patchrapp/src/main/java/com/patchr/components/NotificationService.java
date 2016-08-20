package com.patchr.components;

import android.content.Context;
import android.os.Vibrator;

import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.utilities.Booleans;

import org.json.JSONObject;

public class NotificationService extends NotificationExtenderService {
	@Override
	protected boolean onNotificationProcessing(OSNotificationReceivedResult notification) {

		JSONObject data = notification.payload.additionalData;
		String pushNotificationType = Patchr.settings.getString(StringManager.getString(R.string.pref_push_notification_type)
			, StringManager.getString(R.string.pref_push_notification_type_default));

		if ("none".equals(pushNotificationType)) {
			return true;
		}
		else if ("messages_only".equals(pushNotificationType)) {
			if (data != null) {
				String eventType = data.optString("event");
				if (!"insert_entity_message_content".equals(eventType)) {
					return true;
				}
			}
		}

		if (notification.isAppInFocus) {
			if (data != null) {
				String targetId = data.optString("targetId");
				String parentId = data.optString("parentId");
				String eventType = data.optString("event");
				int priority = data.optInt("priority", 1);

				if (priority == 1) {
					/* Chirp */
					if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_push_notification_sound)
						, Booleans.getBoolean(R.bool.pref_push_notifications_sound_default))) {
						MediaManager.playSound(MediaManager.SOUND_ACTIVITY_NEW, 1.0f, 1);   // Won't play if user turned off sounds
					}

					/* Vibrate */
					if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_push_notification_vibrate)
						, Booleans.getBoolean(R.bool.pref_push_notifications_vibrate_default))) {
						Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						if (vibrator != null && vibrator.hasVibrator()) {
							vibrator.vibrate(new long[]{0, 400, 400, 400}, -1);
						}
					}
				}
				Dispatcher.getInstance().post(new NotificationReceivedEvent(targetId, parentId, eventType));
			}
		}
		return notification.isAppInFocus;    // true stops notification from displaying (alert or system notification)
	}
}
