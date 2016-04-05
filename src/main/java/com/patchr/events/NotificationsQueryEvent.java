package com.patchr.events;

import com.patchr.Constants;
import com.patchr.objects.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.LinkSpecType;

public class NotificationsQueryEvent extends AbsEntitiesQueryEvent {

	public static NotificationsQueryEvent build(ActionType actionType, String entityId) {

		Integer skipCount = ((int) Math.ceil((double) 0 / Constants.PAGE_SIZE) * Constants.PAGE_SIZE);
		Cursor cursor = new Cursor().setSkip(skipCount);

		NotificationsQueryEvent request = new NotificationsQueryEvent();
		request.cursor = cursor;
		request.actionType = actionType;
		request.linkProfile = LinkSpecType.NO_LINKS;
		request.entityId = entityId;

		return request;
	}
}
