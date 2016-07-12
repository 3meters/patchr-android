package com.patchr.events;

import com.patchr.Constants;
import com.patchr.objects.enums.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.enums.LinkSpecType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ucd")
public class EntitiesQueryEvent extends AbsEntitiesQueryEvent {

	public static EntitiesQueryEvent build(ActionType actionType, Map where, String direction, String linkType, String toSchema, String entityId) {

		Cursor cursor = new Cursor();
		cursor.where = where;
		cursor.direction = direction;

		List<String> linkTypes = new ArrayList<>();
		linkTypes.add(linkType);
		cursor.linkTypes = linkTypes;

		List<String> toSchemas = new ArrayList<>();
		toSchemas.add(toSchema);
		cursor.schemas = toSchemas;

		Integer linkProfile = LinkSpecType.LINKS_FOR_PATCH;
		if (toSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			linkProfile = LinkSpecType.LINKS_FOR_MESSAGE;
		}
		else if (toSchema.equals(Constants.SCHEMA_ENTITY_USER)) {
			linkProfile = LinkSpecType.LINKS_FOR_USER_CURRENT;
		}

		EntitiesQueryEvent request = new EntitiesQueryEvent();

		request.cursor = cursor;
		request.actionType = actionType;
		request.linkProfile = linkProfile;
		request.entityId = entityId;

		return request;
	}
}
