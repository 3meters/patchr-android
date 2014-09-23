package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Photo;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.UI;

@SuppressLint("Registered")
public class CommentForm extends BaseEntityForm {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_COMMENT;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * UI
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void draw(View view) {
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		mFirstDraw = false;

		final TextView description = (TextView) findViewById(R.id.description);
		final AirImageView userPhotoView = (AirImageView) findViewById(R.id.user_photo);
		final View userHolder = findViewById(R.id.holder_user);
		final TextView userName = (TextView) findViewById(R.id.user_name);
		final TextView toName = (TextView) findViewById(R.id.to_name);
		final TextView createdDate = (TextView) findViewById(R.id.created_date);
		final EntityView parent = (EntityView) findViewById(R.id.parent);

		UI.setVisibility(findViewById(R.id.button_delete), View.GONE);
		if (mEntity.ownerId != null && Aircandi.getInstance().getCurrentUser() != null
				&& (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
				|| (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Aircandi.getInstance().getCurrentUser().developer != null
				&& Aircandi.getInstance().getCurrentUser().developer))) {
			UI.setVisibility(findViewById(R.id.button_delete), View.VISIBLE);
		}

		/* Message text */

		UI.setVisibility(description, View.GONE);
		if (description != null) {
			description.setText(null);
			if (!TextUtils.isEmpty(mEntity.description)) {
				description.setText(Html.fromHtml(mEntity.description));
				UI.setVisibility(description, View.VISIBLE);
			}
		}
		
		/* User photo */

		UI.setVisibility(userPhotoView, View.GONE);
		if (userPhotoView != null && mEntity.creator != null) {
			/*
			 * Acting a cheap proxy for user view so setting photoview to entity instead of photo.
			 */
			Photo photo = mEntity.creator.getPhoto();
			if (userPhotoView.getPhoto() == null || !userPhotoView.getPhoto().getUri().equals(photo.getUri())) {
				UI.drawPhoto(userPhotoView, photo);
			}
			userHolder.setTag(mEntity.creator);
			UI.setVisibility(userPhotoView, View.VISIBLE);
		}
		
		/* User name */

		UI.setVisibility(userName, View.GONE);
		if (userName != null && mEntity.creator != null && mEntity.creator.name != null && mEntity.creator.name.length() > 0) {
			userName.setText(mEntity.creator.name);
			UI.setVisibility(userName, View.VISIBLE);
		}

		/* User area */

		UI.setVisibility(toName, View.GONE);
		UI.setVisibility(findViewById(R.id.symbol_at), View.GONE);
		if (toName != null && mEntity.creator != null && !TextUtils.isEmpty(mEntity.creator.area)) {
			toName.setText(mEntity.creator.area);
			UI.setVisibility(toName, View.VISIBLE);
			UI.setVisibility(findViewById(R.id.symbol_at), View.VISIBLE);
		}
		
		/* Parent context */

		UI.setVisibility(parent, View.GONE);
		if (parent != null) {
			if (mEntity.toId != null) {
				Entity parentEntity = EntityManager.getCacheEntity(mEntity.toId);
				if (parentEntity != null) {
					parent.databind(parentEntity);
					UI.setVisibility(parent, View.VISIBLE);
				}
			}
			else if (mEntity.place != null) {
				parent.databind(mEntity.place);
				UI.setVisibility(parent, View.VISIBLE);
			}
			else if (mEntity.placeId != null) {
				Entity parentEntity = EntityManager.getCacheEntity(mEntity.placeId);
				if (parentEntity != null) {
					parent.databind(parentEntity);
					UI.setVisibility(parent, View.VISIBLE);
				}
			}
		}

		/* Created date */

		UI.setVisibility(createdDate, View.GONE);
		if (createdDate != null && mEntity.createdDate != null) {
			createdDate.setText(DateTime.dateStringAt(mEntity.createdDate.longValue()));
			UI.setVisibility(createdDate, View.VISIBLE);
		}

		/* Visibility */
		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected int getLayoutId() {
		return R.layout.comment_form;
	}

}