package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.StringManager;
import com.aircandi.events.MessageEvent;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Count;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.List;

@SuppressLint("Registered")
public class PictureForm extends BaseEntityForm {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_PICTURE;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refresh the form because something new has been added to it
		 * like a comment or picture.
		 */
		if (event.message.action.toEntity != null && mEntityId.equals(event.message.action.toEntity.id)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	@Override
	public void onAdd(Bundle extras) {
		Aircandi.dispatch.route(this, Route.NEW_PICKER, mEntity, null, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * UI
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void draw() {
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
		setActivityTitle(mEntity.name);

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) findViewById(R.id.entity_photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);
		final TextView description = (TextView) findViewById(R.id.description);

		final AirImageView parentPhotoView = (AirImageView) findViewById(R.id.parent_photo);
		final TextView parentName = (TextView) findViewById(R.id.parent_name);
		final TextView parentLabel = (TextView) findViewById(R.id.parent_label);

		final EntityView placeView = (EntityView) findViewById(R.id.place);

		final UserView user_one = (UserView) findViewById(R.id.user_one);
		final UserView user_two = (UserView) findViewById(R.id.user_two);

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			candiView.databind(mEntity, new IndicatorOptions());
		}
		else {

			/* Photo */

			UI.setVisibility(photoView, View.GONE);
			if (photoView != null) {

				int screenWidthDp = (int) UI.getScreenWidthDisplayPixels(this);
				int widgetWidthDp = 122;
				if (screenWidthDp - widgetWidthDp <= 264) {
					int photoViewWidth = UI.getRawPixelsForDisplayPixels((float) (screenWidthDp - widgetWidthDp));
					RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(photoViewWidth, photoViewWidth);
					photoView.setLayoutParams(paramsImage);
				}

				if (!Photo.same(photoView.getPhoto(), mEntity.getPhoto())) {
					Photo photo = mEntity.getPhoto();
					UI.drawPhoto(photoView, photo);
					if (Type.isFalse(photo.usingDefault)) {
						photoView.setClickable(true);
					}
				}
				UI.setVisibility(photoView, View.VISIBLE);
			}

			/* Name */

			UI.setVisibility(name, View.GONE);
			if (name != null) {
				name.setText(null);
				if (mEntity.name != null && !mEntity.name.equals("")) {
					name.setText(Html.fromHtml(mEntity.name));
					UI.setVisibility(name, View.VISIBLE);
				}
			}

			/* Subtitle */

			UI.setVisibility(subtitle, View.GONE);
			if (subtitle != null) {
				subtitle.setText(null);
				if (mEntity.subtitle != null && !mEntity.subtitle.equals("")) {
					subtitle.setText(Html.fromHtml(mEntity.subtitle));
					UI.setVisibility(subtitle, View.VISIBLE);
				}
			}
		}

		/* Description */

		UI.setVisibility(findViewById(R.id.section_description), View.GONE);
		if (description != null) {
			description.setText(null);
			if (mEntity.description != null && !mEntity.description.equals("")) {
				description.setText(Html.fromHtml(mEntity.description));
				UI.setVisibility(findViewById(R.id.section_description), View.VISIBLE);
			}
		}

		/*
		 * Parent context
		 */

		View parentHolder = findViewById(R.id.parent_holder);
		UI.setVisibility(parentHolder, View.GONE);
		Link link = mEntity.getParentLink(null, null);
		if (link != null && link.shortcut != null) {

			if (parentPhotoView != null) {
				Photo photo = link.shortcut.getPhoto();
				UI.drawPhoto(parentPhotoView, photo);
				UI.setVisibility(parentPhotoView, View.VISIBLE);
			}

			UI.setVisibility(parentName, View.GONE);
			if (parentName != null && !TextUtils.isEmpty(link.shortcut.name)) {
				parentName.setText(Html.fromHtml(link.shortcut.name));
				UI.setVisibility(parentName, View.VISIBLE);
			}

			parentLabel.setText(StringManager.getString(R.string.label_picture_parent, link.shortcut.schema));
			parentHolder.setTag(link.shortcut);

			UI.setVisibility(parentHolder, View.VISIBLE);
		}

		/* Ambient place context */

		UI.setVisibility(placeView, View.GONE);
		if (placeView != null) {
			if (mEntity.place != null) {
				placeView.setLabel(R.string.label_picture_for_place);
				placeView.databind(mEntity.place);
				UI.setVisibility(placeView, View.VISIBLE);
			}
		}

		/* Stats */

		drawStats();

		/* Shortcuts */

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.holder_shortcuts)).removeAllViews();

		/* Synthetic applink shortcuts */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, true, true);
		settings.appClass = Applink.class;
		List<Shortcut> shortcuts = mEntity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
			prepareShortcuts(shortcuts
					, settings
					, R.string.label_section_applinks
					, R.string.label_link_links_more
					, mResources.getInteger(R.integer.limit_shortcuts_flow)
					, R.id.holder_shortcuts
					, R.layout.widget_shortcut);
		}

		/* Service applink shortcuts */
		settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, true);
		settings.appClass = Applink.class;
		shortcuts = mEntity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
			prepareShortcuts(shortcuts
					, settings
					, null
					, R.string.label_link_links_more
					, mResources.getInteger(R.integer.limit_shortcuts_flow)
					, R.id.holder_shortcuts
					, R.layout.widget_shortcut);
		}

		/* Creator block */

		UI.setVisibility(user_one, View.GONE);
		UI.setVisibility(user_two, View.GONE);
		UserView user = user_one;

		if (user != null
				&& mEntity.creator != null
				&& !mEntity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (((Place) mEntity).getProvider().type.equals("aircandi")) {
					user.setLabel(R.string.label_created_by);
					user.databind(mEntity.creator, mEntity.createdDate.longValue());
					UI.setVisibility(user, View.VISIBLE);
					user = user_two;
				}
			}
			else {
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					user.setLabel(R.string.label_added_by);
				}
				else {
					user.setLabel(R.string.label_created_by);
				}
				user.databind(mEntity.creator, mEntity.createdDate.longValue());
				UI.setVisibility(user_one, View.VISIBLE);
				user = user_two;
			}
		}

		/* Editor block */

		if (user != null && mEntity.modifier != null
				&& !mEntity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.modifier.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
			if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
				user.setLabel(R.string.label_edited_by);
				user.databind(mEntity.modifier, mEntity.modifiedDate.longValue());
				UI.setVisibility(user, View.VISIBLE);
			}
		}

		/* Buttons */
		drawButtons();

		/* Visibility */
		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void drawButtons() {
		super.drawButtons();
	}

	@Override
	protected void drawStats() {

		TextView watchingStats = (TextView) findViewById(R.id.watching_stats);
		if (watchingStats != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, null, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, null, 0);
			}
			watchingStats.setText(String.valueOf(count.count));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected int getLayoutId() {
		return R.layout.picture_form;
	}

}