package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;

@SuppressWarnings("ucd")
public abstract class AbsEntitiesQueryEvent {

	public String     entityId;
	public ActionType actionType;
	public FetchMode  fetchMode;
	public Object     tag;          // Uniquely identifies the requestor
	public CacheStamp cacheStamp;
	public Cursor     cursor;
	public Integer    linkProfile;
	public Integer    pageSize;

	public AbsEntitiesQueryEvent() {}

	public AbsEntitiesQueryEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public AbsEntitiesQueryEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public AbsEntitiesQueryEvent setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public AbsEntitiesQueryEvent setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
		return this;
	}

	public AbsEntitiesQueryEvent setEntityId(String entityId) {
		this.entityId = entityId;
		return this;
	}

	public AbsEntitiesQueryEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}

	public AbsEntitiesQueryEvent setCacheStamp(CacheStamp cacheStamp) {
		this.cacheStamp = cacheStamp;
		return this;
	}

	public AbsEntitiesQueryEvent setLinkProfile(Integer linkProfile) {
		this.linkProfile = linkProfile;
		return this;
	}
}
