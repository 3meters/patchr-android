package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Cursor;
import com.patchr.objects.FetchMode;

@SuppressWarnings("ucd")
public abstract class AbsEntitiesQueryEvent {

	public String     entityId;
	public ActionType actionType;
	public Object     tag;          // Uniquely identifies the requestor
	public CacheStamp cacheStamp;
	public Cursor     cursor;
	public FetchMode  fetchMode;
	public Integer    linkProfile;
}
