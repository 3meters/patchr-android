package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.BubbleButtonEvent;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
public class PlaceForm extends BaseEntityForm {

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			/*
			 * We handle upsizing if the place entity we want to browse isn't
			 * stored in the service yet.
			 */
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mBubbleButton.setEnabled(false);

		/* Default fragment */
		mNextFragmentTag = Constants.FRAGMENT_TYPE_PATCHES;

		mLinkProfile = LinkProfile.NO_LINKS;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onViewLayout() {
		/*
		 * Position bubble button initially allowing for the
		 * list header height.
		 */
		View header = ((EntityListFragment) mCurrentFragment).getHeaderView();
		if (header != null) {
			mBubbleButton.position(header, null);
		}
	}

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {

		final EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
		final Integer count = fragment.getAdapter().getCount();

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				mBusy.hideBusy(false);
				((BaseFragment) mCurrentFragment).onProcessingFinished();
				/*
				 * Non-members can't add messages to private patches.
				 */
				if (mEntity != null && mEntity.privacy != null
						&& mEntity.privacy.equals(Constants.PRIVACY_PRIVATE)
						&& !mEntity.visibleToCurrentUser()) {
					mFab.fadeOut();
				}
				else {
					mFab.fadeIn();
				}

				if (mBubbleButton.isEnabled()) {
					if (count == 0) {
						mBubbleButton.setText(fragment.getListEmptyMessageResId());
						mBubbleButton.fadeIn();
					}
					else {
						mBubbleButton.fadeOut();
					}
				}
			}
		});
	}

	@Override
	public void onAdd(Bundle extras) {

		if (Patchr.getInstance().getMenuManager().canUserAdd(mEntity)) {
			final String json = Json.objectToJson(mEntity);
			extras.putString(Constants.EXTRA_ENTITY_PARENT, json);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
			Patchr.dispatch.route(this, Route.NEW, null, null, extras);
			return;
		}
		if (Type.isTrue(mEntity.locked)) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final NotificationEvent event) {
		/*
		 * Refresh the form because something new has been added to it like a patch.
		 */
		if ((event.notification.parentId != null && event.notification.parentId.equals(mEntityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(mEntityId))) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					bind(BindingMode.AUTO);
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onMapButtonClick(View view) {
		if (mEntity != null) {
			Patchr.dispatch.route(this, Route.MAP, mEntity, null, null);
		}
	}

	@Subscribe
	public void onBubbleButton(BubbleButtonEvent event) {
		UI.setVisibility(findViewById(R.id.button_share), event.visible ? View.GONE : View.VISIBLE);
	}

	@SuppressWarnings("ucd")
	public void onAddPatchButtonClick(View view) {
		if (!mClickEnabled) return;
		if (mEntity != null) {
			mClickEnabled = false;
			onAdd(new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		((EntityListFragment) mCurrentFragment).onHeaderClick(view);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (mCurrentFragment instanceof EntityListFragment) {

			View header = ((EntityListFragment) mCurrentFragment).getHeaderView();
			if (header != null) {

				/* Reset the image aspect ratio */
				AirImageView image = (AirImageView) header.findViewById(R.id.photo);
				TypedValue typedValue = new TypedValue();
				getResources().getValue(R.dimen.aspect_ratio_place_image, typedValue, true);
				image.setAspectRatio(typedValue.getFloat());

				/* Pass the projected header height */
				final DisplayMetrics metrics = getResources().getDisplayMetrics();
				int screenWidth = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) ? metrics.widthPixels : metrics.heightPixels;
				mBubbleButton.position(header, (int) (screenWidth * typedValue.getFloat()));
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void setCurrentFragment(String fragmentType) {
		/*
		 * Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		if (fragmentType.equals(Constants.FRAGMENT_TYPE_PATCHES)) {

			mCurrentFragment = new EntityListFragment();

			EntityMonitor monitor = new EntityMonitor(mEntityId);
			EntitiesQuery query = new EntitiesQuery();

			query.setEntityId(mEntityId)
			     .setLinkDirection(Direction.in.name())
			     .setLinkType(Constants.TYPE_LINK_PROXIMITY)
			     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
			     .setSchema(Constants.SCHEMA_ENTITY_PATCH);

			((EntityListFragment) mCurrentFragment)
					.setMonitor(monitor)
					.setQuery(query)
					.setHeaderViewResId(R.layout.widget_list_header_place)
					.setFooterViewResId(R.layout.widget_list_footer_message)
					.setListEmptyMessageResId(R.string.button_list_share)
					.setListItemResId(R.layout.temp_listitem_patch_for_place)
					.setListLayoutResId(R.layout.patch_list_place_fragment)
					.setListLoadingResId(R.layout.temp_listitem_loading)
					.setListViewType(EntityListFragment.ViewType.LIST)
					.setBubbleButtonMessageResId(R.string.button_list_share)
					.setSelfBindingEnabled(false);

			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report);
		}

		else {
			return;
		}

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();

		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
		    /*
			 * In case upsizing has changed the id we original bound to.
			 */
			if (mCurrentFragment instanceof EntityListFragment) {
				EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
				((EntityMonitor) fragment.getMonitor()).setEntityId(mEntityId);
				((EntitiesQuery) fragment.getQuery()).setEntityId(mEntityId);
				if (mEntityMonitor.changed) {
					fragment.bind(BindingMode.MANUAL);
				}
				else {
					fragment.bind(mode);
				}
			}
		}
	}

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

		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		mFirstDraw = false;

		/*
		 * Drawing is broken up so sections can be selectively overridden.
		 */

		/* Photo overlayed with info */
		drawBanner(view);

		/* Buttons */
		drawButtons(view);

		final CandiView candiViewInfo = (CandiView) view.findViewById(R.id.candi_view_info);
		final TextView address = (TextView) view.findViewById(R.id.candi_form_address);
		final UserView userView = (UserView) view.findViewById(R.id.user);

		if (candiViewInfo != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiViewInfo.databind(mEntity, options, null);
		}

		drawStats(view);

		/* Patch specific info */

		final Place place = (Place) mEntity;

		UI.setVisibility(address, View.GONE);
		if (address != null) {
			String addressBlock = place.getAddressBlock();

			if (place.phone != null) {
				addressBlock += "<br/>" + place.getFormattedPhone();
			}

			if (!"".equals(addressBlock)) {
				address.setText(Html.fromHtml(addressBlock));
				UI.setVisibility(address, View.VISIBLE);
			}
		}
	}

	protected void drawBanner(View view) {

		final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
		final TextView name = (TextView) view.findViewById(R.id.name);
		final TextView category_name = (TextView) view.findViewById(R.id.category_name);

		/* Primary candi image */

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
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

			UI.setVisibility(category_name, View.GONE);
			if (category_name != null) {
				category_name.setText(null);
				if (!TextUtils.isEmpty(mEntity.subtitle)) {
					category_name.setText(Html.fromHtml(mEntity.subtitle));
					UI.setVisibility(category_name, View.VISIBLE);
				}
			}
		}
	}

	@Override
	protected void drawStats(View view) {

		TextView watchersCount = (TextView) view.findViewById(R.id.watchers_count);
		if (watchersCount != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, null, 0);
			}
			watchersCount.setText(String.valueOf(count.count.intValue()));
		}
	}

	@Override
	public void drawButtons(View view) {

		Boolean restricted = (mEntity.privacy != null && mEntity.privacy.equals(Constants.PRIVACY_PRIVATE));
		Boolean messaged = (mEntity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);

		if (mEntity.id != null && mEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.GONE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.VISIBLE);

			/* Watch button coloring */
			ComboButton watched = (ComboButton) view.findViewById(R.id.button_watch);
			if (watched != null) {
				UI.setVisibility(watched, View.VISIBLE);
				Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
				if (link != null && link.enabled) {
					final int color = Colors.getColor(R.color.brand_primary);
					watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					watched.getImageIcon().setColorFilter(null);
				}
			}
		}

		Place place = (Place) mEntity;

		if (restricted && !mEntity.visibleToCurrentUser()) {
			UI.setVisibility(view.findViewById(R.id.button_watch), View.INVISIBLE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_watch), View.VISIBLE);
		}

		ViewGroup alertGroup = (ViewGroup) view.findViewById(R.id.alert_group);
		UI.setVisibility(alertGroup, View.GONE);
		if (alertGroup != null) {

			TextView buttonAlert = (TextView) view.findViewById(R.id.button_alert);
			if (buttonAlert == null) return;

			View rule = view.findViewById(R.id.rule_alert);
			if (rule != null && Constants.SUPPORTS_KIT_KAT) {
				rule.setVisibility(View.GONE);
			}

			buttonAlert.setText(StringManager.getString(R.string.button_list_add_patch));
			buttonAlert.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onAddPatchButtonClick(view);
				}
			});

			UI.setVisibility(alertGroup, View.VISIBLE);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.place_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}