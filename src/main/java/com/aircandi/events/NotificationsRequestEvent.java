package com.aircandi.events;

import com.aircandi.objects.Cursor;

@SuppressWarnings("ucd")
public class NotificationsRequestEvent extends DataRequestEventBase {

	public Cursor cursor;

	public NotificationsRequestEvent() {}

	public NotificationsRequestEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}
}
