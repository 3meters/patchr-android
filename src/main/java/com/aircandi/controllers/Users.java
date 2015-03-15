package com.aircandi.controllers;

import android.support.annotation.NonNull;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkSpecType;
import com.aircandi.objects.User;
import com.aircandi.ui.UserForm;
import com.aircandi.ui.edit.UserEdit;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;

import java.util.Map;

public class Users extends EntityControllerBase {

	public Users() {
		mBrowseClass = UserForm.class;
		mEditClass = UserEdit.class;
		mNewClass = UserEdit.class;
		mSchema = Constants.SCHEMA_ENTITY_USER;
		mPageSize = Integers.getInteger(R.integer.page_size_users);
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	@NonNull
	@Override
	public Entity makeNew() {
		Entity entity = new User();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		return entity;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkSpecType.LINKS_FOR_USER_CURRENT;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return User.setPropertiesFromMap(new User(), map, nameMapping);
	}
}
