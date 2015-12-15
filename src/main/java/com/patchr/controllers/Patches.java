package com.patchr.controllers;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.ui.PatchForm;
import com.patchr.ui.edit.PatchEdit;
import com.patchr.utilities.DateTime;

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
		entity.privacy = Constants.PRIVACY_PUBLIC;

		return entity;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkSpecType.LINKS_FOR_PATCH;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Patch.setPropertiesFromMap(new Patch(), map, nameMapping);
	}
}
