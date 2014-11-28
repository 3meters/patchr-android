package com.aircandi.components;

import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.objects.Entity;
import com.aircandi.utilities.Json;

public class Extras {

	protected Bundle mExtras = new Bundle();
	protected Entity mEntity;

	/*--------------------------------------------------------------------------------------------
	 * Setters
	 *--------------------------------------------------------------------------------------------*/
	public void setExtras(Bundle extras) {
		if (extras != null) {
			mExtras = extras;
		}
	}

	public void addExtras(Bundle extras) {
		if (extras != null) {
			mExtras.putAll(extras);
		}
	}

	@SuppressWarnings("ucd")
	public Extras setMessage(String message) {
		if (message != null) {
			mExtras.putString(Constants.EXTRA_MESSAGE, message);
		}
		return this;
	}

	public Extras setEntityParentId(String entityParentId) {
		if (entityParentId != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_PARENT_ID, entityParentId);
		}
		return this;
	}

	public Extras setEntityForId(String entityForId) {
		if (entityForId != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_FOR_ID, entityForId);
		}
		return this;
	}

	public Extras setEntitySchema(String entitySchema) {
		if (entitySchema != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_SCHEMA, entitySchema);
		}
		return this;
	}

	public Extras setEntityId(String entityId) {
		if (entityId != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_ID, entityId);
		}
		return this;
	}

	public Extras setForceRefresh(Boolean forceRefresh) {
		if (forceRefresh != null) {
			mExtras.putBoolean(Constants.EXTRA_REFRESH_FROM_SERVICE, forceRefresh);
		}
		return this;
	}

	public Extras setListLinkSchema(String listLinkSchema) {
		if (listLinkSchema != null) {
			mExtras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, listLinkSchema);
		}
		return this;
	}

	public Extras setListNewEnabled(Boolean listNewEnabled) {
		if (listNewEnabled != null) {
			mExtras.putBoolean(Constants.EXTRA_LIST_NEW_ENABLED, listNewEnabled);
		}
		return this;
	}

	public Extras setListItemResId(Integer listItemResId) {
		if (listItemResId != null) {
			mExtras.putInt(Constants.EXTRA_LIST_ITEM_RESID, listItemResId);
		}
		return this;
	}

	public Extras setEntity(Entity entity) {
		if (entity != null) {
			final String jsonEntity = Json.objectToJson(entity);
			mExtras.putString(Constants.EXTRA_ENTITY, jsonEntity);
		}
		return this;
	}

	public Extras setListLinkType(String listLinkType) {
		if (listLinkType != null) {
			mExtras.putString(Constants.EXTRA_LIST_LINK_TYPE, listLinkType);
		}
		return this;
	}

	public Extras setListLinkDirection(String listLinkDirection) {
		if (listLinkDirection != null) {
			mExtras.putString(Constants.EXTRA_LIST_LINK_DIRECTION, listLinkDirection);
		}
		return this;
	}

	public Extras setListTitle(String listTitle) {
		if (listTitle != null) {
			mExtras.putString(Constants.EXTRA_LIST_TITLE, listTitle);
		}
		return this;
	}

	public Extras setListPageSize(Integer listPageSize) {
		if (listPageSize != null) {
			mExtras.putInt(Constants.EXTRA_LIST_PAGE_SIZE, listPageSize);
		}
		return this;
	}

	public Extras setListViewType(String listViewType) {
		if (listViewType != null) {
			mExtras.putString(Constants.EXTRA_LIST_VIEW_TYPE, listViewType);
		}
		return this;
	}

	public Extras setEntityType(String entityType) {
		if (entityType != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_TYPE, entityType);
		}
		return this;
	}

	@SuppressWarnings("ucd")
	public Extras setEntityChildId(String entityChildId) {
		if (entityChildId != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_CHILD_ID, entityChildId);
		}
		return this;
	}

	@SuppressWarnings("ucd")
	public Extras setPlaceId(String placeId) {
		if (placeId != null) {
			mExtras.putString(Constants.EXTRA_PATCH_ID, placeId);
		}
		return this;
	}

	public Extras setListNewMessageResId(Integer listNewMessageResId) {
		if (listNewMessageResId != null) {
			mExtras.putInt(Constants.EXTRA_LIST_NEW_MESSAGE_RESID, listNewMessageResId);
		}
		return this;
	}

	public Extras setLayoutResId(Integer layoutResId) {
		if (layoutResId != null) {
			mExtras.putInt(Constants.EXTRA_LAYOUT_RESID, layoutResId);
		}
		return this;
	}

	public Extras setListLoadingResId(Integer listLoadingResId) {
		if (listLoadingResId != null) {
			mExtras.putInt(Constants.EXTRA_LIST_LOADING_RESID, listLoadingResId);
		}
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Getters
	 *--------------------------------------------------------------------------------------------*/
	public Bundle getExtras() {
		return mExtras;
	}

	public String getMessage() {
		return mExtras.getString(Constants.EXTRA_MESSAGE);
	}

	public String getEntityParentId() {
		return mExtras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
	}

	public String getEntitySchema() {
		return mExtras.getString(Constants.EXTRA_ENTITY_SCHEMA);
	}

	public String getEntityId() {
		return mExtras.getString(Constants.EXTRA_ENTITY_ID);
	}

	public Boolean getForceRefresh() {
		return mExtras.getBoolean(Constants.EXTRA_REFRESH_FROM_SERVICE, false);
	}

	public String getListLinkSchema() {
		return mExtras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
	}

	public Boolean getListNewEnabled() {
		return mExtras.getBoolean(Constants.EXTRA_LIST_NEW_ENABLED, false);
	}

	public Integer getListItemResId() {
		return mExtras.getInt(Constants.EXTRA_LIST_ITEM_RESID);
	}

	public Entity getEntity() {
		if (mEntity == null) {
			final String jsonEntity = mExtras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
		return mEntity;
	}

	public String getListLinkType() {
		return mExtras.getString(Constants.EXTRA_LIST_LINK_TYPE);
	}

	public String getListLinkDirection() {
		return mExtras.getString(Constants.EXTRA_LIST_LINK_DIRECTION);
	}

	public String getListTitle() {
		return mExtras.getString(Constants.EXTRA_LIST_TITLE);
	}

	public Integer getListPageSize() {
		return mExtras.getInt(Constants.EXTRA_LIST_PAGE_SIZE);
	}

	public String getListViewType() {
		return mExtras.getString(Constants.EXTRA_LIST_VIEW_TYPE);
	}

	public String getEntityType() {
		return mExtras.getString(Constants.EXTRA_ENTITY_TYPE);
	}

	public String getEntityChildId() {
		return mExtras.getString(Constants.EXTRA_ENTITY_CHILD_ID);
	}

	public String getPlaceId() {
		return mExtras.getString(Constants.EXTRA_PATCH_ID);
	}

	public Integer getListNewMessageResId() {
		return mExtras.getInt(Constants.EXTRA_LIST_NEW_MESSAGE_RESID);
	}

	public Integer getListLayoutResId() {
		return mExtras.getInt(Constants.EXTRA_LAYOUT_RESID);
	}

	public Integer getListLoadingResId() {
		return mExtras.getInt(Constants.EXTRA_LIST_LOADING_RESID);
	}
}
