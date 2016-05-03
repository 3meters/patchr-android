package com.patchr.events;

import com.patchr.objects.Shortcut;

@SuppressWarnings("ucd")
public class LinkInsertEvent extends DataEventBase {

	public String   linkId;
	public String   fromId;
	public String   toId;
	public String   type;
	public Boolean  enabled;
	public Shortcut fromShortcut;
	public Shortcut toShortcut;
	public String   actionEvent;
	public Boolean  skipCache;
}
