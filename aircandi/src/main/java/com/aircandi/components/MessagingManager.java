package com.aircandi.components;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.MessageEvent;
import com.aircandi.objects.Action.EventCategory;
import com.aircandi.objects.ActivityBase;
import com.aircandi.objects.Install;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.exceptions.GcmRegistrationIOException;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Errors;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ucd")
public class MessagingManager {

	public static NotificationManager mNotificationManager;

	private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";
	private GoogleCloudMessaging mGcm;
	private Install              mInstall;
	private Uri                  mSoundUri;
	private Boolean              mNewActivity = false;
	private Map<String, Integer> mCounts      = new HashMap<String, Integer>();

	private MessagingManager() {
		mNotificationManager = (NotificationManager) Aircandi.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mSoundUri = Uri.parse("android.resource://" + Aircandi.applicationContext.getPackageName() + "/" + R.raw.notification_activity);
	}

	private static class NotificationManagerHolder {
		public static final MessagingManager instance = new MessagingManager();
	}

	public static MessagingManager getInstance() {
		return NotificationManagerHolder.instance;
	}

	/*--------------------------------------------------------------------------------------------
	 * GCM
	 *--------------------------------------------------------------------------------------------*/

	public ServiceResponse registerInstallWithGCM() {

		/*
		 * Only called when aircandi application first runs.
		 * 
		 * Called on a background thread.
		 * 
		 * PlayService library check is performed in SplashForm before call this function.
		 * 
		 * Returns as not registered if no registration id or version has changed which clears any current registration
		 * id forcing us to fetch a new one. Registration id and associated app version code are stored in the gcm
		 * shared prefs.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();
		String registrationId = getRegistrationId(Aircandi.applicationContext);
		if (registrationId.isEmpty()) {
			try {
				if (mGcm == null) {
					mGcm = GoogleCloudMessaging.getInstance(Aircandi.applicationContext);
				}
				registrationId = mGcm.register(StringManager.getString(R.string.id_gcm_sender));
				setRegistrationId(Aircandi.applicationContext, registrationId);
				Logger.i(this, "Registered aircandi install with GCM");
			}
			catch (IOException ex) {
				serviceResponse = new ServiceResponse(ResponseCode.FAILED, null, new GcmRegistrationIOException());
				serviceResponse.errorResponse = Errors.getErrorResponse(Aircandi.applicationContext, serviceResponse);
			}
		}
		return serviceResponse;
	}

	public ModelResult registerInstallWithAircandi() {

		Logger.i(this, "Registering install with Aircandi service");

		String registrationId = getRegistrationId(Aircandi.applicationContext);

		Install install = new Install(Aircandi.getInstance().getCurrentUser().id
				, registrationId
				, Aircandi.getinstallId());

		install.clientVersionName = Aircandi.getVersionName(Aircandi.applicationContext, AircandiForm.class);
		install.clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
		install.clientPackageName = Aircandi.applicationContext.getPackageName();

		ModelResult result = Aircandi.getInstance().getEntityManager().registerInstall(install);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Logger.i(this, "Install successfully registered with Aircandi service");
		}
		return result;
	}

	private void setRegistrationId(Context context, String registrationId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int versionCode = Aircandi.getVersionCode(Aircandi.applicationContext, MessagingManager.class);

		Logger.i(this, "Saving GCM registrationId for app version code " + String.valueOf(versionCode));
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(StringManager.getString(R.string.setting_gcm_registration_id), registrationId);
		editor.putInt(StringManager.getString(R.string.setting_gcm_version_code), versionCode);
		editor.apply();
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(StringManager.getString(R.string.setting_gcm_registration_id), "");
		if (registrationId.isEmpty()) {
			Logger.i(this, "GCM registration not found in settings.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersionCode = prefs.getInt(StringManager.getString(R.string.setting_gcm_version_code), Integer.MIN_VALUE);
		int currentVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, MessagingManager.class);
		if (registeredVersionCode != currentVersionCode) {
			Logger.i(this, "GCM app version changed.");
			return "";
		}
		return registrationId;
	}

	private SharedPreferences getGcmPreferences(Context context) {
		return Aircandi.applicationContext.getSharedPreferences(MessagingManager.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	public void broadcastMessage(final ServiceMessage message) {
		BusProvider.getInstance().post(new MessageEvent(message));
	}

	public void notificationForMessage(final ServiceMessage message, Context context) {
	    /*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (message.trigger != null) {
			PreferenceManager preferenceManager = new PreferenceManager();
			if (!preferenceManager.notificationEnabled(message.getTriggerCategory(), message.action.entity)) {
				return;
			}
		}

		String messageTag = getTag(message);
		if (!mCounts.containsKey(messageTag)) {
			mCounts.put(messageTag, 1);
		}
		else {
			Integer count = mCounts.get(messageTag);
			mCounts.put(messageTag, (count++));
		}

		message.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		message.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		final PendingIntent pendingIntent = PendingIntent.getActivity(Aircandi.applicationContext, 0
				, message.intent
				, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Default base notification configuration */

		Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
		PendingIntent deleteIntent = PendingIntent.getBroadcast(Aircandi.applicationContext, 0, intent, 0);

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Aircandi.applicationContext)
				.setContentTitle(message.title)
				.setContentText(message.subtitle)
				.setDeleteIntent(deleteIntent)
				.setNumber(mCounts.get(messageTag))
				.setSmallIcon(R.drawable.ic_stat_notification)
				.setAutoCancel(true)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setVibrate(new long[]{0, 400, 400, 400})
				.setSound(mSoundUri)
				.setOnlyAlertOnce(false)
				.setContentIntent(pendingIntent)
				.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
				.setWhen(System.currentTimeMillis());

