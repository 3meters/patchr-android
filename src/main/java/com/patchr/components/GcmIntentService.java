package com.patchr.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;

import com.google.android.gms.gcm.GcmListenerService;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.objects.Entity;
import com.patchr.objects.Notification;
import com.patchr.objects.User;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Json;

import java.util.LinkedHashMap;

@SuppressLint("Registered")
public class GcmIntentService extends GcmListenerService {

	@Override public void onMessageReceived(String from, Bundle extras) {
		/*
		 * Filter messages based on message type. Since it is likely that GCM will be
		 * extended in the future with new message types, just ignore any message types you're
		 * not interested in, or that you don't recognize.
		 */
		if (extras.getString("registration_id") != null) {
			Logger.i(this, "Install registered with Gcm by parse");
		}
		else {
			/*
			 * Called when our server sends a message to Parse and Parse delivers it to the install. If the message
			 * has a payload, its contents are available as extras in the intent. Parse can send notifications
			 * from a web console among other methods. They have a different format than the ones sent by
			 * the service.
			 */
			String data = extras.getString("data");
			if (isEntity(data)) {

				@SuppressWarnings("ConstantConditions") Notification notification = (Notification) Json.jsonToObject(data, Json.ObjectType.ENTITY);

				User currentUser = UserManager.currentUser;
				if (notification.userId != null && currentUser != null && currentUser.id.equals(notification.userId))
					return;

				/*
				 * Tickle activity date on current user to flag auto refresh for activity list. This service
				 * can be woken up when we don't have a current user. We do this regardless of whether
				 * the application is in the foreground or not.
				 */
				if (currentUser != null) {
					currentUser.activityDate = notification.sentDate;
				}

				/* Tickle activity date on entity manager because that is monitored by radar. */
				String targetSchema = Entity.getSchemaForId(notification.targetId);
				if (targetSchema != null && targetSchema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
					DataController.getInstance().setActivityDate(DateTime.nowDate().getTime());
				}

				/* Track */
				NotificationManager.getInstance().getNotifications().put(notification.id, notification);
				NotificationManager.getInstance().setNewNotificationCount(NotificationManager.getInstance().getNewNotificationCount() + 1);

				/*
				 * BACKGROUND, NEARBY, OR TARGET NOT VISIBLE
				 */

				Boolean background = Foreground.get().isBackground();
				/*
				 * Notifications associated with unmuted patches are priority.ONE
				 * Notifications associated with muted patches are priority.TWO with the following exceptions:
				 *
				 * - Patch inserted nearby
				 * - Action requests like join request, join approval, message share, patch invite.
				 */
				if (background) {
					if (notification.priority.intValue() == Notification.Priority.ONE) {
						/*
						 * Build intent that can be used in association with the notification
						 * - Intents route directly to the activity if the application is already running.
						 * - If app needs to be started, we route to splash for init and then fire the
						 *   intent again.
						 */
						if (targetSchema != null) {
							Bundle extrasOut = new Bundle();
							extrasOut.putBoolean(Constants.EXTRA_REFRESH_FROM_SERVICE, true);
							extrasOut.putString(Constants.EXTRA_NOTIFICATION_ID, notification.id);
							notification.intent = Patchr.router.browse(Patchr.applicationContext, notification.targetId, extrasOut, false);
						}
					    /*
					     * Send notification - includes sound notification
					     */
						NotificationManager.getInstance().statusNotification(notification, Patchr.applicationContext);
					}
				}

				/*
				 * FOREGROUND
				 */

				else {
					if (notification.priority.intValue() == Notification.Priority.ONE) {
						/* Chirp */
						MediaManager.playSound(MediaManager.SOUND_ACTIVITY_NEW, 1.0f, 1);   // Won't play if user turned off sounds

						/* Vibrate */
						Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						if (vibrator != null && vibrator.hasVibrator()) {
							vibrator.vibrate(new long[]{0, 400, 400, 400}, -1);
						}
					}
				}

				/* Trigger event so subscribers can decide if they care about the notification */
				NotificationManager.getInstance().broadcastNotification(notification);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected Boolean isEntity(String json) {
		if (json != null) {
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) Json.jsonToObject(json, Json.ObjectType.OBJECT);
			if (map.get("schema") != null) {
				return true;
			}
		}
		return false;
	}
}
