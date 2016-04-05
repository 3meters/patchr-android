package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Cursor;
import com.patchr.objects.Entity;
import com.patchr.utilities.Errors;

import java.util.List;

@SuppressWarnings("ucd")
public class EntityQueryResultEvent {

	public ActionType           actionType;
	public FetchMode            fetchMode;
	public Object               data;
	public List<Entity>         entities;       // convenience if data = entities
	public Entity               entity;         // convenience if data = entity
	public Entity               scopingEntity;  // Special case for lists
	public Cursor               cursor;         // The cursor used for the request
	public Boolean              more;           // used if data = pageable array
	public Object               tag;            // passed with request
	public boolean              noop;
	public Errors.ErrorResponse error;

	public EntityQueryResultEvent() {}

	public EntityQueryResultEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public EntityQueryResultEvent setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
		return this;
	}

	public EntityQueryResultEvent setData(Object data) {
		this.data = data;
		return this;
	}

	public EntityQueryResultEvent setEntities(List<Entity> entities) {
		this.entities = entities;
		return this;
	}

	public EntityQueryResultEvent setEntity(Entity entity) {
		this.entity = entity;
		return this;
	}

	public EntityQueryResultEvent setScopingEntity(Entity entity) {
		this.scopingEntity = entity;
		return this;
	}

	public EntityQueryResultEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public EntityQueryResultEvent setMore(Boolean more) {
		this.more = more;
		return this;
	}

	public EntityQueryResultEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
