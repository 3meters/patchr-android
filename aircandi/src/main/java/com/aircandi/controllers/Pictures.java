package com.aircandi.controllers;

import android.graphics.drawable.Drawable;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AirApplication;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.Post;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.PictureForm;
import com.aircandi.ui.edit.PictureEdit;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Pictures extends EntityControllerBase {

	public Pictures() {
		mColorPrimary = R.color.holo_green_dark;
		mSchema = Constants.SCHEMA_ENTITY_PICTURE;

		mBrowseClass = PictureForm.class;
		mEditClass = PictureEdit.class;
		mNewClass = PictureEdit.class;

		mListViewType = ViewType.GRID;
		mPageSize = Integers.getInteger(R.integer.page_size_pictures);
		mListItemResId = R.layout.temp_grid_item_entity;
		mListLayoutResId = R.layout.entity_grid_fragment;
		mListLoadingResId = R.layout.temp_grid_item_loading;
		mListNewMessageResId = R.string.button_list_new_picture;
	}

	@Override
	public Entity makeNew() {
		Entity entity = new Post();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_PICTURE;
	}

	@Override
	public String getName(Boolean plural) {
		return (plural ? Constants.SCHEMA_REMAP_PICTURE + "s" : Constants.SCHEMA_REMAP_PICTURE);
	}

	@Override
	public Drawable getIcon() {
		Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_picture_temp);
		//icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);
		return icon;
	}

	@Override
	public Integer getNotificationType(Entity entity) {
		if (entity.getPhoto().getUri() != null) {
			return NotificationType.BIG_PICTURE;
		}
		return NotificationType.NORMAL;
	}

	@Override
	public List<Object> getApplications(String themeTone) {

		final List<Object> listData = new ArrayList<Object>();

		listData.add(new AirApplication(themeTone.equals("light") ? R.drawable.ic_action_monolog_light : R.drawable.ic_action_monolog_dark
				, StringManager.getString(R.string.dialog_application_comment_new), null, Constants.SCHEMA_ENTITY_COMMENT));

		return listData;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Post.setPropertiesFromMap(new Post(), map, nameMapping);
	}

}
