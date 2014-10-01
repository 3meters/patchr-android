package com.aircandi.controllers;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Action;
import com.aircandi.objects.MessageTriggerType;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.objects.ProviderMap;
import com.aircandi.objects.ServiceMessage;
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
		entity.visibility = Constants.VISIBILITY_PUBLIC;

		Place place = (Place) entity;
		place.provider = new ProviderMap();
		place.provider.aircandi = Patch.getInstance().getCurrentUser().id;

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

	@Override
	public String getNotificationTicker(ServiceMessage message, String eventCategory) {
		if (eventCategory.equals(Action.EventCategory.INSERT)) {
			if (message.getTriggerCategory().equals(MessageTriggerType.TriggerType.NEARBY)) {
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
