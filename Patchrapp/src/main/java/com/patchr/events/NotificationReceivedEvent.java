package com.patchr.events;

import com.patchr.model.RealmEntity;

public class NotificationReceivedEvent {
	public final RealmEntity notification;

	public NotificationReceivedEvent(RealmEntity notification) {
		this.notification = notification;
	}
}
