package com.patchr.events;

import com.patchr.components.DataController.ActionType;

@SuppressWarnings("ucd")
public class DataNoopEvent {

	public ActionType actionType;
	public Object     tag;            // passed with request

	public DataNoopEvent() {}

	public DataNoopEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataNoopEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
