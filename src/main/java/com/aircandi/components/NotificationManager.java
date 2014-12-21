package com.aircandi.components;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.NotificationEvent;
import com.aircandi.exceptions.GcmRegistrationIOException;
import com.aircandi.objects.Install;
import com.aircandi.objects.Notification;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ucd")
public class NotificationManager {

	public static android.app.NotificationManager mNotificationManager;

	private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";
	private GoogleCloudMessaging mGcm;
	private Install              mInstall;
	private Uri                  mSoundUri;
	private Integer                   mNewNotificationCount = 0;
	private Map<String, Notification> mNotifications        = new HashMap<String, Notification>();

	private NotificationManager() {
		mNotificationManager = (android.app.NotificationManager) Patchr.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mSoundUri = Uri.parse("android.resource://" + Patchr.applicationContext.getPackageName() + "/" + R.raw.notification_activity);
	}

	private static class NotificationManagerHolder {
		public static final NotificationManager instance = new NotificationManager();
	}

	public static NotificationManager getInstance() {
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
		 * PlayService library check is performed in SplashForm before calling this function.
		 * 
		 * Returns as not registered if no registration id or version has changed which clears any current registration
		 * id forcing us to fetch a new one. Registration id and associated app version code are stored in the gcm
		 * shared prefs.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();
		String registrationId = getRegistrationId(Patchr.applicationContext);
		if (registrationId.isEmpty()) {
			try {
				if (mGcm == null) {
					mGcm = GoogleCloudMessaging.getInstance(Patchr.applicationContext);
				}
				registrationId = mGcm.register(StringManager.getString(R.string.id_gcm_sender));
				setRegistrationId(Patchr.applicationContext, registrationId);
				Logger.i(this, "Registered aircandi install with GCM");
			}
			catch (IOException ex) {
				serviceResponse = new ServiceResponse(ResponseCode.FAILED, null, new GcmRegistrationIOException());
				serviceResponse.errorResponse = Errors.getErrorResponse(Patchr.applicationContext, serviceResponse);
			}
		}
		return serviceResponse;
	}