		String byImageUri = message.photoBy.getUri();

		/* Large icon */

		if (byImageUri != null) {

			try {
				Integer width = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width);
				@SuppressWarnings("SuspiciousNameCombination")
				Bitmap bitmap = DownloadManager.with(Aircandi.applicationContext)
				                               .load(byImageUri)
				                               .centerCrop()
				                               .resize(width, width)
				                               .get();

				builder.setLargeIcon(bitmap);

				/* Enhance or go with default */
				if (message.action.entity != null
						&& (message.action.getEventCategory().equals(EventCategory.INSERT)
						|| message.action.getEventCategory().equals(EventCategory.SHARE))) {

					IEntityController controller = Aircandi.getInstance().getControllerForSchema(message.action.entity.schema);
					if (controller != null) {
						Integer notificationType = controller.getNotificationType(message.action.entity);
						if (notificationType == NotificationType.BIG_PICTURE) {
							useBigPicture(builder, message);
						}
						else if (notificationType == NotificationType.BIG_TEXT) {
							useBigText(builder, message);
						}
						builder.setTicker(controller.getNotificationTicker(message, message.action.getEventCategory()));
					}
				}
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
		}
		mNotificationManager.notify(getTag(message), 0, builder.build());
	}

	public void useBigPicture(final NotificationCompat.Builder builder, final ServiceMessage message) {

		final String imageUri = message.action.entity.getPhoto().getUri();

		try {
			Bitmap bitmap = DownloadManager.with(Aircandi.applicationContext)
			                               .load(imageUri)
			                               .get();

			NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.setBigContentTitle(message.title)
					.setSummaryText(message.subtitle);

			builder.setStyle(style);
			String tag = getTag(message);
			mNotificationManager.notify(tag, 0, builder.build());
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public void useBigText(NotificationCompat.Builder builder, ServiceMessage message) {
		NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
				.setBigContentTitle(message.title)
				.bigText(message.action.entity.description)
				.setSummaryText(message.subtitle);

		builder.setStyle(style);

		mNotificationManager.notify(getTag(message), 0, builder.build());
	}

	public void cancelNotification(String tag) {
		mNotificationManager.cancel(tag, 0);
	}

	public void cancelNotifications() {
		mNotificationManager.cancelAll();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public String getTag(ActivityBase activity) {
		if (activity.action.getEventCategory().equals(EventCategory.INSERT))
			return Tag.INSERT;
		else if (activity.action.getEventCategory().equals(EventCategory.SHARE))
			return Tag.SHARE;

		return Tag.UPDATE;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Install getInstall() {
		return mInstall;
	}

	public void setInstall(Install device) {
		mInstall = device;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public Boolean getNewActivity() {
		return mNewActivity;
	}

	public void setNewActivity(Boolean newActivity) {
		mNewActivity = newActivity;
	}

	public void clearCounts() {
		mCounts.clear();
	}

	public static class Tag {
		public static String INSERT  = "insert";
		public static String SHARE   = "share";
		public static String UPDATE  = "update";
		@SuppressWarnings("ucd")
		public static String REFRESH = "refresh";
	}
}
