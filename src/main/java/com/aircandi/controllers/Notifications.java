package com.aircandi.controllers;

import android.support.annotation.NonNull;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Notification;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;

import java.util.Map;

public class Notifications extends EntityControllerBase {

	public Notifications() {
		mSchema = Constants.SCHEMA_ENTITY_NOTIFICATION;
		mPageSize = Integers.getInteger(R.integer.page_size_users);
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	@NonNull
	@Override
	public Entity makeNew() {
		Entity entity = new Notification();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		return entity;
	}

	public void bind(Entity entity, View view, String groupTag) {
		super.bind(entity, view, groupTag);
	}

		@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Notification.setPropertiesFromMap(new Notification(), map, nameMapping);
	}
}
