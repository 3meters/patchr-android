package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.BubbleButtonEvent;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingFinishedEvent;
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
import com.aircandi.utilities.Colors;
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
	protected Boolean mWaitForContent = true;
	protected Boolean mAutoWatch      = false;
	protected Boolean mJustApproved   = false;
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
		mBubbleButton.setEnabled(false);

		/* Default fragment */
		mNextFragmentTag = com.aircandi.Constants.FRAGMENT_TYPE_MESSAGES;

		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;
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
			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(mEntity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), mEntity.name);
			}
			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
			extras.putString(com.aircandi.Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);
			Patchr.dispatch.route(this, Route.NEW, null, null, extras);
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
		Patchr.dispatch.route(this, Route.HELP, null, null, extras);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final NotificationEvent event) {
		/*
		 * Refresh the form because something new has been added to it like a message.
		 */
		if ((event.notification.parentId != null && event.notification.parentId.equals(mEntityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(mEntityId))) {

			if (event.notification.event.equals("approve_watch_entity")) {
				mJustApproved = true;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					bind(BindingMode.AUTO);
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		if (mProcessing) return;
		mProcessing = true;

		if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
			mProcessing = false;
			String message = StringManager.getString(R.string.alert_signin_message_watch, mEntity.schema);
			Dialogs.signinRequired(this, message);
			return;
		}

		/* User (non-owner) wants to unwatch a private place */
		if (mEntity.privacy.equals(Constants.PRIVACY_PRIVATE)
				&& mEntity.visibleToCurrentUser()
				&& !mEntity.isOwnedByCurrentUser()) {
			final AlertDialog dialog = Dialogs.alertDialog(null
					, null
					, StringManager.getString(R.string.alert_unwatch_message)
					, null
					, this
					, R.string.alert_unwatch_positive
					, android.R.string.cancel
					, null
					, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						mJustApproved = false;
						watch(false);
					}
					else {
						mProcessing = false;
					}
				}
			}, null);
			dialog.setCanceledOnTouchOutside(false);
			return;
		}

		watch(false);
	}

	@SuppressWarnings("ucd")
	public void onWatchersButtonClick(View view) {
		if (mEntity != null) {
			Patchr.dispatch.route(this, Route.WATCHERS, mEntity, null, null);
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
			Patchr.dispatch.route(this, Route.SHARE, mEntity, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		if (mEntity != null) {
			Patchr.dispatch.route(this, Route.EDIT, mEntity, null, new Bundle());
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
		if (fragmentType.equals(com.aircandi.Constants.FRAGMENT_TYPE_MESSAGES)) {

			mCurrentFragment = new MessageListFragment();

			EntityMonitor monitor = new EntityMonitor(mEntityId);
			EntitiesQuery query = new EntitiesQuery();

			query.setEntityId(mEntityId)
			     .setLinkDirection(Direction.in.name())
			     .setLinkType(Constants.TYPE_LINK_CONTENT)
			     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
			     .setSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

			((EntityListFragment) mCurrentFragment)
					.setMonitor(monitor)
					.setQuery(query)
					.setListViewType(EntityListFragment.ViewType.LIST)
					.setListLayoutResId(R.layout.message_list_place_fragment)
					.setListLoadingResId(R.layout.temp_listitem_loading)
					.setListItemResId(R.layout.temp_listitem_message)
					.setListEmptyMessageResId(R.string.button_list_share)
					.setBubbleButtonMessageResId(R.string.button_list_share)
					.setHeaderViewResId(R.layout.widget_list_header_place)
					.setFooterViewResId(R.layout.widget_list_footer_message)
					.setSelfBindingEnabled(false);

			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_edit_place);
			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_delete);
			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report);
		}

		else {
			return;
		}

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_holder, mCurrentFragment);
		ft.commit();

		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
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
			Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null) {
			    /* User is not already watching this */
				if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
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
			candiViewInfo.databind(mEntity, options, null);
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
	}

	protected void drawBanner(View view) {

		final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
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

			UI.setVisibility(subtitle, View.GONE);
			if (subtitle != null) {
				subtitle.setText(null);
				if (!TextUtils.isEmpty(mEntity.subtitle)) {
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
			UI.setVisibility(findViewById(R.id.button_share), View.INVISIBLE);
			UI.setVisibility(view.findViewById(R.id.button_watch), View.INVISIBLE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_watch), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.button_share), View.VISIBLE);
		}

		UI.setVisibility(view.findViewById(R.id.button_map), View.GONE);
		/*
		 * We can map it if we have an address or a decent location fix.
		 */
		if (!place.fuzzy || !TextUtils.isEmpty(place.address)) {
			UI.setVisibility(view.findViewById(R.id.button_map), View.VISIBLE);
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

			Count requestCount = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, false, Direction.in);

			/* Owner */

			if (place.ownerId != null && place.ownerId.equals(Patchr.getInstance().getCurrentUser().id)) {
				/*
				 * - Member requests then alert to handle
				 * - No messages then alert to invite
				 */
				if (requestCount != null) {
					String requests = getResources().getQuantityString(R.plurals.button_pending_requests, requestCount.count.intValue(), requestCount.count.intValue());
					buttonAlert.setText(requests);
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onWatchersButtonClick(view);
						}
					});
					UI.setVisibility(alertGroup, View.VISIBLE);
				}
				else {
					buttonAlert.setText(StringManager.getString(R.string.button_list_share));
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onShareButtonClick(view);
						}
					});
					UI.setVisibility(alertGroup, View.VISIBLE);
				}
			}

			/* Member */

			else {
				if (restricted && !mEntity.visibleToCurrentUser()) {

					Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
					if (link != null && !link.enabled) {
						buttonAlert.setText(R.string.button_list_watch_request_cancel);
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onWatchButtonClick(view);
							}
						});
						UI.setVisibility(alertGroup, View.VISIBLE);
					}
					else {
						buttonAlert.setText(R.string.button_list_watch_request);
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onWatchButtonClick(view);
							}
						});
						UI.setVisibility(alertGroup, View.VISIBLE);
					}
				}
				else if (mJustApproved) {
					if (messaged) {
						buttonAlert.setText(StringManager.getString(R.string.button_just_approved));
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								mJustApproved = false;
								onShareButtonClick(view);
							}
						});
					}
					else {
						buttonAlert.setText(StringManager.getString(R.string.button_just_approved_no_message));
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								mJustApproved = false;
								onAddMessageButtonClick(view);
							}
						});
					}
					UI.setVisibility(alertGroup, View.VISIBLE);
				}
				else if (!messaged) {
					buttonAlert.setText(StringManager.getString(R.string.button_no_message));
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onAddMessageButtonClick(view);
						}
					});
					UI.setVisibility(alertGroup, View.VISIBLE);
				}
				else {
					buttonAlert.setText(StringManager.getString(R.string.button_list_share));
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onShareButtonClick(view);
						}
					});
					UI.setVisibility(alertGroup, View.VISIBLE);
				}
			}
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
				final ModelResult result = Patchr.getInstance().getEntityManager().upsizeSynthetic((Place) mEntity, mWaitForContent);
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
			menuItem.setVisible(Patchr.getInstance().getMenuManager().showAction(Route.SHARE, mEntity, mForId));
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