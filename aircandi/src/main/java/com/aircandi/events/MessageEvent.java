package com.aircandi.events;

import com.aircandi.objects.ServiceMessage;

public class MessageEvent {
	public final ServiceMessage message;

	public MessageEvent(ServiceMessage message) {
		this.message = message;
	}
}
