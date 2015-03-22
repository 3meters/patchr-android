package com.aircandi.events;

import com.aircandi.objects.Cursor;

@SuppressWarnings("ucd")
public class EntitiesRequestEvent extends DataRequestEventBase {

	public Cursor  cursor;
	public Integer linkProfile;

	public EntitiesRequestEvent() {}

	public EntitiesRequestEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public EntitiesRequestEvent setLinkProfile(Integer linkProfile) {
		this.linkProfile = linkProfile;
		return this;
	}
}
