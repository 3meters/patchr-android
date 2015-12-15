package com.patchr.events;

import com.patchr.components.DataController.ActionType;
import com.patchr.interfaces.IBind;
import com.patchr.objects.CacheStamp;

@SuppressWarnings("ucd")
public abstract class DataRequestEventBase {

	public String            entityId;
	public ActionType        actionType;
	public IBind.BindingMode mode;
	public Object            tag;          // Uniquely identifies the requestor
	public CacheStamp        cacheStamp;

	public DataRequestEventBase() {}

	public DataRequestEventBase setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataRequestEventBase setMode(IBind.BindingMode mode) {
		this.mode = mode;
		return this;
	}

	public DataRequestEventBase setEntityId(String entityId) {
		this.entityId = entityId;
		return this;
	}

	public DataRequestEventBase setTag(Object tag) {
		this.tag = tag;
		return this;
	}

	public DataRequestEventBase setCacheStamp(CacheStamp cacheStamp) {
		this.cacheStamp = cacheStamp;
		return this;
	}
}
