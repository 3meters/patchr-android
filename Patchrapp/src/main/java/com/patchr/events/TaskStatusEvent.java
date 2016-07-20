package com.patchr.events;

import com.patchr.objects.enums.TaskStatus;

@SuppressWarnings("ucd")
public class TaskStatusEvent {
	public final String     tag;
	public final String     entityId;
	public final String     parentId;
	public final TaskStatus status;

	public TaskStatusEvent(String tag, String entityId, String parentId, TaskStatus status) {
		this.tag = tag;
		this.entityId = entityId;
		this.parentId = parentId;
		this.status = status;
	}
}
