package com.aircandi.components;

import android.text.TextUtils;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Action.EventCategory;
import com.aircandi.objects.MessageTriggerType;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.objects.Message;
import com.aircandi.objects.Photo;

public class ActivityDecorator {

	/*
	 * Own
	 * 
	 * [George Snelling] commented on your [picture] at [Taco Del Mar].
	 * [George Snelling] commented on your [place] [Taco Del Mar].
	 * 
	 * [George Snelling] added a [picture] to your [place] [Taco Del Mar].
	 * 
	 * Watching
	 * 
	 * [George Snelling] commented on a [picture] you are watching.
	 * [George Snelling] commented on a [place] you are watching.
	 * 
	 * [George Snelling] added a [picture] to a [place] you are watching.
	 * 
	 * Nearby
	 * 
	 * [George Snelling] commented on a [picture] nearby.
	 * [George Snelling] commented on a [place] nearby.
	 * 
	 * [George Snelling] added a [picture] to a [place] nearby.
	 */

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
					if (activity.trigger.equals(MessageTriggerType.TriggerType.NEARBY))
						return String.format("sent a message to a %1$s nearby.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggerType.TriggerType.WATCH_TO))
						return String.format("sent a message to a %1$s you are watching.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggerType.TriggerType.WATCH_USER)
							|| activity.trigger.equals(MessageTriggerType.TriggerType.NONE))
						return "sent a message.";
					else if (activity.trigger.equals(MessageTriggerType.TriggerType.OWN_TO))
						return String.format("sent a message to a %1$s of yours.", activity.action.toEntity.getLabelForSchema());
				}
				else if (activity.action.entity.type.equals(Message.MessageType.REPLY)) {

					if (activity.trigger.equals(MessageTriggerType.TriggerType.NEARBY))
						return String.format("replied to a message at %1$s nearby.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggerType.TriggerType.WATCH_TO))
						return String.format("replied to a message at a %1$s you are watching.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(MessageTriggerType.TriggerType.WATCH_USER)
							|| activity.trigger.equals(MessageTriggerType.TriggerType.NONE))
						return "replied to a message.";
					else if (activity.trigger.equals(MessageTriggerType.TriggerType.OWN_TO))
						return String.format("replied to a message at a %1$s of yours.", activity.action.toEntity.getLabelForSchema());
				}
				else if (activity.action.entity.type.equals(Message.MessageType.REPLY)) {
				}
			}
			else if (activity.action.entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_PLACE)) {

				if (activity.trigger.equals(MessageTriggerType.TriggerType.NEARBY))
					return "dropped a new patch nearby.";
			}
		}
		else if (activity.action.getEventCategory().equals(EventCategory.SHARE)) {

			if (activity.action.entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
				return "shared with you.";
			}
		}
		else if (activity.action.getEventCategory().equals(EventCategory.WATCH)) {
			if (activity.action.toEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				return String.format("started watching a patch of yours: %1$s", activity.action.toEntity.name);
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
			IEntityController controller = Patch.getInstance().getControllerForSchema(com.aircandi.Constants.SCHEMA_ENTITY_USER);
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
