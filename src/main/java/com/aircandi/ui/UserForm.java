package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.MenuManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataNoopEvent;
import com.aircandi.events.DataResultEvent;
import com.aircandi.objects.Count;
import com.aircandi.objects.Link;
import com.aircandi.objects.LinkSpecType;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
@SuppressWarnings("ucd")
public class UserForm extends BaseEntityForm {

	private TextView mButtonWatching;
	private TextView mButtonCreated;
	private View     mButtonEdit;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		Boolean currentUser = Patchr.getInstance().getCurrentUser().id.equals(mEntityId);
		mLinkProfile = currentUser ? LinkSpecType.LINKS_FOR_USER_CURRENT : LinkSpecType.LINKS_FOR_USER;
		mCurrentFragment = new MessageListFragment();

		((EntityListFragment) mCurrentFragment)
				.setMonitorEntityId(mEntityId)
				.setLinkSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setLinkType(Constants.TYPE_LINK_CREATE)
				.setLinkDirection(Link.Direction.out.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_messages))
				.setListViewType(EntityListFragment.ViewType.LIST)
				.setListLayoutResId(R.layout.entity_list_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListItemResId(R.layout.temp_listitem_message)
				.setHeaderViewResId(R.layout.widget_list_header_user);


		if (!currentUser) {
			((EntityListFragment) mCurrentFragment).setSelfBind(false);
		}

		getFragmentManager().beginTransaction().add(R.id.fragment_holder, mCurrentFragment).commit();
	}


	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(DataResultEvent event) {
		super.onDataResult(event);
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe
	public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	public void onEditButtonClick(View view) {
		Patchr.router.route(this, Route.EDIT, mEntity, null);
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

		int emptyResId = 0;
		if (linkType.equals(Constants.TYPE_LINK_WATCH)) {
			emptyResId = R.string.label_watching_empty;
		}
		else if (linkType.equals(Constants.TYPE_LINK_CREATE)) {
			emptyResId = R.string.label_created_empty;
		}

		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
		extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);
		extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, emptyResId);
		extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

		Patchr.router.route(this, Route.PATCH_LIST, mEntity, extras);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void draw(View view) {

		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		mFirstDraw = false;

		User user = (User) mEntity;

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView area = (TextView) findViewById(R.id.area);

		if (candiView != null) {
			/*
			 * This is a patch entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.watchingEnabled = false;
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiView.databind(mEntity, options, null);
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

			UI.setVisibility(area, View.GONE);
			if (area != null) {
				area.setText(null);
				if (!TextUtils.isEmpty(user.area)) {
					area.setText(Html.fromHtml(user.area));
					UI.setVisibility(area, View.VISIBLE);
				}
			}
		}

		drawButtons(view);
	}

	public void drawButtons(View view) {

		mButtonWatching = (TextView) findViewById(R.id.button_watching);
		mButtonCreated = (TextView) findViewById(R.id.button_created);
		mButtonEdit = findViewById(R.id.button_edit);

		UI.setVisibility(mButtonEdit, View.GONE);
		if (MenuManager.canUserEdit(mEntity)) {
			UI.setVisibility(mButtonEdit, View.VISIBLE);
		}

		Count watching = mEntity.getCount(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);
		Count created = mEntity.getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);

		mButtonWatching.setText(StringManager.getString(R.string.label_user_watching)
				+ ": " + ((watching != null)
				          ? String.valueOf(watching.count.intValue())
				          : StringManager.getString(R.string.label_user_watching_none)));
		mButtonCreated.setText(StringManager.getString(R.string.label_user_created)
				+ ": " + ((created != null)
				          ? String.valueOf(created.count.intValue())
				          : StringManager.getString(R.string.label_user_created_none)));
	}

	@Override
	protected int getLayoutId() {
		return R.layout.user_form;
	}
}