package com.aircandi.controllers;

import android.graphics.drawable.Drawable;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AirApplication;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Action;
import com.aircandi.objects.ActivityBase;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.objects.ProviderMap;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.ui.PlaceForm;
import com.aircandi.ui.edit.PlaceEdit;
import com.aircandi.utilities.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Places extends EntityControllerBase {

	public Places() {
		mColorPrimary = R.color.holo_red_dark;
		mSchema = Constants.SCHEMA_ENTITY_PLACE;
		mBrowseClass = PlaceForm.class;
		mEditClass = PlaceEdit.class;
		mNewClass = PlaceEdit.class;
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_list_item_loading;
	}

	@Override
	public Entity makeNew() {

		Entity entity = new Place();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		entity.visibility = Constants.VISIBILITY_PUBLIC;

		Place place = (Place) entity;
		place.provider = new ProviderMap();
		place.provider.aircandi = Aircandi.getInstance().getCurrentUser().id;

		return place;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_PLACE;
	}

	@Override
	public Drawable getIcon() {
		Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_place_temp);
		//icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);
		return icon;
	}

	@Override
	public List<Object> getApplications(String themeTone) {

		final List<Object> listData = new ArrayList<Object>();

		listData.add(new AirApplication(themeTone.equals("light") ? R.drawable.ic_action_picture_light : R.drawable.ic_action_picture_dark
				, StringManager.getString(R.string.dialog_application_picture_new), null, Constants.SCHEMA_ENTITY_PICTURE));

		listData.add(new AirApplication(themeTone.equals("light") ? R.drawable.ic_action_monolog_light : R.drawable.ic_action_monolog_dark
				, StringManager.getString(R.string.dialog_application_comment_new), null, Constants.SCHEMA_ENTITY_COMMENT));

		return listData;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Place.setPropertiesFromMap(new Place(), map, nameMapping);
	}

}
