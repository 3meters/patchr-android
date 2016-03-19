package com.patchr.events;

import com.patchr.objects.ActionType;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Cursor;
import com.patchr.objects.Entity;

import java.util.List;

@SuppressWarnings("ucd")
public class DataQueryResultEvent {

	public ActionType   actionType;
	public FetchMode    mode;
	public Object       data;
	public List<Entity> entities;       // convenience if data = entities
	public Entity       entity;         // convenience if data = entity
	public Entity       scopingEntity;  // Special case for lists
	public Cursor       cursor;         // The cursor used for the request
	public Boolean      more;           // used if data = pageable array
	public Object       tag;            // passed with request

	public DataQueryResultEvent() {}

	public DataQueryResultEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataQueryResultEvent setMode(FetchMode mode) {
		this.mode = mode;
		return this;
	}

	public DataQueryResultEvent setData(Object data) {
		this.data = data;
		return this;
	}

	public DataQueryResultEvent setEntities(List<Entity> entities) {
		this.entities = entities;
		return this;
	}

	public DataQueryResultEvent setEntity(Entity entity) {
		this.entity = entity;
		return this;
	}

	public DataQueryResultEvent setScopingEntity(Entity entity) {
		this.scopingEntity = entity;
		return this;
	}

	public DataQueryResultEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public DataQueryResultEvent setMore(Boolean more) {
		this.more = more;
		return this;
	}

	public DataQueryResultEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
