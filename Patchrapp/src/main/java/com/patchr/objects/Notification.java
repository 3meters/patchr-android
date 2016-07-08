package com.patchr.objects;

import android.content.Intent;

import java.io.Serializable;
import java.util.Map;

public class Notification extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = 4362288672244719348L;

	/*--------------------------------------------------------------------------------------------
	 * Service fields
	 *--------------------------------------------------------------------------------------------*/

	public String   targetId;
	public String   parentId;
	public String   userId;
	public Number   sentDate;
	public String   trigger;
	public String   event;
	public String   ticker;
	public Number   priority;
	public PhotoOld photoBig;
	public String   summary;

	/* client only */
	public Intent intent;
	public Boolean read = false;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public String getTriggerCategory() {
		if (this.trigger.contains("nearby")) return TriggerCategory.NEARBY;
		if (this.trigger.contains("watch")) return TriggerCategory.WATCH;
		if (this.trigger.contains("own")) return TriggerCategory.OWN;
		return TriggerCategory.NONE;
	}

	public String getEventCategory() {
		if (this.event.contains("share")) return EventCategory.SHARE;
		if (this.event.contains("insert")) return EventCategory.INSERT;
		if (this.event.contains("watch")) return EventCategory.WATCH;
		if (this.event.contains("like")) return EventCategory.LIKE;
		return EventCategory.NONE;
	}

	public static Notification setPropertiesFromMap(Notification entity, Map map) {
	    /*
	     * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Notification) Entity.setPropertiesFromMap(entity, map);
			entity.targetId = (String) (map.get("targetId") != null ? map.get("targetId") : map.get("_target"));
			entity.parentId = (String) (map.get("parentId") != null ? map.get("parentId") : map.get("_parent"));
			entity.userId = (String) (map.get("userId") != null ? map.get("userId") : map.get("_user"));
			entity.sentDate = (Number) map.get("sentDate");
			entity.priority = (Number) map.get("priority");
			entity.trigger = (String) map.get("trigger");
			entity.summary = (String) map.get("summary");
			entity.event = (String) map.get("event");
			entity.ticker = (String) map.get("ticker");

			if (map.get("photoBig") != null) {
				entity.photoBig = PhotoOld.setPropertiesFromMap(new PhotoOld(), (Map<String, Object>) map.get("photoBig"));
			}
		}
		return entity;
	}

	@Override public Notification clone() {
		final Notification clone = (Notification) super.clone();
		return clone;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class Priority {
		public static Integer ONE   = 1;    // All bells and whistles for in-app notifications
		public static Integer TWO   = 2;    // Mute chirping/toast for in-app notifications
		public static Integer THREE = 3;
	}

	public static class NotificationType {
		/*
		 * Used to determine icon to display in notification ui.
		 */
		public static String MESSAGE = "message";
		public static String MEDIA   = "media";
		public static String PLACE   = "patch";
		public static String SHARE   = "share";
		public static String WATCH   = "watch";
		public static String LIKE    = "like";
	}

	public static class TriggerCategory {
		/*
		 * Used to characterize why the current user is receiving the notification.
		 * Used to enable/disable status notifications based on user preference settings.
		 */
		public static String NEARBY = "nearby";         // sent because this user is nearby
		public static String WATCH  = "watch";          // sent because this user is watching the target entity
		public static String OWN    = "own";            // sent because this user is the owner of the target entity
		public static String NONE   = "none";
	}

	public static class EventCategory {
		/*
		 * Used to characterize the action associated with the notification.
		 * Used to enable/disable status notifications based on user preference settings.
		 */
		public static String INSERT = "insert";         // notification about a patch|message insert
		public static String SHARE  = "share";          // notification about patch|message|photo share
		public static String LIKE   = "like";           // notification about patch|message like
		public static String WATCH  = "watch";          // notification about patch watch
		public static String NONE   = "none";
	}
}