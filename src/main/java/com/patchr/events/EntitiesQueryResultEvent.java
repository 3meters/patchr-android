package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Cursor;
import com.patchr.objects.Entity;
import com.patchr.utilities.Errors;

import java.util.List;

@SuppressWarnings("ucd")
public class EntitiesQueryResultEvent {

	public ActionType           actionType;
	public FetchMode            mode;
	public Object               data;
	public List<Entity>         entities;       // convenience if data = entities
	public Entity               entity;         // convenience if data = entity
	public Entity               scopingEntity;  // Special case for lists
	public Cursor               cursor;         // The cursor used for the request
	public Boolean              more;           // used if data = pageable array
	public Object               tag;            // passed with request
	public boolean              noop;
	public Errors.ErrorResponse error;

	public EntitiesQueryResultEvent() {}

	public EntitiesQueryResultEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public EntitiesQueryResultEvent setMode(FetchMode mode) {
		this.mode = mode;
		return this;
	}

	public EntitiesQueryResultEvent setData(Object data) {
		this.data = data;
		return this;
	}

	public EntitiesQueryResultEvent setEntities(List<Entity> entities) {
		this.entities = entities;
		return this;
	}

	public EntitiesQueryResultEvent setEntity(Entity entity) {
		this.entity = entity;
		return this;
	}

	public EntitiesQueryResultEvent setScopingEntity(Entity entity) {
		this.scopingEntity = entity;
		return this;
	}

	public EntitiesQueryResultEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public EntitiesQueryResultEvent setMore(Boolean more) {
		this.more = more;
		return this;
	}

	public EntitiesQueryResultEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
