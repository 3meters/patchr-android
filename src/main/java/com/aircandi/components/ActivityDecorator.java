package com.aircandi.components;

import android.text.TextUtils;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Action.EventCategory;
import com.aircandi.objects.MessageTriggers;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.objects.Message;
import com.aircandi.objects.Photo;

public class ActivityDecorator {

	public void decorate(ServiceMessage activity) {
		activity.title = title(activity);
		activity.subtitle = subtitle(activity);
		if (activity.subtitle == null) {
			Logger.v(null, "missing subtitle");
		}
		activity.description = description(activity);
		activity.photoBy = photoBy(activity);
		activity.photoOne = photoOne(activity);
	}

	public String subtitle(ServiceMessage activity) {

		if (activity.action.getEventCategory().equals(EventCategory.INSERT)) {

			if (activity.action.entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {

				if (activity.action.entity.type.equals(Message.MessageType.ROOT)) {
					if (activity.trigger.equals(MessageTriggers.TriggerType.NEARBY))
						return String.format(StringManager.getString(R.string.label_notification_message_nearby_subtitle), activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggers.TriggerType.WATCH_TO))
						return String.format(StringManager.getString(R.string.label_notification_message_watchto_subtitle), activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggers.TriggerType.OWN_TO))
						return String.format(StringManager.getString(R.string.label_notification_message_ownto_subtitle), activity.action.toEntity.getLabelForSchema());
				}
				else if (activity.action.entity.type.equals(Message.MessageType.REPLY)) {

					if (activity.trigger.equals(MessageTriggers.TriggerType.NEARBY))
						return String.format(StringManager.getString(R.string.label_notification_message_reply_nearby_subtitle), activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggers.TriggerType.WATCH_TO))
						return String.format(StringManager.getString(R.string.label_notification_message_reply_watchto_subtitle), activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggers.TriggerType.OWN_TO))
						return String.format(StringManager.getString(R.string.label_notification_message_reply_ownto_subtitle), activity.action.toEntity.getLabelForSchema());
				}
			}
			else if (activity.action.entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_PLACE)) {

				if (activity.trigger.equals(MessageTriggers.TriggerType.NEARBY))
					return StringManager.getString(R.string.label_notification_place_nearby_subtitle);
			}
		}
		else if (activity.action.getEventCategory().equals(EventCategory.SHARE)) {

			if (activity.action.entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
				return StringManager.getString(R.string.label_notification_share_subtitle);
			}
		}
		else if (activity.action.getEventCategory().equals(EventCategory.WATCH)) {
			if (activity.action.toEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				return String.format(StringManager.getString(R.string.label_notification_watch_subtitle), activity.action.toEntity.name);
			}
		}

		Logger.w(ActivityDecorator.class, "activity missing subtitle");
		return null;
	}

	public String title(ServiceMessage activity) {
		if (activity.action.user == null)
			return activity.action.entity.name;
		else
			return activity.action.user.name;
	}

	public Photo photoBy(ServiceMessage activity) {
		Photo photo;
		if (activity.action.user == null) {
			IEntityController controller = Patchr.getInstance().getControllerForSchema(com.aircandi.Constants.SCHEMA_ENTITY_USER);
			photo = ((controller != null) ? controller.getDefaultPhoto(null) : null);
		}
		else {
			photo = activity.action.user.getPhoto();
			photo.name = activity.action.user.name;
			photo.shortcut = activity.action.user.getShortcut();
		}
		return photo;
	}

	public Photo photoOne(ServiceMessage activity) {
		Photo photo;

		photo = activity.action.entity.getPhoto();
		photo.name = TextUtils.isEmpty(activity.action.entity.name)
		             ? activity.action.entity.schema
		             : activity.action.entity.name;
		photo.shortcut = activity.action.entity.getShortcut();
		return photo;
	}

	public static String description(ServiceMessage activity) {
		return activity.action.entity.description;
	}
}
