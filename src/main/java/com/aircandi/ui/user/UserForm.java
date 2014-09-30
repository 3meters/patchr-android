package com.aircandi.ui.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Count;
import com.aircandi.objects.Link;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.MessageListFragment;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

@SuppressLint("Registered")
@SuppressWarnings("ucd")
public class UserForm extends BaseEntityForm {

	private EntityListFragment mListFragment;
	private TextView           mButtonWatching;
	private TextView           mButtonCreated;
	private View               mButtonEdit;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		Boolean currentUser = Patch.getInstance().getCurrentUser().id.equals(mEntityId);
		mLinkProfile = currentUser ? LinkProfile.LINKS_FOR_USER_CURRENT : LinkProfile.LINKS_FOR_USER;
		mListFragment = new MessageListFragment();

		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Link.Direction.out.name())
		     .setLinkType(Constants.TYPE_LINK_CREATE)
		     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
		     .setSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListViewType(EntityListFragment.ViewType.LIST)
		             .setListLayoutResId(R.layout.entity_list_fragment)
		             .setListLoadingResId(R.layout.temp_listitem_loading)
		             .setListItemResId(R.layout.temp_listitem_message)
		             .setListEmptyMessageResId(R.string.label_sent_empty)
		             .setHeaderViewResId(R.layout.widget_list_header_user)
		             .setSelfBindingEnabled(false);

		getFragmentManager().beginTransaction().add(R.id.fragment_holder, mListFragment).commit();
	}

	@Override
	public void afterDatabind(BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);

		Boolean currentUser = Patch.getInstance().getCurrentUser().id.equals(mEntityId);
		if (!currentUser) return;
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (mEntityMonitor.changed) {
				mListFragment.bind(BindingMode.MANUAL);
			}
			else {
				mListFragment.bind(mode);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	public void onEditButtonClick(View view) {
		Patch.dispatch.route(this, Route.EDIT, mEntity, null, null);
	}

	public void onPlaceListButtonClick(View view) {

		String linkType = (String) view.getTag();
		int titleResId = 0;
		if (linkType.equals(Constants.TYPE_LINK_WATCH)) {
			titleResId = R.string.label_drawer_item_watch;
		}
		else if (linkType.equals(Constants.TYPE_LINK_CREATE)) {
			titleResId = R.string.label_drawer_item_create;
		}

		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
		extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);

		Patch.dispatch.route(this, Route.PLACE_LIST, mEntity, null, extras);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void drawButtons(View view) {
		super.drawButtons(view);

		mButtonWatching = (TextView) findViewById(R.id.button_watching);
		mButtonCreated = (TextView) findViewById(R.id.button_created);
		mButtonEdit = findViewById(R.id.button_edit);

		UI.setVisibility(mButtonEdit, View.GONE);
		if (Patch.getInstance().getMenuManager().canUserEdit(mEntity)) {
			UI.setVisibility(mButtonEdit, View.VISIBLE);
		}

		Count watching = mEntity.getCount(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, Link.Direction.out);
		Count created = mEntity.getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, Link.Direction.out);

		if (watching != null) {
			mButtonWatching.setText(StringManager.getString(R.string.label_user_watching) + ": " + String.valueOf(watching.count.intValue()));
		}
		if (created != null) {
			mButtonCreated.setText(StringManager.getString(R.string.label_user_created) + ": " + String.valueOf(created.count.intValue()));
		}
	}

	//	protected void loadStats() {
	//
	//		new AsyncTask() {
	//
	//			@Override
	//			protected void onPreExecute() {
	//				mBusy.showBusy(BusyAction.Update);
	//			}
	//
	//			@Override
	//			protected Object doInBackground(Object... params) {
	//				Thread.currentThread().setName("AsyncGetStats");
	//
	//				/* get user stats using rest api */
	//				ModelResult result = Patch.getInstance().getEntityManager().getUserStats(mEntityId);
	//				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
	//					((User) mEntity).stats = (List<Count>) result.data;
	//				}
	//				return result;
	//			}
	//
	//			@Override
	//			protected void onPostExecute(Object modelResult) {
	//				final ModelResult result = (ModelResult) modelResult;
	//
	//				mBusy.hideBusy(false);
	//				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
	//					if (result.data != null) {
	//						drawStats(null);
	//					}
	//				}
	//				else {
	//					Errors.handleError(UserForm.this, result.serviceResponse);
	//				}
	//			}
	//		}.execute();
	//	}

	/*--------------------------------------------------------------------------------------------
	 * UI routines
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void draw(View view) {

		if (view == null) {
			view = findViewById(android.R.id.content);
		}
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
				drawStats(view);
			}
		}

		drawButtons(view);
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.user_form;
	}
}