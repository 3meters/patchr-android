package com.patchr.components;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.EventCategory;
import com.patchr.objects.enums.PhotoCategory;
import com.patchr.ui.MainScreen;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Utils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ucd")
public class NotificationManager {

	public static android.app.NotificationManager mNotificationService;

	private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";
	private Uri mSoundUri;
	private Integer                  mNewNotificationCount = 0;
	private Map<String, RealmEntity> mNotifications        = new HashMap<>();

	private NotificationManager() {
		mNotificationService = (android.app.NotificationManager) Patchr.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mSoundUri = Uri.parse("android.resource://" + Patchr.applicationContext.getPackageName() + "/" + R.raw.notification_activity);
	}

	private static class NotificationManagerHolder {
		public static final NotificationManager instance = new NotificationManager();
	}

	public static NotificationManager getInstance() {
		return NotificationManagerHolder.instance;
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	public void broadcastNotification(final RealmEntity notification) {
		Dispatcher.getInstance().post(new NotificationReceivedEvent(notification));
	}

	public void statusNotification(final RealmEntity notification, Context context) {
	    /*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (notification.trigger != null && notification.event != null) {
			PreferenceManager preferenceManager = new PreferenceManager();
			if (!preferenceManager.notificationEnabled(notification.getTriggerCategory(), notification.getEventCategory())) {
				return;
			}
		}

		notification.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notification.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent pendingIntent = TaskStackBuilder
			.create(Patchr.applicationContext)
			.addNextIntent(new Intent(Patchr.applicationContext, MainScreen.class))
			.addNextIntent(notification.intent)
			.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Default base notification configuration */

		PendingIntent deleteIntent = PendingIntent.getBroadcast(Patchr.applicationContext
			, 0
			, new Intent(NOTIFICATION_DELETED_ACTION)
			, 0);

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Patchr.applicationContext)
			.setContentTitle(StringManager.getString(R.string.name_app))
			.setContentText(Utils.fromHtml(notification.summary))
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
		if (notification.getPhoto() != null) {
			String url = notification.getPhoto().uri(PhotoCategory.PROFILE);

			try {
				@SuppressWarnings("SuspiciousNameCombination")
				Bitmap bitmap = Picasso.with(Patchr.applicationContext)
					.load(url)
					.centerCrop()
					.resizeDimen(R.dimen.notification_tray_large_icon_width, R.dimen.notification_tray_large_icon_width)
					.get();

				builder.setLargeIcon(bitmap);
			}
			catch (ConnectException e) {
				Reporting.breadcrumb("Picasso failed to load bitmap: connect");
			}
			catch (IOException e) {
				Reporting.breadcrumb("Picasso failed to load bitmap: io");
			}
		}

		/* Ticker */
		if (notification.ticker != null) {
			builder.setTicker(Utils.fromHtml(notification.ticker));
		}

		/* Big photo or text - photo trumps text */
		if (notification.getPhotoBig() != null) {
			useBigPicture(builder, notification);
		}
		else if (notification.description != null) {
			useBigText(builder, notification);
		}
		else {
			String tag = notification.getEventCategory().equals(EventCategory.LIKE) ? Tag.LIKE : Tag.NOTIFICATION;
			mNotificationService.notify(tag, 0, builder.build());
		}
	}

	public void useBigPicture(final NotificationCompat.Builder builder, final RealmEntity notification) {

		final String url = notification.getPhotoBig().uri(PhotoCategory.STANDARD);

		try {
			Bitmap bitmap = Picasso.with(Patchr.applicationContext)
				.load(url)
				.centerCrop()
				.resizeDimen(R.dimen.notification_tray_big_picture_width, R.dimen.notification_tray_big_picture_height)
				.get();

			NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
				.bigPicture(bitmap)
				.setBigContentTitle(notification.name)
				.setSummaryText(Utils.fromHtml(notification.subtitle));

			builder.setStyle(style);
			String tag = notification.getEventCategory().equals(EventCategory.LIKE) ? Tag.LIKE : Tag.NOTIFICATION;
			mNotificationService.notify(tag, 0, builder.build());
		}
		catch (ConnectException e) {
			Reporting.breadcrumb("Picasso failed to load bitmap: connect");
		}
		catch (IOException e) {
			Reporting.breadcrumb("Picasso failed to load bitmap: io");
		}
	}

	public void useBigText(NotificationCompat.Builder builder, RealmEntity notification) {

		NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
			.setBigContentTitle(notification.name)
			.bigText(Utils.fromHtml(notification.description))
			.setSummaryText(Utils.fromHtml(notification.subtitle));

		builder.setStyle(style);

		String tag = notification.getEventCategory().equals(EventCategory.LIKE) ? Tag.LIKE : Tag.NOTIFICATION;
		mNotificationService.notify(tag, 0, builder.build());
	}

	public void cancelNotification(String tag) {
		mNotificationService.cancel(tag, 0);
	}

	public void cancelAllNotifications() {
		mNotificationService.cancelAll();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Integer getNewNotificationCount() {
		return mNewNotificationCount;
	}

	public void setNewNotificationCount(Integer newNotificationCount) {
		mNewNotificationCount = newNotificationCount;
	}

	public Map<String, RealmEntity> getNotifications() {
		return mNotifications;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class Tag {
		public static String INSERT       = "insert";
		public static String SHARE        = "share";
		public static String NOTIFICATION = "alert";
		public static String LIKE         = "like";
	}
}
