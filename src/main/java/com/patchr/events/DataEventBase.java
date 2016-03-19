package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.FetchMode;

@SuppressWarnings("ucd")
public abstract class DataEventBase {

	public ActionType actionType;
	public FetchMode  mode;
	public Object     tag;          // Uniquely identifies the requestor

	public DataEventBase() {}

	public DataEventBase setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataEventBase setMode(FetchMode mode) {
		this.mode = mode;
		return this;
	}

	public DataEventBase setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
