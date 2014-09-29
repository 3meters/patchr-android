package com.aircandi.components;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Action.EventCategory;
import com.aircandi.objects.MessageTriggerType;
import com.aircandi.objects.EventType;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("Registered")
public class GcmIntentService extends IntentService {

	// wakelock
	protected static final String WAKELOCK_KEY = "GCM_LIB";
	protected static PowerManager.WakeLock sWakeLock;

	// Java lock used to synchronize access to sWakelock
	protected static final Object LOCK = GcmIntentService.class;

	public GcmIntentService() {
		super(StringManager.getString(R.string.id_gcm_sender));
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		try {

			Bundle extras = intent.getExtras();
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
			String messageType = gcm.getMessageType(intent);

			if (!extras.isEmpty()) {  // has effect of unparcelling Bundle

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
					String jsonMessage = extras.getString("message");
					ServiceMessage message = (ServiceMessage) Json.jsonToObject(jsonMessage, Json.ObjectType.SERVICE_MESSAGE);
					/*
					 * If the primary entity of the message isn't one we have a controller for, it won't be deserialized
					 * and entity will be null. That is a good reason to discard the message.
					 */
					if (message == null || message.action.entity == null) return;

					/* Is this a message event we know how to handle */
					if (message.action.getEventCategory().equals(EventCategory.UNKNOWN)) return;
					if (!isValidSchema(message)) return;
					if (!isValidEvent(message)) return;

					/*
					 * Tickle activity date on current user to flag auto refresh for activity list. This service
					 * can be woken up when we don't have a current user. We do this regardless of whether
					 * the application is in the foreground or not.
					 */
					if (Patch.getInstance().getCurrentUser() != null) {
						Patch.getInstance().getCurrentUser().activityDate = message.sentDate;
					}

					/* Tickle activity date on entity manager because that is monitored by radar. */
					if ((message.action.entity != null && message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE))
							|| (message.action.toEntity != null && message.action.toEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE))) {
						Patch.getInstance().getEntityManager().setActivityDate(DateTime.nowDate().getTime());
					}

					/* Do some cache stuffing */

					/*
					 * Hmm, if this is a message for a place, the place will get replace with this toEntity which
					 * is not exactly a full monty place entity. Its ok if its a new place not in the cache
					 * because it provide context info for things like comments and pictures.
					 */
					if (message.action.toEntity != null) {
						if (!EntityManager.getEntityCache().containsKey(message.action.toEntity.id)) {
							EntityManager.getEntityCache().upsertEntity(message.action.toEntity);
							message.action.entity.toId = message.action.toEntity.id;
						}
					}

					/*
					 * BACKGROUND, NEARBY, OR TARGET NOT VISIBLE
					 */

					Boolean background = (Patch.getInstance().getCurrentActivity() == null);
					Boolean targetVisible = targetContextVisible(message);

					if (background || !targetVisible || message.getTriggerCategory().equals(MessageTriggerType.TriggerType.NEARBY)) {

						if (!message.getTriggerCategory().equals(MessageTriggerType.TriggerType.NEARBY)) {
							MessagingManager.getInstance().setNewActivity(true);
						}

						if (background || message.getTriggerCategory().equals(MessageTriggerType.TriggerType.NEARBY)) {

						    /* Build intent that can be used in association with the notification */
							if (message.action.entity != null) {
								IEntityController controller = Patch.getInstance().getControllerForSchema(message.action.entity.schema);
								Extras bundle = new Extras().setForceRefresh(true);
								String parentId = (message.action.toEntity != null) ? message.action.toEntity.id : null;
								message.intent = controller.view(Patch.applicationContext, null, message.action.entity.id, parentId, null,
										bundle.getExtras(),
										false);
							}

						    /* Customize title and subtitle before broadcasting */
							Patch.getInstance().getActivityDecorator().decorate(message);

						    /* Send notification */
							MessagingManager.getInstance().notificationForMessage(message, Patch.applicationContext);
						}
						else {
							MediaManager.playSound(MediaManager.SOUND_ACTIVITY_NEW, 1.0f, 1);
						}
					}

					/*
					 * FOREGROUND
					 */

					else {

						Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						if (vibrator != null && vibrator.hasVibrator()) {
							vibrator.vibrate(new long[]{0, 400, 400, 400}, -1);
						}
						MediaManager.playSound(MediaManager.SOUND_ACTIVITY_NEW, 1.0f, 1);
					}

					/* Trigger event so subscribers can decide if they care about the activity */
					MessagingManager.getInstance().broadcastMessage(message);
				}
			}
		}
		finally {
			/*
			 * Release the power lock, so phone can get back to sleep. The lock is reference-counted by default, so
			 * multiple messages are ok.
			 */
			synchronized (LOCK) {
				if (sWakeLock != null) {
					Logger.v(this, "Releasing wakelock");
					sWakeLock.release();
				}
				else {
					Logger.e(this, "Wakelock reference is null");
				}
			}
		}

	}

	protected Boolean targetContextVisible(ServiceMessage message) {
		/*
		 * If user is currently on the activities list, it will be auto refreshed
		 * so don't show indicator in the tab.
		 */
		Boolean showingEntity = false;
		if (message.action.toEntity != null) {
			showingEntity = showingEntity(message.action.toEntity.id);
		}
		return (showingEntity);
	}

	@SuppressWarnings("ucd")
	protected Boolean triggerVibratorAlert(ServiceMessage message) {
		/*
		 * If user is currently on the activities list, it will be auto refreshed
		 * so don't show indicator in the tab.
		 */
		Boolean showingActivities = showingActivities();
		Boolean showingEntity = false;
		if (message.action.toEntity != null) {
			showingEntity = showingEntity(message.action.toEntity.id);
		}
		return !(showingActivities || showingEntity);
	}

	protected Boolean showingActivities() {
		android.app.Activity currentActivity = Patch.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.getClass().equals(AircandiForm.class)) {
			BaseFragment fragment = (BaseFragment) ((AircandiForm) currentActivity).getCurrentFragment();
			if (fragment != null && fragment.isActivityStream()) {
				return true;
			}
		}
		return false;
	}

	protected Boolean showingEntity(String entityId) {
		android.app.Activity currentActivity = Patch.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity instanceof BaseActivity) {
			return ((BaseActivity) currentActivity).related(entityId);
		}
		return false;
	}

	protected Boolean isValidSchema(ServiceMessage message) {
		String[] validSchemas = {com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, Constants.SCHEMA_ENTITY_PLACE};
		String[] validToSchemas = {com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, Constants.SCHEMA_ENTITY_PLACE, Constants.SCHEMA_ENTITY_USER};

		if (message.action.entity != null) {
			if (!Arrays.asList(validSchemas).contains(message.action.entity.schema)) return false;
		}
		if (message.action.toEntity != null) {
			if (!Arrays.asList(validToSchemas).contains(message.action.toEntity.schema))
				return false;
		}

		return true;
	}

	protected Boolean isValidEvent(ServiceMessage message) {
		List<String> events = new ArrayList<String>();
		events.add(EventType.INSERT_PLACE);
		events.add(EventType.INSERT_MESSAGE);
		events.add(EventType.INSERT_MESSAGE_SHARE);

		if (message.action.entity != null) {
			if (!events.contains(message.action.event)) return false;
		}

		return true;
	}

	public static void runIntentInService(Context context, Intent intent, String className) {
		synchronized (LOCK) {
			if (sWakeLock == null) {
				/* This is called from BroadcastReceiver, there is no init. */
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
			}
		}
		Logger.v(context, "Acquiring wakelock");
		sWakeLock.acquire();
		intent.setClassName(context, className);
		context.startService(intent);
	}
}
