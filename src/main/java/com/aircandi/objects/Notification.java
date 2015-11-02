package com.aircandi.objects;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Notification extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = 4362288672244719348L;
	public static final  String collectionId     = "notifications";

	/*--------------------------------------------------------------------------------------------
	 * Service fields
	 *--------------------------------------------------------------------------------------------*/

	@Expose
	public String targetId;
	@Expose
	public String parentId;
	@Expose
	public String userId;
	@Expose(serialize = false, deserialize = true)
	public Number sentDate;
	@Expose(serialize = false, deserialize = true)
	public String trigger;
	@Expose(serialize = false, deserialize = true)
	public String event;
	@Expose(serialize = false, deserialize = true)
	public String ticker;
	@Expose(serialize = false, deserialize = true)
	public Number priority;
	@Expose(serialize = false, deserialize = true)
	public Photo  photoBig;
	@Expose(serialize = false, deserialize = true)
	public String summary;

	/* client only */
	public Intent intent;
	public Photo  photoType;
	public Boolean read = false;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public String getTriggerCategory() {
		if (this.trigger.contains("nearby")) return TriggerCategory.NEARBY;
		if (this.trigger.contains("watch")) return TriggerCategory.WATCH;
		if (this.trigger.contains("own")) return TriggerCategory.OWN;
		return TriggerCategory.NONE;
	}

	@NonNull
	public String getEventCategory() {
		if (this.event.contains("share")) return EventCategory.SHARE;
		if (this.event.contains("insert")) return EventCategory.INSERT;
		if (this.event.contains("watch")) return EventCategory.WATCH;
		if (this.event.contains("like")) return EventCategory.LIKE;
		return EventCategory.NONE;
	}

	public static Notification setPropertiesFromMap(Notification entity, Map map, Boolean nameMapping) {
	    /*
	     * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Notification) Entity.setPropertiesFromMap(entity, map, nameMapping);
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
				entity.photoBig = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photoBig"), nameMapping);
			}
		}
		return entity;
	}

	@Override
	public Notification clone() {
		final Notification clone = (Notification) super.clone();
		return clone;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/


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