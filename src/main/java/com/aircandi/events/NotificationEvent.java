package com.aircandi.events;

import com.aircandi.objects.Notification;

public class NotificationEvent {
	public final Notification notification;

	public NotificationEvent(Notification notification) {
		this.notification = notification;
	}
}
