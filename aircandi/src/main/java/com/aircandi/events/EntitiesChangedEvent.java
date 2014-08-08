package com.aircandi.events;

import java.util.List;

import com.aircandi.objects.Entity;

@SuppressWarnings("ucd")
public class EntitiesChangedEvent {

	public final List<Entity> entities;
	public final String       source;

	public EntitiesChangedEvent(List<Entity> entities, String source) {
		this.entities = entities;
		this.source = source;
	}
}
