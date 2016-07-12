package com.patchr.events;

import com.patchr.objects.enums.ActionType;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Cursor;
import com.patchr.objects.enums.FetchMode;
import com.patchr.utilities.Errors;

@SuppressWarnings("ucd")
public abstract class AbsEntityQueryEvent {

	public String               entityId;
	public ActionType           actionType;
	public FetchMode            fetchMode;
	public Object               tag;          // Uniquely identifies the requestor
	public CacheStamp           cacheStamp;
	public Cursor               cursor;
	public Integer              linkProfile;
	public Integer              pageSize;
	public boolean              noop;
	public Errors.ErrorResponse error;
}
