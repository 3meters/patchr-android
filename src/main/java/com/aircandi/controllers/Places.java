package com.aircandi.controllers;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.ui.PlaceForm;

import java.util.Map;

public class Places extends EntityControllerBase {

	public Places() {
		mColorPrimary = R.color.holo_red_dark;
		mSchema = Constants.SCHEMA_ENTITY_PLACE;
		mBrowseClass = PlaceForm.class;
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	public boolean supportsNew() {
		return false;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.NO_LINKS;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Place.setPropertiesFromMap(new Place(), map, nameMapping);
	}
}
