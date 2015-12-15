package com.patchr.events;

import com.patchr.components.DataController.ActionType;
import com.patchr.interfaces.IBind;

@SuppressWarnings("ucd")
public abstract class DataEventBase {

	public ActionType        actionType;
	public IBind.BindingMode mode;
	public Object            tag;          // Uniquely identifies the requestor

	public DataEventBase() {}

	public DataEventBase setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataEventBase setMode(IBind.BindingMode mode) {
		this.mode = mode;
		return this;
	}

	public DataEventBase setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
