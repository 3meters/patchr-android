package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.objects.Notification;
import com.patchr.objects.Photo;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class NotificationView extends FrameLayout {

	private static final Object lock = new Object();

	public    Entity   entity;
	protected BaseView base;
	protected Integer  layoutResId;

	protected ViewGroup   layout;
	protected ImageLayout userPhoto;
	protected ImageLayout notificationPhoto;
	protected TextView    modifiedDate;
	protected TextView    summary;
	protected ImageView   notificationType;
	protected ImageView   recencyIndicator;

	public NotificationView(Context context) {
		this(context, null, 0);
	}

	public NotificationView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NotificationView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.notification_view;
		initialize();
	}

	public NotificationView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {
		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.userPhoto = (ImageLayout) layout.findViewById(R.id.user_photo);
		this.notificationPhoto = (ImageLayout) layout.findViewById(R.id.notification_photo);
		this.modifiedDate = (TextView) layout.findViewById(R.id.modified_date);
		this.summary = (TextView) layout.findViewById(R.id.summary);
		this.notificationType = (ImageView) layout.findViewById(R.id.notification_type);
		this.recencyIndicator = (ImageView) layout.findViewById(R.id.recency_indicator);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(Entity entity) {

		synchronized (lock) {

			this.entity = entity;
			Notification notification = (Notification) entity;

			/* User */
			if (notification.photo != null) {
				this.userPhoto.setImageWithPhoto(notification.photo, null);
			}
			else if (notification.name != null) {
				this.userPhoto.setImageWithText(notification.name, true);
			}

			/* Create date */
			String dateFormatted = null;
			if (notification.modifiedDate != null) {
				dateFormatted = DateTime.intervalCompact(notification.modifiedDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			}

			base.setOrGone(this.modifiedDate, dateFormatted);
			base.setOrGone(this.summary, notification.summary);

	        /* Photo */
			UI.setVisibility(this.notificationPhoto, GONE);
			if (notification.photoBig != null) {
				final Photo photo = notification.photoBig;
				this.notificationPhoto.setImageWithPhoto(photo, null);
				this.notificationPhoto.setTag(photo);
				UI.setVisibility(this.notificationPhoto, VISIBLE);
			}

			/* Notification type */
			if (notification.type != null) {
				UI.setVisibility(this.notificationType, View.GONE);
				Integer drawableResId = null;
				if (notification.type.equals(Notification.NotificationType.WATCH)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconWatch);
				}
				else if (notification.type.equals(Notification.NotificationType.PLACE)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconPatch);
				}
				else if (notification.type.equals(Notification.NotificationType.MESSAGE)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconMessage);
				}
				else if (notification.type.equals(Notification.NotificationType.MEDIA)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconMediaMessage);
				}
				else if (notification.type.equals(Notification.NotificationType.SHARE)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconShare);
				}
				else if (notification.type.equals(Notification.NotificationType.LIKE)) {
					if (notification.event.equals("like_entity_patch")) {
						drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconFavorite);
					}
					else if (notification.event.equals("like_entity_message")) {
						drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconLike);
					}
				}
				if (drawableResId != null) {
					this.notificationType.setImageResource(drawableResId);
					UI.setVisibility(this.notificationType, View.VISIBLE);
				}
			}
		}
	}
}
