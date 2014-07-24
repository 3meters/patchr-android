package com.aircandi.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.aircandi.Aircandi;
import com.aircandi.controllers.IEntityController;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Action extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				event;										// insert_entity_picture_to_place, etc
	@Expose
	public User					user;										// can be null
	@Expose
	public Entity				entity;
	@Expose
	public Entity				toEntity;
	@Expose
	public Entity				fromEntity;

	

	public static Action setPropertiesFromMap(Action action, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		action.event = (String) map.get("event");

		if (map.get("entity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("entity");
			String schema = (String) entityMap.get("schema");
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			if (controller != null && action != null) {
				action.entity = controller.makeFromMap(entityMap, nameMapping);			
			}
		}

		if (map.get("toEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("toEntity");
			String schema = (String) entityMap.get("schema");
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			action.toEntity = controller.makeFromMap(entityMap, nameMapping);			
		}

		if (map.get("fromEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("fromEntity");
			String schema = (String) entityMap.get("schema");
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			action.fromEntity = controller.makeFromMap(entityMap, nameMapping);			
		}

		if (map.get("user") != null) {
			action.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return action;
	}

	public String getEventCategory() {
		if (this.event.contains("insert")) return EventCategory.INSERT;
		if (this.event.contains("update")) return EventCategory.UPDATE;
		if (this.event.contains("delete")) return EventCategory.DELETE;
		if (this.event.contains("watch")) return EventCategory.WATCH;
		if (this.event.contains("move")) return EventCategory.MOVE;
		if (this.event.contains("forward")) return EventCategory.FORWARD;
		return EventCategory.UNKNOWN;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class EventCategory {
		public static String	INSERT	= "insert";
		public static String	DELETE	= "delete";
		public static String	UPDATE	= "update";
		public static String	WATCH	= "watch";
		public static String	MOVE	= "move";
		public static String	FORWARD	= "forward";
		public static String	UNKNOWN	= "unknown";
	}
}