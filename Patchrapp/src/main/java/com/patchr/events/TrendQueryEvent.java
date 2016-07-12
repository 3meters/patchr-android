package com.patchr.events;

import com.patchr.Constants;
import com.patchr.objects.enums.ActionType;
import com.patchr.objects.Cursor;

@SuppressWarnings("ucd")
public class TrendQueryEvent extends AbsEntitiesQueryEvent {

	public String toSchema;
	public String fromSchema;
	public String linkType;

	public TrendQueryEvent() {}

	public static TrendQueryEvent build(ActionType actionType, String fromSchema, String toSchema, String linkType) {

		Integer skipCount = ((int) Math.ceil((double) 0 / Constants.PAGE_SIZE) * Constants.PAGE_SIZE);
		Cursor cursor = new Cursor();
		cursor.skip = skipCount;

		TrendQueryEvent request = new TrendQueryEvent();

		request.cursor = cursor;
		request.actionType = actionType;
		request.fromSchema = fromSchema;
		request.toSchema = toSchema;
		request.linkType = linkType;

		return request;
	}
}
