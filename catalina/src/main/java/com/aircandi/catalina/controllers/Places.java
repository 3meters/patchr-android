package com.aircandi.catalina.controllers;

import java.util.ArrayList;
import java.util.List;

import com.aircandi.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.ui.PlaceForm;
import com.aircandi.catalina.ui.edit.PlaceEdit;
import com.aircandi.components.AirApplication;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Action;
import com.aircandi.objects.ActivityBase;
import com.aircandi.objects.ServiceMessage;

public class Places extends com.aircandi.controllers.Places {

	public Places() {
		mBrowseClass = PlaceForm.class;
		mNewClass = PlaceEdit.class;
		mEditClass = PlaceEdit.class;
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
	public String getNotificationTicker(ServiceMessage message, String eventCategory) {
		if (eventCategory.equals(Action.EventCategory.INSERT)) {
			if (message.getTriggerCategory().equals(ActivityBase.TriggerType.NEARBY)) {
				return String.format(StringManager.getString(R.string.label_notification_ticker_place_insert_nearby), message.title);
			}
			return String.format(StringManager.getString(R.string.label_notification_ticker_place_insert), message.title);
		}
		else if (eventCategory.equals(Action.EventCategory.SHARE)) {
			return String.format(StringManager.getString(R.string.label_notification_ticker_place_share), message.title);
		}
		return super.getNotificationTicker(message, eventCategory);
	}
}
