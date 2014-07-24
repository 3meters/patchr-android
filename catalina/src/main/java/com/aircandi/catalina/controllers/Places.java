package com.aircandi.catalina.controllers;

import java.util.ArrayList;
import java.util.List;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.catalina.ui.PlaceForm;
import com.aircandi.components.AirApplication;
import com.aircandi.components.StringManager;

public class Places extends com.aircandi.controllers.Places {

	public Places() {
		mBrowseClass = PlaceForm.class;
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
}
