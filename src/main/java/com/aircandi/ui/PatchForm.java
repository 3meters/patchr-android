package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
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
import com.aircandi.objects.Message;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.User;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.components.CircleTransform;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.List;

@SuppressLint("Registered")
public class PatchForm extends BaseEntityForm {

	protected Boolean mPreApproved       = null;                // Set from intent or by first watch request
	protected Boolean mPreApprovedPush   = null;                // Pre-approval push passed in
	protected Boolean mJustApproved      = false;               // Set in onMessage via notification
	protected Boolean mRestrictedForUser = false;               // Set in draw
	protected Integer mWatchStatus       = WatchStatus.NONE;    // Set in draw

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			/*
			 * We handle upsizing if the patch entity we want to browse isn't
			 * stored in the service yet.
			 */
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
			mPreApprovedPush = extras.getBoolean(Constants.EXTRA_PRE_APPROVED);  // Defaults to false
		}

		Intent intent = getIntent();
		if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
			Uri uri = intent.getData();
			if (uri != null) {
				if (uri.getPath().contains("/patch/")) {
					mEntityId = uri.getPath().replace("/patch/", "");
					mPreApprovedPush = true;
				}
				else if (uri.getPath().contains("/patch/")) {
					mEntityId = uri.getPath().replace("/patch/", "");
					mPreApprovedPush = true;
				}
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEmptyView.setEnabled(false);

		/* Default fragment */
		mNextFragmentTag = Constants.FRAGMENT_TYPE_MESSAGES;

		mLinkProfile = LinkProfile.LINKS_FOR_PATCH;
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
			mEmptyView.position(header, null);
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
						&& !mEntity.isVisibleToCurrentUser()) {
					mFab.fadeOut();
				}
				else {
					mFab.fadeIn();
				}

				if (mEmptyView.isEnabled()) {
					if (count == 0) {
						mEmptyView.setText(fragment.getListEmptyMessageResId());
						mEmptyView.fadeIn();
					}
					else {
						mEmptyView.fadeOut();
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
			extras.putString(Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
			Patchr.dispatch.route(this, Route.NEW, null, extras);
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
	public void onPlaceClick(View view) {
		Entity entity = (Entity) view.getTag();
		Patchr.dispatch.route(PatchForm.this, Route.BROWSE, entity, null);
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

		/* Cancel request */
		if (mWatchStatus == WatchStatus.WATCHING) {
			if (mEntity.isRestrictedForCurrentUser()) {
				confirmLeave();
			}
			else {
				watch(false /* delete */);
			}
		}
		else if (mWatchStatus == WatchStatus.REQUESTED) {
			watch(false /* delete */);
		}
		else if (mWatchStatus == WatchStatus.NONE) {
			if (mPreApproved == null) {
				shareCheck();   // Checks for pre-approved and if true then chains to confirmJoin else watch
			}
			else if (mPreApproved) {
				confirmJoin(); // Chains to watch() if user confirms else ends
			}
			else {
				watch(true /* insert */);
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onWatchersButtonClick(View view) {
		if (mEntity != null) {
			Patchr.dispatch.route(this, Route.WATCHERS, mEntity, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onExpandDescriptionButtonClick(View view) {
		TextView description = (TextView) findViewById(R.id.description);
		Button buttonMore = (Button) findViewById(R.id.button_more);
		if (description != null) {
			int maxLines = Integers.getInteger(R.integer.max_lines_patch_description);
			boolean collapsed = ((String)buttonMore.getTag()).equals("collapsed");
			description.setMaxLines(collapsed ? Integer.MAX_VALUE : maxLines);
			buttonMore.setText(StringManager.getString(collapsed
			                                           ? R.string.button_text_collapse
			                                           : R.string.button_text_expand));
			buttonMore.setTag(collapsed ? "expanded" : "collapsed");
		}
	}

	@Subscribe
	public void onBubbleButton(BubbleButtonEvent event) {}

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
			Patchr.dispatch.route(this, Route.SHARE, mEntity, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		if (mEntity != null) {
			Patchr.dispatch.route(this, Route.EDIT, mEntity, new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		TextView description = (TextView) findViewById(R.id.description);
		Button buttonMore = (Button) findViewById(R.id.button_more);
		if (description != null) {
			int maxLines = Integers.getInteger(R.integer.max_lines_patch_description);
			boolean collapsed = ((String)buttonMore.getTag()).equals("collapsed");
			if (!collapsed) {
				onExpandDescriptionButtonClick(null);
			}
		}

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
				mEmptyView.position(header, (int) (screenWidth * typedValue.getFloat()));
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ConstantConditions")
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
		Logger.v(this, "Draw called");
		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		mFirstDraw = false;

		/* Some state management */
		Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		mRestrictedForUser = mEntity.isRestrictedForCurrentUser();
		mWatchStatus = (link == null) ? WatchStatus.NONE : (link.enabled) ? WatchStatus.WATCHING : WatchStatus.REQUESTED;

		final View holderPlace = view.findViewById(R.id.holder_place);
		final AirImageView placePhotoView = (AirImageView) view.findViewById(R.id.place_photo);
		final TextView placeName = (TextView) view.findViewById(R.id.place_name);

		/* Message place context */

		UI.setVisibility(holderPlace, View.GONE);
		UI.setVisibility(placePhotoView, View.GONE);
		if (holderPlace != null) {
			Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);
			if (linkPlace != null) {
				holderPlace.setTag(linkPlace.shortcut.getAsEntity());

				/* Name */
				placeName.setText(linkPlace.shortcut.name);
				UI.setVisibility(holderPlace, View.VISIBLE);

				/* Photo */
				Photo photo = linkPlace.shortcut.photo;
				if (photo != null) {
					if (placePhotoView.getPhoto() == null || !placePhotoView.getPhoto().getUri().equals(photo.getUri())) {
						placePhotoView.setTag(linkPlace.shortcut.getAsEntity());
						UI.drawPhoto(placePhotoView, photo, new CircleTransform());
					}
					UI.setVisibility(placePhotoView, View.VISIBLE);
				}
			}
		}

		/* Photo overlayed with info */

		final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
		final TextView name = (TextView) view.findViewById(R.id.name);
		final TextView category_name = (TextView) view.findViewById(R.id.category_name);

		/* Primary candi image */

		if (candiView != null) {
			/*
			 * This is a patch entity with a fancy image widget
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

		/* Buttons */
		drawButtons(view);

		final CandiView candiViewInfo = (CandiView) view.findViewById(R.id.candi_view_info);
		final TextView description = (TextView) view.findViewById(R.id.description);
		final UserView userView = (UserView) view.findViewById(R.id.user);
		final Button buttonMore = (Button) view.findViewById(R.id.button_more);

		if (candiViewInfo != null) {
			/*
			 * This is a patch entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiViewInfo.databind(mEntity, options, null);
		}

		/* Patch specific info */

		final Patch place = (Patch) mEntity;

		UI.setVisibility(description, View.GONE);
		UI.setVisibility(buttonMore, View.GONE);
		if (description != null && !TextUtils.isEmpty(place.description)) {
			description.setText(Html.fromHtml(place.description));
			UI.setVisibility(description, View.VISIBLE);
			buttonMore.setText(StringManager.getString(R.string.button_text_expand));
			//if (description.getLineCount() > 3) {
			UI.setVisibility(buttonMore, View.VISIBLE);
			//}
		}

		/* Creator (on info side) */

		UI.setVisibility(userView, View.GONE);
		if (userView != null) {
			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
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

	@Override
	public void drawButtons(View view) {

		Boolean messaged = (mEntity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);

		if (mEntity.id != null && mEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.GONE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.VISIBLE);

			/* Watch button coloring */
			ViewAnimator watched = (ViewAnimator) view.findViewById(R.id.button_watch);
			if (watched != null) {
				UI.setVisibility(watched, View.VISIBLE);
				Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
				ImageView image = (ImageView) watched.findViewById(R.id.button_image);
				if (link != null && link.enabled) {
					final int color = Colors.getColor(R.color.brand_primary);
					image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					image.setColorFilter(null);
				}
			}

			TextView watchersCount = (TextView) view.findViewById(R.id.watchers_count);
			if (watchersCount != null) {
				Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
				if (count == null) {
					count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, null, 0);
				}
				watchersCount.setText(String.valueOf(count.count.intValue()));
			}
		}

		Patch place = (Patch) mEntity;

		ViewGroup alertGroup = (ViewGroup) view.findViewById(R.id.alert_group);
		UI.setVisibility(alertGroup, View.GONE);
		if (alertGroup != null) {

			TextView buttonAlert = (TextView) view.findViewById(R.id.button_alert);
			if (buttonAlert == null) return;

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

			/* Members and non-members */

			else {
				if (mWatchStatus == WatchStatus.REQUESTED) {
					buttonAlert.setText(R.string.button_list_watch_request_cancel);
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onWatchButtonClick(view);
						}
					});
					UI.setVisibility(alertGroup, View.VISIBLE);
				}
				else if (mWatchStatus == WatchStatus.NONE) {
					buttonAlert.setText(R.string.button_list_watch_request);
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onWatchButtonClick(view);
						}
					});
					UI.setVisibility(alertGroup, View.VISIBLE);
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

	@Override
	public void setCurrentFragment(String fragmentType) {
		/*
		 * Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		if (fragmentType.equals(Constants.FRAGMENT_TYPE_MESSAGES)) {

			mCurrentFragment = new MessageListFragment();

			EntityMonitor monitor = new EntityMonitor(mEntityId);
			EntitiesQuery query = new EntitiesQuery();

			query.setEntityId(mEntityId)
			     .setLinkDirection(Direction.in.name())
			     .setLinkType(Constants.TYPE_LINK_CONTENT)
			     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
			     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

			((EntityListFragment) mCurrentFragment)
					.setMonitor(monitor)
					.setQuery(query)
					.setHeaderViewResId(R.layout.widget_list_header_patch)
					.setFooterViewResId(R.layout.widget_list_footer_message)
					.setListEmptyMessageResId(R.string.button_list_share)
					.setListItemResId(R.layout.temp_listitem_message)
					.setListLayoutResId(R.layout.message_list_patch_fragment)
					.setListLoadingResId(R.layout.temp_listitem_loading)
					.setListViewType(EntityListFragment.ViewType.LIST)
					.setBubbleButtonMessageResId(R.string.button_list_share)
					.setSelfBindingEnabled(false);

			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_edit_patch);
			((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_delete);
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

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	public void watch(final boolean activate) {

		final boolean enabled = (!mRestrictedForUser || Type.isTrue(mPreApproved));

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				((ViewAnimator) findViewById(R.id.button_watch)).setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncWatchEntity");
				ModelResult result;
				Patchr.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();

				if (activate) {

					/* Used as part of link management */
					Shortcut fromShortcut = Patchr.getInstance().getCurrentUser().getAsShortcut();
					Shortcut toShortcut = mEntity.getAsShortcut();

					result = Patchr.getInstance().getEntityManager().insertLink(null
							, Patchr.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, enabled
							, fromShortcut
							, toShortcut
							, mEntity.isVisibleToCurrentUser() ? "watch_entity_place" : "request_watch_entity"
							, false);
				}
				else {
					result = Patchr.getInstance().getEntityManager().deleteLink(Patchr.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, enabled
							, mEntity.schema
							, "unwatch_entity_" + mEntity.schema);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;

				((ViewAnimator) findViewById(R.id.button_watch)).setDisplayedChild(0);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					bind(BindingMode.AUTO); // Triggers redraw including buttons and updates state
					//					View view = findViewById(android.R.id.content);
					//					drawButtons(view);
					if (activate && enabled && mEntity.privacy.equals(Constants.PRIVACY_PRIVATE)) {
						UI.showToastNotification(StringManager.getString(R.string.alert_auto_watch), Toast.LENGTH_SHORT, Gravity.CENTER);
					}
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != ServiceConstants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(PatchForm.this, result.serviceResponse);
					}
				}
				mProcessing = false;
			}
		}.execute();
	}

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setSubject(String.format(StringManager.getString(R.string.label_patch_share_subject)
				, (mEntity.name != null) ? mEntity.name : "A"));

		builder.setType("text/plain");
		builder.setText(String.format(StringManager.getString(R.string.label_patch_share_body), mEntityId));
		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_patch_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

		builder.startChooser();
	}

	protected void shareCheck() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncShareCheck");
				ModelResult result = Patchr.getInstance().getEntityManager().checkShare(mEntity.id, Patchr.getInstance().getCurrentUser().id);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					List<Link> links = (List<Link>) result.data;
					if (links != null && links.size() == 0) {
						watch(true /* activate */);
					}
					else {
						mPreApproved = true;
						confirmJoin();
					}
				}
			}
		}.execute();
	}

	protected void confirmJoin() {
		final AlertDialog dialog = Dialogs.alertDialog(null
				, null
				, StringManager.getString(R.string.alert_autowatch_message)
				, null
				, this
				, R.string.alert_autowatch_positive
				, R.string.alert_autowatch_negative
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					watch(true /* activate */);
				}
				else {
					mProcessing = false;
				}
			}
		}, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	protected void confirmLeave() {
		/* User (non-owner) wants to unwatch a private patch */
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
					watch(false /* delete */);
				}
				else {
					mProcessing = false;
				}
			}
		}, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);

		/* Not watching a restricted patch and pre-approved */
		if (mWatchStatus == WatchStatus.NONE && mRestrictedForUser && Type.isTrue(mPreApprovedPush) && mEntity != null) {
			confirmJoin();
			return;
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

	@Override
	protected int getLayoutId() {
		return R.layout.patch_form;
	}

	public static class WatchStatus {
		public static int NONE      = 0;
		public static int WATCHING  = 1;
		public static int REQUESTED = 2;
	}
}