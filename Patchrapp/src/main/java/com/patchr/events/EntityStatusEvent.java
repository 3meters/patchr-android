package com.patchr.events;

import com.patchr.objects.enums.EntityStatus;

@SuppressWarnings("ucd")
public class EntityStatusEvent {
	public final EntityStatus status;

	public EntityStatusEvent(EntityStatus status) {
		this.status = status;
	}
}
