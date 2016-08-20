package com.patchr.events;

public class NotificationReceivedEvent {
	public final String targetId;
	public final String parentId;
	public final String eventType;

	public NotificationReceivedEvent(String targetId, String parentId, String eventType) {
		this.targetId = targetId;
		this.parentId = parentId;
		this.eventType = eventType;
	}
}
