package com.aircandi.events;

import com.aircandi.objects.CacheStamp;

@SuppressWarnings("ucd")
public abstract class DataRequestEventBase {

	public String     entityId;
	public Integer    actionType;
	public CacheStamp cacheStamp;
	public Object     tag;          // Uniquely identifies the requestor

	public DataRequestEventBase() {}

	public DataRequestEventBase setActionType(Integer actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataRequestEventBase setEntityId(String entityId) {
		this.entityId = entityId;
		return this;
	}

	public DataRequestEventBase setCacheStamp(CacheStamp cacheStamp) {
		this.cacheStamp = cacheStamp;
		return this;
	}

	public DataRequestEventBase setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
