package com.aircandi.catalina.components;

import android.text.TextUtils;

import com.aircandi.catalina.Constants;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.Logger;
import com.aircandi.objects.Action.EventCategory;
import com.aircandi.objects.ActivityBase;
import com.aircandi.objects.ActivityBase.TriggerType;
import com.aircandi.objects.Photo;

public class ActivityDecorator extends com.aircandi.components.ActivityDecorator {

	/*
	 * Own
	 * 
	 * [George Snelling] commented on your [candigram] at [Taco Del Mar].
	 * [George Snelling] commented on your [picture] at [Taco Del Mar].
	 * [George Snelling] commented on your [place] [Taco Del Mar].
	 * 
	 * [George Snelling] added a [picture] to your [place] [Taco Del Mar].
	 * [George Snelling] added a [picture] to your [candigram] at [Taco Del Mar].
	 * [George Snelling] added a [candigram] to your [place] [Taco Del Mar].
	 * 
	 * [George Snelling] kicked a [candigram] to your [place] [Taco Del Mar].
	 * 
	 * Watching
	 * 
	 * [George Snelling] commented on a [candigram] you are watching.
	 * [George Snelling] commented on a [picture] you are watching.
	 * [George Snelling] commented on a [place] you are watching.
	 * 
	 * [George Snelling] added a [picture] to a [place] you are watching.
	 * [George Snelling] added a [picture] to a [candigram] you are watching.
	 * [George Snelling] added a [candigram] to a [place] you are watching.
	 * 
	 * [George Snelling] kicked a [candigram] to a [place] you are watching.
	 * 
	 * Nearby
	 * 
	 * [George Snelling] commented on a [candigram] nearby.
	 * [George Snelling] commented on a [picture] nearby.
	 * [George Snelling] commented on a [place] nearby.
	 * 
	 * [George Snelling] added a [picture] to a [place] nearby.
	 * [George Snelling] added a [picture] to a [candigram] nearby.
	 * [George Snelling] added a [candigram] to a [place] nearby.
	 * 
	 * [George Snelling] kicked a [candigram] to a [place] nearby.
	 * 
	 * Move
	 * 
	 * A candigram has traveled to a place nearby
	 * A candigram has traveled to your place Taco Del Mar.
	 * A candigram has traveled to a place you are watching.
	 */

	@Override
	public String subtitle(ActivityBase activity) {

		if (activity.action.getEventCategory().equals(EventCategory.INSERT)) {

			if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {

				if (activity.action.entity.type.equals(MessageType.ROOT)) {
					if (activity.trigger.equals(TriggerType.NEARBY))
						return String.format("sent a message to a %1$s nearby.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(TriggerType.WATCH_TO))
						return String.format("sent a message to a %1$s you are watching.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(TriggerType.WATCH_USER)
							|| activity.trigger.equals(TriggerType.NONE))
						return String.format("sent a message.", activity.action.toEntity.getSchemaMapped());
					else if (activity.trigger.equals(TriggerType.OWN_TO))
						return String.format("sent a message to a %1$s of yours.", activity.action.toEntity.getLabelForSchema());
				}
				else if (activity.action.entity.type.equals(MessageType.REPLY)) {

					if (activity.trigger.equals(TriggerType.NEARBY))
						return String.format("replied to a message at %1$s nearby.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(TriggerType.WATCH_TO))
						return String.format("replied to a message at a %1$s you are watching.", activity.action.toEntity.getLabelForSchema());
					else if (activity.trigger.equals(TriggerType.WATCH_USER)
							|| activity.trigger.equals(TriggerType.NONE))
						return String.format("replied to a message.", activity.action.toEntity.getSchemaMapped());
					else if (activity.trigger.equals(TriggerType.OWN_TO))
						return String.format("replied to a message at a %1$s of yours.", activity.action.toEntity.getLabelForSchema());
				}
			}
		}
		Logger.w(ActivityDecorator.class, "activity missing subtitle");
		return null;
	}

	@Override
	public String title(ActivityBase activity) {
		if (activity.action.user == null)
			return activity.action.entity.name;
		else
			return activity.action.user.name;
	}

	@Override
	public Photo photoBy(ActivityBase activity) {
		Photo photo = null;
		if (activity.action.user == null) {
			photo = activity.action.user.getDefaultPhoto();
		}
		else {
			photo = activity.action.user.getPhoto();
			photo.name = activity.action.user.name;
			photo.shortcut = activity.action.user.getShortcut();
		}
		return photo;
	}

	@Override
	public Photo photoOne(ActivityBase activity) {
		Photo photo = null;

		photo = activity.action.entity.getPhoto();
		photo.name = TextUtils.isEmpty(activity.action.entity.name)
				? activity.action.entity.getSchemaMapped()
				: activity.action.entity.name;
		photo.shortcut = activity.action.entity.getShortcut();
		return photo;
	}
}
