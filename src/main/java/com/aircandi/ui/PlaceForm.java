package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.ButtonSpecialEvent;
import com.aircandi.events.MessageEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Message;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
public class PlaceForm extends BaseEntityForm {

	private Boolean mDoUpsize;
	protected       Boolean         mWaitForContent  = true;
	protected       Boolean         mAutoWatch       = false;
	private   Fragment              mFragment;
	protected ToolTipRelativeLayout mTooltips;

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
			mDoUpsize = extras.getBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC);
			mAutoWatch = extras.getBoolean(Constants.EXTRA_AUTO_WATCH);
		}

		Intent intent = getIntent();
		if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
			Uri uri = intent.getData();
			if (uri != null) {
				if (uri.getPath().contains("/place/")) {
					mEntityId = uri.getPath().replace("/place/", "");
					mAutoWatch = true;
				}
				else if (uri.getPath().contains("/patch/")) {
					mEntityId = uri.getPath().replace("/patch/", "");
					mAutoWatch = true;
				}
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		mTooltips.setSingleShot(Constants.TOOLTIPS_PLACE_BROWSE_ID);

		/* Default fragment */
		mNextFragmentTag = com.aircandi.Constants.FRAGMENT_TYPE_MESSAGES;

		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;
	}

	@Override
	public void bind(final BindingMode mode) {
		if (mDoUpsize && mEntity != null) {
			mDoUpsize = false;
			upsize();
		}
		else {
			super.bind(mode);
		}
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);

		if (mAutoWatch && mEntity != null) {
			Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null) {
			    /* User is not already watching this */
				if (Patch.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
						, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
					watch(true);
				}
			}
		}

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

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAdd(Bundle extras) {

		if (Patch.getInstance().getMenuManager().canUserAdd(mEntity)) {
			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(mEntity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), mEntity.name);
			}
			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
			extras.putString(com.aircandi.Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);
			Patch.dispatch.route(this, Route.NEW, null, null, extras);
			return;
		}
		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.place_help);
		Patch.dispatch.route(this, Route.HELP, null, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {

		if (Patch.getInstance().getCurrentUser().isAnonymous()) {
			Integer messageResId = R.string.alert_signin_message_place_tune;
			Dialogs.signinRequired(this, messageResId);
			return;
		}

		if (Patch.getInstance().getMenuManager().canUserAdd(mEntity)) {
			Patch.dispatch.route(this, Route.TUNE, mEntity, null, null);
			return;
		}

		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refresh the form because something new has been added to it like a message.
		 */
		if (event.message.action.entity != null
				&& event.message.action.entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)
				&& event.message.action.entity.placeId != null
				&& event.message.action.entity.placeId.equals(mEntityId)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((BaseFragment) mCurrentFragment).bind(BindingMode.AUTO);
				}
			});
		}
	}

	//	@Override
	//	@SuppressWarnings("ucd")
	//	public void onWatchButtonClick(View view) {
	//
	//		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
	//			String message = StringManager.getString(R.string.alert_signin_message_watch, mEntity.schema);
	//			Dialogs.signinRequired(this, message);
	//			return;
	//		}
	//
	//		if (mEntity.visibility != null) {
	//
	//			/* Public place */
	//			if (mEntity.visibility.equals(Constants.VISIBILITY_PUBLIC)) {
	//				watch();
	//			}
	//
	//			/* Private place owned by current user */
	//			else if (mEntity.isOwnedByCurrentUser()) {
	//				/*
	//				 * Do nothing for now, owners always stay as watchers
	//				 */
	//			}
	//
	//			/* Private place not owned by current user */
	//			else if (!mEntity.visibleToCurrentUser()) {
	//
	//				Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
	//				if (link == null || link.enabled == null) {
	//
	//					/* User doesn't have a pending request */
	//					UI.showToastNotification(StringManager.getString(R.string.button_list_watch_request), Toast.LENGTH_SHORT);
	//				}
	//				else if (!link.enabled) {
	//
	//					/* User has a pending request */
	//					UI.showToastNotification(StringManager.getString(R.string.button_list_watch_request_cancel), Toast.LENGTH_SHORT);
	//					watch();
	//				}
	//				else if (link.enabled) {
	//
	//					/* User has an approved link */
	//					UI.showToastNotification(StringManager.getString(R.string.button_list_watch_request_cancel), Toast.LENGTH_SHORT);
	//					watch();
	//				}
	//			}
	//		}
	//	}

	@SuppressWarnings("ucd")
	public void onWatchersButtonClick(View view) {

		/* The owner of a private place is a permanent member */
		if (mEntity != null) {
			if (mEntity.visibility.equals(Constants.VISIBILITY_PUBLIC)
					|| (mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE) && mEntity.visibleToCurrentUser())) {
				Patch.dispatch.route(this, Route.WATCHERS, mEntity, null, null);
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onMapButtonClick(View view) {
		if (mEntity != null) {
			Patch.dispatch.route(this, Route.MAP, mEntity, null, null);
		}
	}

	@Subscribe
	public void onButtonSpecial(ButtonSpecialEvent event) {
		UI.setVisibility(findViewById(R.id.button_share), event.visible ? View.GONE : View.VISIBLE);
	}

	@SuppressWarnings("ucd")
	public void onAddMessageButtonClick(View view) {
		if (!mClickEnabled) return;
		if (mEntity != null) {
			mClickEnabled = false;
			onAdd(new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onShareButtonClick(View view) {
		if (mEntity != null) {
			Patch.dispatch.route(this, Route.SHARE, mEntity, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		if (mEntity != null) {
			Patch.dispatch.route(this, Route.EDIT, mEntity, null, new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		((MessageListFragment) mCurrentFragment).onHeaderClick(view);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (mCurrentFragment instanceof EntityListFragment) {

			View header = ((EntityListFragment) mCurrentFragment).getHeaderView();
			if (header != null) {

			/* Reset the image aspect ratio */
				AirImageView image = (AirImageView) header.findViewById(R.id.entity_photo);
				TypedValue typedValue = new TypedValue();
				getResources().getValue(R.dimen.aspect_ratio_place_image, typedValue, true);
				image.setAspectRatio(typedValue.getFloat());

			/* Pass the projected header height */
				final DisplayMetrics metrics = getResources().getDisplayMetrics();
				int screenWidth = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) ? metrics.widthPixels : metrics.heightPixels;
				positionButton((int) (screenWidth * typedValue.getFloat()));
			}
			else {
				positionButton(null);
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
		if (fragmentType.equals(com.aircandi.Constants.FRAGMENT_TYPE_MESSAGES)) {

			mFragment = new MessageListFragment();

			EntityMonitor monitor = new EntityMonitor(mEntityId);
			EntitiesQuery query = new EntitiesQuery();

			query.setEntityId(mEntityId)
			     .setLinkDirection(Direction.in.name())
			     .setLinkType(Constants.TYPE_LINK_CONTENT)
			     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
			     .setSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

			((EntityListFragment) mFragment)
					.setMonitor(monitor)
					.setQuery(query)
					.setListViewType(EntityListFragment.ViewType.LIST)
					.setListLayoutResId(R.layout.message_list_place_fragment)
					.setListLoadingResId(R.layout.temp_listitem_loading)
					.setListItemResId(R.layout.temp_listitem_message)
					.setListEmptyMessageResId(R.string.button_list_share)
					.setListButtonMessageResId(R.string.button_list_share)
					.setHeaderViewResId(R.layout.widget_list_header_place)
					.setFooterViewResId(R.layout.widget_list_footer_message)
					.setSelfBindingEnabled(false)
					.setButtonSpecialClickable(true);

			((BaseFragment) mFragment).getMenuResIds().add(R.menu.menu_refresh);
			((BaseFragment) mFragment).getMenuResIds().add(R.menu.menu_share_place);
			((BaseFragment) mFragment).getMenuResIds().add(R.menu.menu_edit_place);
			((BaseFragment) mFragment).getMenuResIds().add(R.menu.menu_delete);
			((BaseFragment) mFragment).getMenuResIds().add(R.menu.menu_report);
		}

		else {
			return;
		}

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_holder, mFragment);
		ft.commit();

		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
		mCurrentFragment = mFragment;
	}

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setSubject(String.format(StringManager.getString(R.string.label_place_share_subject)
				, (mEntity.name != null) ? mEntity.name : "A"));

		builder.setType("text/plain");
		builder.setText(String.format(StringManager.getString(R.string.label_place_share_body), mEntityId));
		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_place_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PLACE);

		builder.startChooser();
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
		setActivityTitle(mEntity.name);

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
			candiViewInfo.databind(mEntity, options);
		}

		drawStats(view);

		/* Place specific info */

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

		/* Creator (on info side) */

		UI.setVisibility(userView, View.GONE);
		if (userView != null) {
			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (mEntity.isOwnedBySystem()) {
					User admin = new User();
					admin.name = StringManager.getString(R.string.name_app);
					admin.id = ServiceConstants.ANONYMOUS_USER_ID;
					userView.setLabel(R.string.label_owned_by);
					userView.databind(admin);
				}
				else {
					userView.setTag(mEntity.creator);
					userView.setLabel(R.string.label_owned_by);
					userView.databind(mEntity.creator, mEntity.createdDate != null ? mEntity.createdDate.longValue() : null);
				}
				UI.setVisibility(userView, View.VISIBLE);
			}
		}

		/* Get the special button positioned initially */

		positionButton(null);

		//		final Button messageButton = (Button) findViewById(R.id.footer_holder);
		//		if (messageButton != null && mEntity != null && !TextUtils.isEmpty(mEntity.name)) {
		//			//messageButton.setLabel(String.format(StringManager.getString(R.string.button_send_message_this_place), mEntity.name));
		//		}

	}

	protected void drawBanner(View view) {

		final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) view.findViewById(R.id.entity_photo);
		final TextView name = (TextView) view.findViewById(R.id.name);
		final TextView subtitle = (TextView) view.findViewById(R.id.subtitle);

		/* Primary candi image */

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
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
				if (mEntity.subtitle != null && !mEntity.subtitle.equals("")) {
					subtitle.setText(Html.fromHtml(mEntity.subtitle));
					UI.setVisibility(subtitle, View.VISIBLE);
				}
			}
		}
	}

	protected void drawBody(View view) {

		final TextView address = (TextView) view.findViewById(R.id.candi_form_address);

		/* Place specific info */
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

	@Override
	protected void drawStats(View view) {

		TextView watchersCount = (TextView) view.findViewById(R.id.watchers_count);
		if (watchersCount != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, null, 0);
			}
			watchersCount.setText(String.valueOf(count.count.intValue()));
		}
	}

	@Override
	public void drawButtons(View view) {
		super.drawButtons(view);

		Place place = (Place) mEntity;

		/* TUNE */
		UI.setVisibility(view.findViewById(R.id.button_tune), View.GONE);

			/* Tuning buttons */
		final Boolean hasActiveProximityLink = place.hasActiveProximity();
		if (hasActiveProximityLink) {
			ComboButton button = (ComboButton) view.findViewById(R.id.button_tune);
			if (button != null) {
				button.setDrawableId(R.drawable.ic_action_signal_tuned);
			}
		}

		UI.setVisibility(view.findViewById(R.id.button_tune), View.VISIBLE);

		if (mEntity.visibility != null
				&& mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE)
				&& !mEntity.visibleToCurrentUser()) {

			UI.setVisibility(view.findViewById(R.id.button_watch), View.INVISIBLE);
			UI.setVisibility(view.findViewById(R.id.footer_holder), View.INVISIBLE);

			Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null) {
				((BaseFragment) mCurrentFragment).getButtonSpecial().setText(R.string.button_list_watch_request);
			}
			else if (!link.enabled) {
				((BaseFragment) mCurrentFragment).getButtonSpecial().setText(R.string.button_list_watch_request_cancel);
			}
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_watch), View.VISIBLE);
			UI.setVisibility(view.findViewById(R.id.footer_holder), View.VISIBLE);
		}

		UI.setVisibility(view.findViewById(R.id.button_map), View.GONE);
		UI.setVisibility(view.findViewById(R.id.button_edit), View.GONE);
		/*
		 * We can map it if we have an address or a decent location fix.
		 */
		if (!place.fuzzy || !TextUtils.isEmpty(place.address)) {
			UI.setVisibility(view.findViewById(R.id.button_map), View.VISIBLE);
		}
	}

	protected void positionButton(final Integer headerHeightProjected) {

		final View header = ((EntityListFragment) mCurrentFragment).getHeaderView();
		final Button buttonSpecial = ((EntityListFragment) mCurrentFragment).getButtonSpecial();

		if (buttonSpecial != null && header != null) {

			ViewTreeObserver vto = header.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {

					/*
					 * We don't get this right because this can happen before the image pops in so
					 * the header size changes.
					 */
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(buttonSpecial.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						int headerHeight = (headerHeightProjected != null)
						                   ? headerHeightProjected
						                   : header.getHeight();

						params.topMargin = headerHeight + UI.getRawPixelsForDisplayPixels(100f);
						buttonSpecial.setLayoutParams(params);
					}
					else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(buttonSpecial.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						Logger.i(this, "header " + header.getHeight());
						buttonSpecial.setLayoutParams(params);
					}

					if (Constants.SUPPORTS_JELLY_BEAN) {
						header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
					else {
						header.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
	}

	protected void upsize() {
		mWaitForContent = false;
		/*
		 * Upsized places do not automatically link to nearby beacons because
		 * the browsing action isn't enough of an indicator of proximity.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_upsizing);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncUpsizeSynthetic");
				final ModelResult result = Patch.getInstance().getEntityManager().upsizeSynthetic((Place) mEntity, mWaitForContent);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusy.hideBusy(false);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final Entity upsizedEntity = (Entity) result.data;
					mEntityId = upsizedEntity.id;
					mEntity = null;
					mEntityMonitor = new EntityMonitor(mEntityId);
					mFirstDraw = true;
					bind(BindingMode.AUTO);
				}
				else {
					Errors.handleError(PlaceForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected Boolean afterWatch(ModelResult result) {

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE) && !mEntity.isOwnedByCurrentUser()) {
				Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
				if (link == null) {
					((EntityListFragment) mCurrentFragment).getButtonSpecial().setText(R.string.button_list_watch_request);
					UI.showToastNotification(StringManager.getString(R.string.alert_watch_request_canceled), Toast.LENGTH_SHORT);
				}
				else if (!link.enabled) {
					((EntityListFragment) mCurrentFragment).getButtonSpecial().setText(R.string.button_list_watch_request_cancel);
					UI.showToastNotification(StringManager.getString(R.string.alert_watch_request_sent), Toast.LENGTH_SHORT);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.place_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.findItem(com.aircandi.R.id.share);
		if (menuItem != null) {
			menuItem.setVisible(Patch.getInstance().getMenuManager().showAction(Route.SHARE, mEntity, mForId));
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mTooltips.hide(false);
		return super.onOptionsItemSelected(item);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}