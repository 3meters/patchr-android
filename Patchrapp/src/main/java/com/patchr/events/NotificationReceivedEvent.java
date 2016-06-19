package com.patchr.events;

import com.patchr.objects.Notification;

public class NotificationReceivedEvent {
	public final Notification notification;

	public NotificationReceivedEvent(Notification notification) {
		this.notification = notification;
	}
}
