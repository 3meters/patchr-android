package com.aircandi.events;

import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;

import java.util.List;

@SuppressWarnings("ucd")
public class DataReadyEvent {

	public Integer      actionType;
	public Object       data;
	public List<Entity> entities;       // convenience if data = entities
	public Entity       entity;         // convenience if data = entity
	public Cursor       cursor;         // The cursor used for the request
	public Boolean      more;           // used if data = pageable array
	public Object       tag;            // passed with request

	public DataReadyEvent() {}

	public DataReadyEvent setActionType(Integer actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataReadyEvent setData(Object data) {
		this.data = data;
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
