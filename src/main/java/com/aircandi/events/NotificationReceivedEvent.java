package com.aircandi.events;

import com.aircandi.objects.Notification;

public class NotificationReceivedEvent {
	public final Notification notification;

	public NotificationReceivedEvent(Notification notification) {
		this.notification = notification;
	}
}
