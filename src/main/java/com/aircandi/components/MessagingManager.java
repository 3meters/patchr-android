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

import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.MessageEvent;
import com.aircandi.exceptions.GcmRegistrationIOException;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Action.EventCategory;
import com.aircandi.objects.Install;
import com.aircandi.objects.Message;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
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
	private Boolean              mNewMessage = false;
	private Boolean              mNewAlert   = false;
	private Map<String, Message> mAlerts     = new HashMap<String, Message>();
	private Map<String, Message> mMessages   = new HashMap<String, Message>();

	private MessagingManager() {
		mNotificationManager = (NotificationManager) Patch.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mSoundUri = Uri.parse("android.resource://" + Patch.applicationContext.getPackageName() + "/" + R.raw.notification_activity);
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
		String registrationId = getRegistrationId(Patch.applicationContext);
		if (registrationId.isEmpty()) {
			try {
				if (mGcm == null) {
					mGcm = GoogleCloudMessaging.getInstance(Patch.applicationContext);
				}
				registrationId = mGcm.register(StringManager.getString(R.string.id_gcm_sender));
				setRegistrationId(Patch.applicationContext, registrationId);
				Logger.i(this, "Registered aircandi install with GCM");
			}
			catch (IOException ex) {
				serviceResponse = new ServiceResponse(ResponseCode.FAILED, null, new GcmRegistrationIOException());
				serviceResponse.errorResponse = Errors.getErrorResponse(Patch.applicationContext, serviceResponse);
			}
		}
		return serviceResponse;
	}

	public ModelResult registerInstallWithAircandi() {

		Logger.i(this, "Registering install with Aircandi service");

		String registrationId = getRegistrationId(Patch.applicationContext);

		Install install = new Install(Patch.getInstance().getCurrentUser().id
				, registrationId
				, Patch.getinstallId());

		install.clientVersionName = Patch.getVersionName(Patch.applicationContext, AircandiForm.class);
		install.clientVersionCode = Patch.getVersionCode(Patch.applicationContext, AircandiForm.class);
		install.clientPackageName = Patch.applicationContext.getPackageName();
		install.deviceName = AndroidManager.getInstance().getDeviceName();

		ModelResult result = Patch.getInstance().getEntityManager().registerInstall(install);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Logger.i(this, "Install successfully registered with Aircandi service");
		}
		return result;
	}

	private void setRegistrationId(Context context, String registrationId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int versionCode = Patch.getVersionCode(Patch.applicationContext, MessagingManager.class);

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
		int currentVersionCode = Patch.getVersionCode(Patch.applicationContext, MessagingManager.class);
		if (registeredVersionCode != currentVersionCode) {
			Logger.i(this, "GCM app version changed.");
			return "";
		}
		return registrationId;
	}

	private SharedPreferences getGcmPreferences(Context context) {
		return Patch.applicationContext.getSharedPreferences(MessagingManager.class.getSimpleName(), Context.MODE_PRIVATE);
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

		message.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		message.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		final PendingIntent pendingIntent = PendingIntent.getActivity(Patch.applicationContext, 0
				, message.intent
				, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Default base notification configuration */

		Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
		PendingIntent deleteIntent = PendingIntent.getBroadcast(Patch.applicationContext, 0, intent, 0);

		String messageTag = getTag(message);
		Integer count = messageTag.equals(Tag.ALERT) ? mAlerts.size() : mMessages.size();

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Patch.applicationContext)
				.setContentTitle(message.title)
				.setContentText(message.subtitle)
				.setDeleteIntent(deleteIntent)
				.setNumber(count)
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
				Integer width = Patch.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width);
				@SuppressWarnings("SuspiciousNameCombination")
				Bitmap bitmap = DownloadManager.with(Patch.applicationContext)
				                               .load(byImageUri)
				                               .centerCrop()
				                               .resize(width, width)
				                               .get();

				builder.setLargeIcon(bitmap);

				/* Enhance or go with default */
				if (message.action.entity != null
						&& (message.action.getEventCategory().equals(EventCategory.INSERT)
						|| message.action.getEventCategory().equals(EventCategory.SHARE))) {

					IEntityController controller = Patch.getInstance().getControllerForSchema(message.action.entity.schema);
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
			catch (IOException e) {
				Reporting.logException(e);
			}
		}
		mNotificationManager.notify(messageTag, 0, builder.build());
	}

	public void useBigPicture(final NotificationCompat.Builder builder, final ServiceMessage message) {

		final String imageUri = message.action.entity.getPhoto().getUri();

		try {
			Bitmap bitmap = DownloadManager.with(Patch.applicationContext)
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
		catch (IOException e) {
			Reporting.logException(e);
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

	public String getTag(ServiceMessage activity) {
		String eventCategory = activity.action.getEventCategory();
		if (eventCategory.equals(EventCategory.INSERT))
			return Tag.INSERT;
		else if (eventCategory.equals(EventCategory.SHARE))
			return Tag.SHARE;
		else
			return Tag.ALERT;
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

	public Boolean getNewActivity() {
		return (mNewMessage || mNewAlert);
	}

	public void setNewActivity(Boolean visible) {
		mNewMessage = visible;
		mNewAlert = visible;
	}

	public Boolean getNewMessage() {
		return mNewMessage;
	}

	public void setNewMessage(Boolean newMessage) {
		mNewMessage = newMessage;
	}

	public Boolean getNewAlert() {
		return mNewAlert;
	}

	public void setNewAlert(Boolean newAlert) {
		mNewAlert = newAlert;
	}

	public Map<String, Message> getAlerts() {
		return mAlerts;
	}

	public Map<String, Message> getMessages() {
		return mMessages;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class Tag {
		public static String INSERT = "insert";
		public static String SHARE  = "share";
		public static String ALERT  = "alert";
	}
}
