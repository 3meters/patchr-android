package com.aircandi.controllers;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.User;
import com.aircandi.ui.user.UserEdit;
import com.aircandi.ui.user.UserForm;
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

	@Override
	public Entity makeNew() {
		Entity entity = new User();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_USER_CURRENT;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return User.setPropertiesFromMap(new User(), map, nameMapping);
	}
}
