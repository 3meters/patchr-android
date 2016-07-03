package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.model.RealmPhoto;
import com.patchr.objects.Notification;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class NotificationView extends BaseView {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	protected Integer     layoutResId;

	protected ViewGroup   layout;
	protected ImageWidget userPhoto;
	protected ImageWidget notificationPhoto;
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
		this.layoutResId = R.layout.view_notification;
		initialize();
	}

	public NotificationView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.userPhoto = (ImageWidget) layout.findViewById(R.id.user_photo);
		this.notificationPhoto = (ImageWidget) layout.findViewById(R.id.notification_photo);
		this.modifiedDate = (TextView) layout.findViewById(R.id.modified_date);
		this.summary = (TextView) layout.findViewById(R.id.summary);
		this.notificationType = (ImageView) layout.findViewById(R.id.notification_type);
		this.recencyIndicator = (ImageView) layout.findViewById(R.id.recency_indicator);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(RealmEntity entity) {

		synchronized (lock) {

			this.entity = entity;

			/* User */
			this.userPhoto.setImageWithRealmEntity(entity.photo, entity.name);

			/* Create date */
			String dateFormatted = null;
			if (entity.modifiedDate != null) {
				dateFormatted = DateTime.intervalCompact(entity.modifiedDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			}

			setOrGone(this.modifiedDate, dateFormatted);
			setOrGone(this.summary, entity.summary);

	        /* Photo */
			UI.setVisibility(this.notificationPhoto, GONE);
			if (entity.photoBig != null) {
				final RealmPhoto photo = entity.photoBig;
				this.notificationPhoto.setImageWithRealmEntity(photo, null);
				this.notificationPhoto.setTag(photo);
				UI.setVisibility(this.notificationPhoto, VISIBLE);
			}

			/* Notification type */
			if (entity.type != null) {
				UI.setVisibility(this.notificationType, View.GONE);
				Integer drawableResId = null;
				if (entity.type.equals(Notification.NotificationType.WATCH)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconWatch);
				}
				else if (entity.type.equals(Notification.NotificationType.PLACE)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconPatch);
				}
				else if (entity.type.equals(Notification.NotificationType.MESSAGE)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconMessage);
				}
				else if (entity.type.equals(Notification.NotificationType.MEDIA)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconMediaMessage);
				}
				else if (entity.type.equals(Notification.NotificationType.SHARE)) {
					drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconShare);
				}
				else if (entity.type.equals(Notification.NotificationType.LIKE)) {
					if (entity.event.equals("like_entity_patch")) {
						drawableResId = UI.getResIdForAttribute(getContext(), R.attr.iconFavorite);
					}
					else if (entity.event.equals("like_entity_message")) {
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
