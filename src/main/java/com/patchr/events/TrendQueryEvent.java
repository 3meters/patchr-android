package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;

@SuppressWarnings("ucd")
public class TrendQueryEvent extends AbsEntitiesQueryEvent {

	public String toSchema;
	public String fromSchema;
	public String linkType;

	public TrendQueryEvent() {}

	public TrendQueryEvent setToSchema(String toSchema) {
		this.toSchema = toSchema;
		return this;
	}

	public TrendQueryEvent setFromSchema(String fromSchema) {
		this.fromSchema = fromSchema;
		return this;
	}

	public TrendQueryEvent setLinkType(String linkType) {
		this.linkType = linkType;
		return this;
	}

	public static TrendQueryEvent build(ActionType actionType, String fromSchema, String toSchema, String linkType, int pageSize) {

		Integer skipCount = ((int) Math.ceil((double) 0 / pageSize) * pageSize);
		Cursor cursor = new Cursor()
				.setLimit(pageSize)
				.setSkip(skipCount);

		TrendQueryEvent request = new TrendQueryEvent();
		request.setCursor(cursor)
				.setPageSize(pageSize)
				.setActionType(actionType)
				.setFetchMode(FetchMode.MANUAL);

		request.setFromSchema(fromSchema)
				.setToSchema(toSchema)
				.setLinkType(linkType);

		return request;
	}
}
