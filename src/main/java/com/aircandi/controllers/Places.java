package com.aircandi.controllers;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.objects.ProviderMap;
import com.aircandi.ui.PlaceForm;
import com.aircandi.ui.edit.PlaceEdit;
import com.aircandi.utilities.DateTime;

import java.util.Map;

public class Places extends EntityControllerBase {

	public Places() {
		mColorPrimary = R.color.holo_red_dark;
		mSchema = Constants.SCHEMA_ENTITY_PLACE;
		mBrowseClass = PlaceForm.class;
		mEditClass = PlaceEdit.class;
		mNewClass = PlaceEdit.class;
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	@Override
	public Entity makeNew() {

		Entity entity = new Place();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		entity.privacy = Constants.PRIVACY_PRIVATE;

		Place place = (Place) entity;
		place.provider = new ProviderMap();
		place.provider.aircandi = Patchr.getInstance().getCurrentUser().id;

		return place;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_PLACE;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Place.setPropertiesFromMap(new Place(), map, nameMapping);
	}
}
