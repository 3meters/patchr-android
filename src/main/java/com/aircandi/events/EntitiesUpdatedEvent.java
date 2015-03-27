package com.aircandi.events;

import com.aircandi.objects.Entity;

import java.util.List;

@SuppressWarnings("ucd")
public class EntitiesUpdatedEvent {

	public final List<Entity> entities;
	public final String       source;

	public EntitiesUpdatedEvent(List<Entity> entities, String source) {
		this.entities = entities;
		this.source = source;
	}
}