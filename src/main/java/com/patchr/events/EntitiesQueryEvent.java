package com.patchr.events;

import com.patchr.Patchr;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ucd")
public class EntitiesQueryEvent extends AbsEntitiesQueryEvent {

	public static EntitiesQueryEvent build(ActionType actionType, int pageSize, Map where, String direction, String linkType, String toSchema, String entityId) {

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

		IEntityController controller = Patchr.getInstance().getControllerForSchema(toSchema);
		Integer linkProfile = controller.getLinkProfile();

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
