package com.aircandi.events;

import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;

import java.util.List;

@SuppressWarnings("ucd")
public class DataReadyEvent {

	public Integer      actionType;
	public List<Entity> entities;
	public Entity       entity;
	public Cursor       cursor;           // The cursor used for the request
	public Boolean      more;
	public Object       tag;

	public DataReadyEvent() {}

	public DataReadyEvent setActionType(Integer actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataReadyEvent setEntities(List<Entity> entities) {
		this.entities = entities;
		return this;
	}

	public DataReadyEvent setEntity(Entity entity) {
		this.entity = entity;
		return this;
	}

	public DataReadyEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public DataReadyEvent setMore(Boolean more) {
		this.more = more;
		return this;
	}

	public DataReadyEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
