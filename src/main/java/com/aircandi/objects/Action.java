package com.aircandi.objects;

import com.aircandi.Patchr;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Action extends ServiceObject implements Cloneable, Serializable {

	private static final long   serialVersionUID = 4362288672244719448L;
	public static final  String schemaName       = "action";
	public static final  String schemaId         = "ac";

	@Expose
	public String event;                                        // insert_entity_picture_to_place, etc
	@Expose
	public User   user;                                        // can be null
	@Expose
	public Entity entity;
	@Expose
	public Entity toEntity;
	@Expose
	public Entity fromEntity;

	public static Action setPropertiesFromMap(Action action, Map map, Boolean nameMapping) {
	    /*
	     * Properties involved with editing are copied from one entity to another.
		 */
		action.event = (String) map.get("event");

		if (map.get("entity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("entity");
			String schema = (String) entityMap.get("schema");
			IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
			if (controller != null) {
				action.entity = controller.makeFromMap(entityMap, nameMapping);
			}
		}

		if (map.get("toEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("toEntity");
			String schema = (String) entityMap.get("schema");
			IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
			if (controller != null) {
				action.toEntity = controller.makeFromMap(entityMap, nameMapping);
			}
		}

		if (map.get("fromEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("fromEntity");
			String schema = (String) entityMap.get("schema");
			IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
			if (controller != null) {
				action.fromEntity = controller.makeFromMap(entityMap, nameMapping);
			}
		}

		if (map.get("user") != null) {
			action.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return action;
	}

	public String getEventCategory() {
		/*
		 * event can contain multiple candidates: i.e. insert_message_share
		 */
		if (this.event.contains("share")) return EventCategory.SHARE;
		if (this.event.contains("watch")) return EventCategory.WATCH;
		if (this.event.contains("like")) return EventCategory.LIKE;
		if (this.event.contains("join")) return EventCategory.JOIN;
		if (this.event.contains("insert")) return EventCategory.INSERT;
		return EventCategory.UNKNOWN;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class EventCategory {
		public static String INSERT  = "insert";
		public static String SHARE   = "share";
		public static String WATCH   = "watch";
		public static String LIKE    = "like";
		public static String JOIN    = "join";
		public static String UNKNOWN = "unknown";
	}
}