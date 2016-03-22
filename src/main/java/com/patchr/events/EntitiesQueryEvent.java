package com.patchr.events;

import com.patchr.Constants;
import com.patchr.objects.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;
import com.patchr.objects.LinkSpecType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ucd")
public class EntitiesQueryEvent extends AbsEntitiesQueryEvent {

	public static EntitiesQueryEvent build(ActionType actionType, Map where, String direction, String linkType, String toSchema, String entityId) {

		Integer pageSize = Constants.PAGE_SIZE;
		Integer skipCount = ((int) Math.ceil((double) 0 / pageSize) * pageSize);
		Cursor cursor = new Cursor()
				.setLimit(pageSize)
				.setSkip(skipCount)
				.setWhere(where)
				.setDirection(direction);

		List<String> linkTypes = new ArrayList<>();
		linkTypes.add(linkType);
		cursor.setLinkTypes(linkTypes);

		List<String> toSchemas = new ArrayList<>();
		toSchemas.add(toSchema);
		cursor.setToSchemas(toSchemas);

		Integer linkProfile = LinkSpecType.LINKS_FOR_PATCH;
		if (toSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			linkProfile = LinkSpecType.LINKS_FOR_MESSAGE;
		}
		else if (toSchema.equals(Constants.SCHEMA_ENTITY_USER)) {
			linkProfile = LinkSpecType.LINKS_FOR_USER_CURRENT;
		}

		EntitiesQueryEvent request = new EntitiesQueryEvent();
		request.setCursor(cursor)
				.setLinkProfile(linkProfile)
				.setPageSize(pageSize)
				.setActionType(actionType)
				.setFetchMode(FetchMode.MANUAL)
				.setEntityId(entityId);

		return request;
	}
}
