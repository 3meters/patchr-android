package com.aircandi.components;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.objects.Entity;
import com.aircandi.utilities.Json;

public class IntentBuilder {

	private Context  mContext;
	private Class<?> mClass;
	private String   mAction;
	private Uri      mData;
	private String   mMimeType;
	private Bundle mExtras = new Bundle();

	public IntentBuilder() {}

	public IntentBuilder(Context context, Class<?> clazz) {
		mContext = context;
		mClass = clazz;
	}

	public IntentBuilder(String action) {
		mAction = action;
	}

	public Intent create() {
		Intent intent = new Intent();

		/* Internal intent */
		if (mContext != null && mClass != null) {
			intent = new Intent(mContext, mClass);
		}

		/* External intent */
		else if (mAction != null) {
			intent = new Intent(mAction);
			if (mData != null) {
				intent.setData(mData);
			}
			if (mMimeType != null) {
				intent.setType(mMimeType);
			}
		}

		if (!mExtras.isEmpty()) {
			intent.putExtras(mExtras);
		}

		if (mData != null) {
			intent.setData(mData);
		}

		return intent;
	}

	public IntentBuilder setData(Uri data) {
		mData = data;
		return this;
	}

	public IntentBuilder setEntity(Entity entity) {
		if (entity != null) {
			final String jsonEntity = Json.objectToJson(entity);
			mExtras.putString(Constants.EXTRA_ENTITY, jsonEntity);
		}
		return this;
	}

	public IntentBuilder setEntityId(String entityId) {
		if (entityId != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_ID, entityId);
		}
		return this;
	}

	public IntentBuilder setEntitySchema(String entitySchema) {
		if (entitySchema != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_SCHEMA, entitySchema);
		}
		return this;
	}

	public IntentBuilder setEntityType(String entityType) {
		if (entityType != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_TYPE, entityType);
		}
		return this;
	}

	public IntentBuilder setEntityParentId(String entityParentId) {
		if (entityParentId != null) {
			mExtras.putString(Constants.EXTRA_ENTITY_PARENT_ID, entityParentId);
		}
		return this;
	}

	public IntentBuilder setListLinkSchema(String listLinkSchema) {
		if (listLinkSchema != null) {
			mExtras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, listLinkSchema);
		}
		return this;
	}

	public IntentBuilder setListLinkType(String listLinkType) {
		if (listLinkType != null) {
			mExtras.putString(Constants.EXTRA_LIST_LINK_TYPE, listLinkType);
		}
		return this;
	}

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

	public String getMimeType() {
		return mMimeType;
	}
}
