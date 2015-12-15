package com.patchr.events;

import com.patchr.components.DataController.ActionType;
import com.patchr.interfaces.IBind;
import com.patchr.objects.Cursor;
import com.patchr.objects.Entity;

import java.util.List;

@SuppressWarnings("ucd")
public class DataResultEvent{

	public ActionType        actionType;
	public IBind.BindingMode mode;
	public Object            data;
	public List<Entity>      entities;       // convenience if data = entities
	public Entity            entity;         // convenience if data = entity
	public Entity            scopingEntity;  // Special case for lists
	public Cursor            cursor;         // The cursor used for the request
	public Boolean           more;           // used if data = pageable array
	public Object            tag;            // passed with request

	public DataResultEvent() {}

	public DataResultEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public DataResultEvent setMode(IBind.BindingMode mode) {
		this.mode = mode;
		return this;
	}

	public DataResultEvent setData(Object data) {
		this.data = data;
		return this;
	}

	public DataResultEvent setEntities(List<Entity> entities) {
		this.entities = entities;
		return this;
	}

	public DataResultEvent setEntity(Entity entity) {
		this.entity = entity;
		return this;
	}

	public DataResultEvent setScopingEntity(Entity entity) {
		this.scopingEntity = entity;
		return this;
	}

	public DataResultEvent setCursor(Cursor cursor) {
		this.cursor = cursor;
		return this;
	}

	public DataResultEvent setMore(Boolean more) {
		this.more = more;
		return this;
	}

	public DataResultEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
