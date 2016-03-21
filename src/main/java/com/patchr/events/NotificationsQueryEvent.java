package com.patchr.events;

import com.patchr.Constants;
import com.patchr.objects.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;
import com.patchr.objects.LinkSpecType;

public class NotificationsQueryEvent extends AbsEntitiesQueryEvent {

	public static NotificationsQueryEvent build(ActionType actionType, String entityId) {

		Integer pageSize = Constants.PAGE_SIZE;
		Integer skipCount = ((int) Math.ceil((double) 0 / pageSize) * pageSize);
		Cursor cursor = new Cursor()
				.setLimit(pageSize)
				.setSkip(skipCount);

		NotificationsQueryEvent request = new NotificationsQueryEvent();
		request.setCursor(cursor)
				.setActionType(actionType)
				.setLinkProfile(LinkSpecType.NO_LINKS)
				.setPageSize(pageSize)
				.setFetchMode(FetchMode.MANUAL)
				.setEntityId(entityId);

		return request;
	}
}