	public ModelResult registerInstallWithAircandi() {

		Logger.i(this, "Registering install with Aircandi service");

		String registrationId = getRegistrationId(Patchr.applicationContext);

		Install install = new Install(Patchr.getInstance().getCurrentUser().id
				, registrationId
				, Patchr.getInstance().getinstallId());

		install.clientVersionName = Patchr.getVersionName(Patchr.applicationContext, AircandiForm.class);
		install.clientVersionCode = Patchr.getVersionCode(Patchr.applicationContext, AircandiForm.class);
		install.clientPackageName = Patchr.applicationContext.getPackageName();
		install.deviceName = AndroidManager.getInstance().getDeviceName();

		ModelResult result = Patchr.getInstance().getEntityManager().registerInstall(install);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Logger.i(this, "Install successfully registered with Aircandi service");
		}
		return result;
	}

	private void setRegistrationId(Context context, String registrationId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int versionCode = Patchr.getVersionCode(Patchr.applicationContext, NotificationManager.class);

		Logger.i(this, "GCM: saving gcm registrationId for app version code " + String.valueOf(versionCode));
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(StringManager.getString(R.string.setting_gcm_registration_id), registrationId);
		editor.putInt(StringManager.getString(R.string.setting_gcm_version_code), versionCode);
		editor.apply();
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(StringManager.getString(R.string.setting_gcm_registration_id), "");
		if (registrationId.isEmpty()) {
			Logger.i(this, "GCM: registration not found in settings.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersionCode = prefs.getInt(StringManager.getString(R.string.setting_gcm_version_code), Integer.MIN_VALUE);
		int currentVersionCode = Patchr.getVersionCode(Patchr.applicationContext, NotificationManager.class);
		if (registeredVersionCode != currentVersionCode) {
			Logger.i(this, "GCM: app version changed.");
			return "";
		}
		Logger.i(this, "GCM: app version unchanged so using locally cached registrationId.");
		return registrationId;
	}

	private SharedPreferences getGcmPreferences(Context context) {
		return Patchr.applicationContext.getSharedPreferences(NotificationManager.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	public void broadcastNotification(final Notification notification) {
		BusProvider.getInstance().post(new NotificationEvent(notification));
	}

	public void statusNotification(final Notification notification, Context context) {
	    /*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (notification.trigger != null && notification.event != null) {
			PreferenceManager preferenceManager = new PreferenceManager();
			if (!preferenceManager.notificationEnabled(notification.getTriggerCategory())) {
				return;
			}
		}

		notification.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notification.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent pendingIntent = TaskStackBuilder
				.create(Patchr.applicationContext)
				.addNextIntent(new Intent(Patchr.applicationContext, AircandiForm.class))
				.addNextIntent(notification.intent)
				.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

		//		final PendingIntent pendingIntent = PendingIntent.getActivity(Patchr.applicationContext, 0
		//				, notification.intent
		//				, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Default base notification configuration */

		PendingIntent deleteIntent = PendingIntent.getBroadcast(Patchr.applicationContext
				, 0
				, new Intent(NOTIFICATION_DELETED_ACTION)
				, 0);

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Patchr.applicationContext)
				.setContentTitle(StringManager.getString(R.string.name_app))
				.setContentText(Html.fromHtml(notification.subtitle))
				.setDeleteIntent(deleteIntent)
				.setNumber(mNewNotificationCount)
				.setSmallIcon(R.drawable.ic_stat_notification)
				.setAutoCancel(true)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setVibrate(new long[]{0, 400, 400, 400})
				.setSound(mSoundUri)
				.setOnlyAlertOnce(false)
				.setContentIntent(pendingIntent)
				.setDefaults(android.app.Notification.DEFAULT_LIGHTS | android.app.Notification.DEFAULT_VIBRATE)
				.setWhen(System.currentTimeMillis());

		/* Large icon */
		if (notification.photo != null) {
			String photoUri = notification.photo.getUri();

			try {
				@SuppressWarnings("SuspiciousNameCombination")
				Bitmap bitmap = DownloadManager.with(Patchr.applicationContext)
				                               .load(photoUri)
				                               .centerCrop()
				                               .resizeDimen(R.dimen.notification_large_icon_width, R.dimen.notification_large_icon_width)
				                               .get();
				DownloadManager.logBitmap(this, bitmap);

				builder.setLargeIcon(bitmap);
			}
			catch (IOException e) {
				Reporting.logException(e);
			}
		}

		/* Ticker */
		if (notification.ticker != null) {
			builder.setTicker(Html.fromHtml(notification.ticker));
		}

		/* Big photo or text - photo trumps text */
		if (notification.photoBig != null) {
			useBigPicture(builder, notification);
		}
		else if (notification.description != null) {
			useBigText(builder, notification);
		}
		else {
			mNotificationManager.notify(Constants.SCHEMA_ENTITY_NOTIFICATION, 0, builder.build());
		}
	}

	public void useBigPicture(final NotificationCompat.Builder builder, final Notification notification) {

		final String imageUri = notification.photoBig.getUri();

		try {
			Bitmap bitmap = DownloadManager.with(Patchr.applicationContext)
			                               .load(imageUri)
			                               .centerCrop()
			                               .resizeDimen(R.dimen.notification_big_picture_width, R.dimen.notification_big_picture_height)
			                               .get();
			DownloadManager.logBitmap(this, bitmap);
			NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.setBigContentTitle(notification.name)
					.setSummaryText(Html.fromHtml(notification.subtitle));

			builder.setStyle(style);
			mNotificationManager.notify(Constants.SCHEMA_ENTITY_NOTIFICATION, 0, builder.build());
		}
		catch (IOException e) {
			Reporting.logException(e);
		}
	}

	public void useBigText(NotificationCompat.Builder builder, Notification notification) {
		NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
				.setBigContentTitle(notification.name)
				.bigText(Html.fromHtml(notification.description))
				.setSummaryText(Html.fromHtml(notification.subtitle));

		builder.setStyle(style);

		mNotificationManager.notify(Constants.SCHEMA_ENTITY_NOTIFICATION, 0, builder.build());
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

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Install getInstall() {
		return mInstall;
	}

	public void setInstall(Install device) {
		mInstall = device;
	}

	public Integer getNewNotificationCount() {
		return mNewNotificationCount;
	}

	public void setNewNotificationCount(Integer newNotificationCount) {
		mNewNotificationCount = newNotificationCount;
	}

	public Map<String, Notification> getNotifications() {
		return mNotifications;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class Tag {
		public static String INSERT       = "insert";
		public static String SHARE        = "share";
		public static String NOTIFICATION = "alert";
	}
}
