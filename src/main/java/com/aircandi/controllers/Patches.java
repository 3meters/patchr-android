package com.aircandi.controllers;

import android.support.annotation.NonNull;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Category;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.ui.PatchForm;
import com.aircandi.ui.edit.PatchEdit;
import com.aircandi.utilities.DateTime;

import java.util.Map;

public class Patches extends EntityControllerBase {

	public Patches() {
		mColorPrimary = R.color.holo_red_dark;
		mSchema = Constants.SCHEMA_ENTITY_PATCH;
		mBrowseClass = PatchForm.class;
		mEditClass = PatchEdit.class;
		mNewClass = PatchEdit.class;
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	@NonNull
	@Override
	public Entity makeNew() {

		Patch entity = new Patch();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.privacy = Constants.PRIVACY_PRIVATE;
		entity.category = new Category();
		entity.category.id = "chat";
		entity.category.name = "Chat";
		entity.category.photo = new Photo("img_group.png", null, null, null, Photo.PhotoSource.assets_categories);

		return entity;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_PATCH;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Patch.setPropertiesFromMap(new Patch(), map, nameMapping);
	}
}
