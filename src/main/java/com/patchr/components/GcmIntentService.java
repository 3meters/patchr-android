package com.patchr.components;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Entity;
import com.patchr.objects.Notification;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Json;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.LinkedHashMap;

@SuppressLint("Registered")
public class GcmIntentService extends IntentService {

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that GCM will be
			 * extended in the future with new message types, just ignore any message types you're
			 * not interested in, or that you don't recognize.
			 */
			if (extras.getString("registration_id") != null) {
				Logger.i(this, "Install registered with Gcm by parse");
			}
			else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				/*
				 * Called when our server sends a message to Parse and Parse delivers it to the install. If the message
				 * has a payload, its contents are available as extras in the intent. Parse can send notifications
				 * from a web console among other methods. They have a different format than the ones sent by
				 * the service.
				 */
				String data = extras.getString("data");
				if (isEntity(data)) {

					@SuppressWarnings("ConstantConditions") Notification notification = (Notification) Json.jsonToObject(data, Json.ObjectType.ENTITY);

					User currentUser = UserManager.getInstance().getCurrentUser();
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

					Boolean background = (Patchr.getInstance().getCurrentActivity() == null);

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
								IEntityController controller = Patchr.getInstance().getControllerForSchema(targetSchema);
								Bundle extrasOut = new Bundle();
								extrasOut.putBoolean(Constants.EXTRA_REFRESH_FROM_SERVICE, true);
								extrasOut.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.VIEW_TO);
								extrasOut.putString(Constants.EXTRA_NOTIFICATION_ID, notification.id);
								String parentId = (notification.parentId != null) ? notification.parentId : null;
								notification.intent = controller.view(Patchr.applicationContext
										, null
										, notification.targetId
										, parentId
										, null
										, extrasOut
										, false);
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

		/* Release the wake lock provided by WakefulBroadcastReceiver */
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	protected Boolean isEntity(String json) {
		if (json != null) {
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) Json.jsonToObject(json, Json.ObjectType.OBJECT);
			if (map.get("schema") != null) {
				return true;
			}
		}
		return false;
	}

	protected Boolean showingEntity(String entityId) {
		android.app.Activity currentActivity = Patchr.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity instanceof BaseActivity) {
			return ((BaseActivity) currentActivity).related(entityId);
		}
		return false;
	}
}
