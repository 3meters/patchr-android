package com.patchr.events;

import com.patchr.objects.ActionType;

@SuppressWarnings("ucd")
public abstract class DataEventBase {

	public ActionType actionType;
	public Object     tag;          // Uniquely identifies the requestor

	public DataEventBase() {}

	public DataEventBase setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataEventBase setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}