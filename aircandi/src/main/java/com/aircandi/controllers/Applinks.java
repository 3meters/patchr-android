package com.aircandi.controllers;

import java.util.Locale;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.ui.edit.ApplinkEdit;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;

public class Applinks extends EntityControllerBase {

	public Applinks() {
		mEditClass = ApplinkEdit.class;
		mNewClass = ApplinkEdit.class;
		mSchema = Constants.SCHEMA_ENTITY_APPLINK;
		mPageSize = Integers.getInteger(R.integer.page_size_applinks);
		mListItemResId = R.layout.temp_listitem_applink;
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_list_item_loading;
		mListNewMessageResId = R.string.button_list_new_applink;
	}

	@Override
	public Entity makeNew() {
		Entity entity = new Applink();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	@Override
	public Photo getDefaultPhoto(String type) {
		String prefix = "img_placeholder_logo_bw";
		String source = PhotoSource.resource;
		if (type != null) {
			prefix = type.toLowerCase(Locale.US) + ".png";
			source = PhotoSource.assets_applinks;
		}
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Applink.setPropertiesFromMap(new Applink(), map, nameMapping);
	}
}
