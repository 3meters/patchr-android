package com.aircandi.controllers;

import android.graphics.drawable.Drawable;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Comment;
import com.aircandi.objects.Entity;
import com.aircandi.objects.NotificationType;
import com.aircandi.ui.CommentForm;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;

import java.util.Map;

public class Comments extends EntityControllerBase {

	public Comments() {
		mColorPrimary = R.color.holo_orange_dark;
		mBrowseClass = CommentForm.class;
		mEditClass = CommentEdit.class;
		mNewClass = CommentEdit.class;
		mSchema = Constants.SCHEMA_ENTITY_COMMENT;
		mPageSize = Integers.getInteger(R.integer.page_size_comments);
		mListItemResId = R.layout.temp_listitem_comment;
		mListLayoutResId = R.layout.entity_list_fragment;
		mListLoadingResId = R.layout.temp_list_item_loading;
		mListNewMessageResId = R.string.button_list_new_comment;
	}

	@Override
	public Entity makeNew() {
		Entity entity = new Comment();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	@Override
	public Integer getNotificationType(Entity entity) {
		return NotificationType.BIG_TEXT;
	}

	@Override
	public Drawable getIcon() {
		Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_comment_temp);
		//icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);
		return icon;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Comment.setPropertiesFromMap(new Comment(), map, nameMapping);
	}
}
