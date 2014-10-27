package com.aircandi.components;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Notification;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.google.android.gms.gcm.GoogleCloudMessaging;

@SuppressLint("Registered")
public class GcmIntentService extends IntentService {

	public GcmIntentService() {
		super(StringManager.getString(R.string.id_gcm_sender));
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
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				/*
				 * Called when our server sends a message to GCM, and GCM delivers it to the install. If the message
				 * has a payload, its contents are available as extras in the intent.
				 */
				String jsonNotification = extras.getString(Constants.SCHEMA_ENTITY_NOTIFICATION);
				Notification notification = (Notification) Json.jsonToObject(jsonNotification, Json.ObjectType.ENTITY);
				if (notification == null) return;

				User currentUser = Patchr.getInstance().getCurrentUser();
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
				if (targetSchema != null && targetSchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Patchr.getInstance().getEntityManager().setActivityDate(DateTime.nowDate().getTime());
				}

				/* Track */
				NotificationManager.getInstance().getNotifications().put(notification.id, notification);
				NotificationManager.getInstance().setNewNotificationCount(NotificationManager.getInstance().getNewNotificationCount() + 1);

				/*
				 * BACKGROUND, NEARBY, OR TARGET NOT VISIBLE
				 */

				Boolean background = (Patchr.getInstance().getCurrentActivity() == null);
				Boolean showingTarget = showingEntity(notification.targetId);

				if (background
						|| !showingTarget
						|| notification.priority.intValue() == Notification.Priority.ONE) {

					if (background || notification.trigger.equals(Notification.TriggerType.NEARBY)) {

						/* Build intent that can be used in association with the notification */
						IEntityController controller = Patchr.getInstance().getControllerForSchema(targetSchema);
						if (controller != null) {
							Extras bundle = new Extras().setForceRefresh(true);
							String parentId = (notification.parentId != null) ? notification.parentId : null;
							notification.intent = controller.view(Patchr.applicationContext
									, null
									, notification.targetId
									, parentId
									, null
									, bundle.getExtras()
									, false);
						}

					    /*
					     * Send notification - includes sound notification
					     */
						NotificationManager.getInstance().statusNotification(notification, Patchr.applicationContext);
					}
					else {
						/* Chirp */
						MediaManager.playSound(MediaManager.SOUND_ACTIVITY_NEW, 1.0f, 1);
					}
				}

				/*
				 * FOREGROUND
				 */

				else {
					/* Chirp */
					MediaManager.playSound(MediaManager.SOUND_ACTIVITY_NEW, 1.0f, 1);

					/* Vibrate */
					Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					if (vibrator != null && vibrator.hasVibrator()) {
						vibrator.vibrate(new long[]{0, 400, 400, 400}, -1);
					}
				}

				/* Trigger event so subscribers can decide if they care about the notification */
				NotificationManager.getInstance().broadcastNotification(notification);
			}
		}

		/* Release the wake lock provided by WakefulBroadcastReceiver */
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	protected Boolean showingEntity(String entityId) {
		android.app.Activity currentActivity = Patchr.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity instanceof BaseActivity) {
			return ((BaseActivity) currentActivity).related(entityId);
		}
		return false;
	}
}
