package com.aircandi.events;

@SuppressWarnings("ucd")
public abstract class DataEventBase {

	public Integer    actionType;
	public Object     tag;          // Uniquely identifies the requestor

	public DataEventBase() {}

	public DataEventBase setActionType(Integer actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataEventBase setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
