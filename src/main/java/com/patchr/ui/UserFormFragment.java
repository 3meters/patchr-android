package com.patchr.ui;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.objects.Count;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.widgets.AirImageView;
import com.patchr.ui.widgets.CandiView;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

public class UserFormFragment extends EntityFormFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Boolean currentUser = Patchr.getInstance().getCurrentUser().id.equals(mEntityId);
		mLinkProfile = currentUser ? LinkSpecType.LINKS_FOR_USER_CURRENT : LinkSpecType.LINKS_FOR_USER;
	}

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
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

	@Subscribe
	public void onViewClick(ActionEvent event) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. This lets
		 * us handle view clicks inside fragments if we want.
		 */
		if (mProcessing) return;

		if (event.view != null) {
			mProcessing = true;
			Integer id = event.view.getId();
			if (id == R.id.button_watching || id == R.id.button_created || id == R.id.button_likes) {
				onPlaceListButtonClick(event.view);
			}
			mProcessing = false;
		}
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

		Patchr.router.route(getActivity(), Route.PATCH_LIST, mEntity, extras);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ConstantConditions")
	@Override
	public void draw(final View view) {

		if (view == null) {
			Logger.w(this, "Draw called but no view");
			return;
		}

		Logger.v(this, "Draw called");

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {

				User user = (User) mEntity;

				final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
				final AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
				final TextView name = (TextView) view.findViewById(R.id.name);
				final TextView area = (TextView) view.findViewById(R.id.area);

				if (candiView != null) {
					/*
					 * This is a patch entity with a fancy image widget
					 */
					CandiView.IndicatorOptions options = new CandiView.IndicatorOptions();
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

				/* Button state */

				TextView buttonWatching = (TextView) view.findViewById(R.id.button_watching);
				TextView buttonCreated = (TextView) view.findViewById(R.id.button_created);

				Count watching = mEntity.getCount(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);
				Count created = mEntity.getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);

				buttonWatching.setText(StringManager.getString(R.string.label_user_watching)
						+ ": " + ((watching != null)
						          ? String.valueOf(watching.count.intValue())
						          : StringManager.getString(R.string.label_user_watching_none)));
				buttonCreated.setText(StringManager.getString(R.string.label_user_created)
						+ ": " + ((created != null)
						          ? String.valueOf(created.count.intValue())
						          : StringManager.getString(R.string.label_user_created_none)));
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

}