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

	public LinkInsertEvent() {}

	public LinkInsertEvent setLinkId(String linkId) {
		this.linkId = linkId;
		return this;
	}

	public LinkInsertEvent setFromId(String fromId) {
		this.fromId = fromId;
		return this;
	}

	public LinkInsertEvent setToId(String toId) {
		this.toId = toId;
		return this;
	}

	public LinkInsertEvent setType(String type) {
		this.type = type;
		return this;
	}

	public LinkInsertEvent setEnabled(Boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public LinkInsertEvent setFromShortcut(Shortcut fromShortcut) {
		this.fromShortcut = fromShortcut;
		return this;
	}

	public LinkInsertEvent setToShortcut(Shortcut toShortcut) {
		this.toShortcut = toShortcut;
		return this;
	}

	public LinkInsertEvent setActionEvent(String actionEvent) {
		this.actionEvent = actionEvent;
		return this;
	}

	public LinkInsertEvent setSkipCache(Boolean skipCache) {
		this.skipCache = skipCache;
		return this;
	}
}
