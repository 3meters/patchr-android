package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;
import com.patchr.objects.CacheStamp;

@SuppressWarnings("ucd")
public abstract class AbsEntityQueryEvent {

	public String     entityId;
	public ActionType actionType;
	public FetchMode  fetchMode;
	public Object     tag;          // Uniquely identifies the requestor
	public CacheStamp cacheStamp;
	public Cursor     cursor;
	public Integer    linkProfile;
	public Integer    pageSize;

	public AbsEntityQueryEvent() {}

	public AbsEntityQueryEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public AbsEntityQueryEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public AbsEntityQueryEvent setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public AbsEntityQueryEvent setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
		return this;
	}

	public AbsEntityQueryEvent setEntityId(String entityId) {
		this.entityId = entityId;
		return this;
	}

	public AbsEntityQueryEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}

	public AbsEntityQueryEvent setCacheStamp(CacheStamp cacheStamp) {
		this.cacheStamp = cacheStamp;
		return this;
	}

	public AbsEntityQueryEvent setLinkProfile(Integer linkProfile) {
		this.linkProfile = linkProfile;
		return this;
	}
}
