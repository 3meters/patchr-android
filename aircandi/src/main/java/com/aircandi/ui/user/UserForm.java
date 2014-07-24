package com.aircandi.ui.user;

import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.RenderDelegate;
import com.aircandi.objects.Count;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Photo;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.components.UserStats;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class UserForm extends BaseEntityForm {

	protected RenderDelegate	mDrawStats;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		Boolean currentUser = Aircandi.getInstance().getCurrentUser().id.equals(mEntityId);
		mLinkProfile = currentUser ? LinkProfile.LINKS_FOR_USER_CURRENT : LinkProfile.LINKS_FOR_USER;
		mDrawStats = new UserStats();
	}

	@Override
	public void afterDatabind(BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (mDrawStats != null) {
				loadStats();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void loadStats() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.Update);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetStats");

				/* get user stats using rest api */
				ModelResult result = Aircandi.getInstance().getEntityManager().getUserStats(mEntityId);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					((User) mEntity).stats = (List<Count>) result.data;
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						drawStats();
					}
				}
				else {
					Errors.handleError(UserForm.this, result.serviceResponse);
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {

		mFirstDraw = false;
		setActivityTitle(mEntity.name);

		User user = (User) mEntity;

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) findViewById(R.id.entity_photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);
		final TextView area = (TextView) findViewById(R.id.area);
		final TextView webUri = (TextView) findViewById(R.id.web_uri);
		final TextView bio = (TextView) findViewById(R.id.bio);

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.watchingEnabled = false;
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiView.databind(mEntity, options);
		}
		else {
			UI.setVisibility(photoView, View.GONE);
			if (photoView != null) {
				Photo photo = mEntity.getPhoto();
				UI.drawPhoto(photoView, photo);
				if (Type.isFalse(photo.usingDefault)) {
					photoView.setTag(photo);
					photoView.setClickable(true);
				}
				UI.setVisibility(photoView, View.VISIBLE);
			}

			UI.setVisibility(name, View.GONE);
			if (name != null) {
				name.setText(null);
				if (!TextUtils.isEmpty(mEntity.name)) {
					name.setText(Html.fromHtml(mEntity.name));
					UI.setVisibility(name, View.VISIBLE);
				}
			}

			UI.setVisibility(subtitle, View.GONE);
			if (subtitle != null) {
				subtitle.setText(null);
				if (!TextUtils.isEmpty(user.area)) {
					subtitle.setText(Html.fromHtml(user.area));
					UI.setVisibility(subtitle, View.VISIBLE);
				}
			}

			/* Description section */

			UI.setVisibility(findViewById(R.id.section_details), View.GONE);

			SectionLayout section = (SectionLayout) findViewById(R.id.section_details);
			if (section != null && user.name != null && !user.name.equals("")) {
				section.setHeaderTitle(Html.fromHtml(user.name).toString());
				UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
			}

			UI.setVisibility(area, View.GONE);
			if (area != null && user.area != null && !user.area.equals("")) {
				area.setText(Html.fromHtml(user.area));
				UI.setVisibility(area, View.VISIBLE);
				UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
			}

			UI.setVisibility(webUri, View.GONE);
			if (webUri != null && user.webUri != null && !user.webUri.equals("")) {
				webUri.setText(Html.fromHtml(user.webUri));
				UI.setVisibility(webUri, View.VISIBLE);
				UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
			}

			UI.setVisibility(bio, View.GONE);
			if (bio != null && user.bio != null && !user.bio.equals("")) {
				bio.setText(Html.fromHtml(user.bio));
				UI.setVisibility(bio, View.VISIBLE);
				UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
			}

			if (user.stats != null) {
				drawStats();
			}
		}

		drawButtons();

		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void drawStats() {
		if (mDrawStats != null) {
			mDrawStats.draw(mEntity, findViewById(android.R.id.content));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.user_form;
	}
}