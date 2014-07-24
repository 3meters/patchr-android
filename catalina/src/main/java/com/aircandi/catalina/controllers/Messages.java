package com.aircandi.catalina.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import com.aircandi.catalina.Catalina;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.LinkProfile;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.ui.MessageForm;
import com.aircandi.catalina.ui.edit.MessageEdit;
import com.aircandi.components.AirApplication;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.EntityControllerBase;
import com.aircandi.objects.Entity;
import com.aircandi.objects.NotificationType;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;

public class Messages extends EntityControllerBase {

	public Messages() {
		mColorPrimary = R.color.holo_blue_dark;
		mSchema = Constants.SCHEMA_ENTITY_MESSAGE;
		mBrowseClass = MessageForm.class;
		mEditClass = MessageEdit.class;
		mNewClass = MessageEdit.class;
		mPageSize = Integers.getInteger(R.integer.page_size_messages);
		mListLayoutResId = R.layout.entity_list_fragment;
		mListItemResId = R.layout.temp_listitem_message;
		mListLoadingResId = R.layout.temp_list_item_loading;
	}

	@Override
	public Entity makeNew() {
		Entity entity = new Message();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_MESSAGE;
	}

	@Override
	public Drawable getIcon() {
		Drawable icon = Catalina.applicationContext.getResources().getDrawable(R.drawable.img_comment_temp);
		icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);
		return icon;
	}

	@Override
	public Integer getNotificationType(Entity entity) {
		if (entity.photo != null) {
			return NotificationType.BIG_PICTURE;
		}
		else {
			return NotificationType.BIG_TEXT;
		}
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
		return Message.setPropertiesFromMap(new Message(), map, nameMapping);
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------
}
