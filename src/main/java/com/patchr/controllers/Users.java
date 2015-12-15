package com.patchr.controllers;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.User;
import com.patchr.ui.UserForm;
import com.patchr.ui.edit.UserEdit;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Integers;

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
