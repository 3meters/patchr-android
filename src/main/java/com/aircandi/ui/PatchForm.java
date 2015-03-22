package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
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
import com.aircandi.components.DataController.ActionType;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.Logger;
import com.aircandi.components.MenuManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataNoopEvent;
import com.aircandi.events.DataResultEvent;
import com.aircandi.events.LinkDeleteEvent;
import com.aircandi.events.LinkInsertEvent;
import com.aircandi.events.NotificationReceivedEvent;
import com.aircandi.events.ShareCheckEvent;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkSpecType;
import com.aircandi.objects.Message;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.components.CircleTransform;
import com.aircandi.ui.components.ListController;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.Locale;

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

		mLinkProfile = LinkSpecType.LINKS_FOR_PATCH;

		mCurrentFragment = new MessageListFragment();

		((EntityListFragment) mCurrentFragment)
				.setMonitorEntityId(mEntityId)
				.setActionType(ActionType.ACTION_GET_ENTITIES)
				.setLinkSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setLinkType(Constants.TYPE_LINK_CONTENT)
				.setLinkDirection(Direction.in.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_messages))
				.setHeaderViewResId(R.layout.widget_list_header_patch)
				.setFooterViewResId(R.layout.widget_list_footer_message)
				.setListItemResId(R.layout.temp_listitem_message)
				.setListLayoutResId(R.layout.message_list_patch_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListViewType(EntityListFragment.ViewType.LIST)
				.setBubbleButtonMessageResId(R.string.button_list_share);

		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_edit_patch);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_delete);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_qrcode);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report);

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
		super.onDataResult(event); // Handles GET_ENTITY, INSERT_LIKE, DELETE_LIKE

		if (event.tag.equals(System.identityHashCode(this))
				&& (event.entity == null || event.entity.id.equals(mEntityId))) {

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (event.actionType == ActionType.ACTION_GET_ENTITY) {
						/* Not watching a restricted patch and pre-approved */
						if (mWatchStatus == WatchStatus.NONE
								&& mRestrictedForUser
								&& Type.isTrue(mPreApprovedPush)
								&& mEntity != null) {
							confirmJoin();
						}
					}
					else if (event.actionType == ActionType.ACTION_SHARE_CHECK) {
						Logger.v(this, "Data result accepted: " + event.actionType.name().toString());
						List<Link> links = (List<Link>) event.data;
						if (links != null && links.size() == 0) {
							watch(true /* activate */);
						}
						else {
							mPreApproved = true;
							confirmJoin();
						}
					}
					else if (event.actionType == ActionType.ACTION_LINK_INSERT_WATCH) {
						Logger.v(this, "Data result accepted: " + event.actionType.name().toString());
						if (((Patch) mEntity).privacy.equals(Constants.PRIVACY_PRIVATE)) {

							final boolean enabled = (!mRestrictedForUser || Type.isTrue(mPreApproved));
							if (enabled && ((Patch) mEntity).privacy.equals(Constants.PRIVACY_PRIVATE)) {
								UI.showToastNotification(StringManager.getString(R.string.alert_auto_watch), Toast.LENGTH_SHORT, Gravity.CENTER);
							}

							bind(BindingMode.MANUAL);
							if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
								((EntityListFragment) mCurrentFragment).bind(BindingMode.MANUAL);
							}
						}
					}
					else if (event.actionType == ActionType.ACTION_LINK_DELETE_WATCH) {
						Logger.v(this, "Data result accepted: " + event.actionType.name().toString());
						if (((Patch) mEntity).privacy.equals(Constants.PRIVACY_PRIVATE)) {

							bind(BindingMode.MANUAL);
							if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
								((EntityListFragment) mCurrentFragment).bind(BindingMode.MANUAL);
							}
						}
					}
				}
			});
		}
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe
	public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	@Override
	public void onProcessingComplete(final ResponseCode responseCode) {
		super.onProcessingComplete(responseCode);

		final EntityListFragment fragment = (EntityListFragment) mCurrentFragment;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/*
				 * Non-members can't add messages to private patches.
				 */
				Patch patch = (Patch) mEntity;
				ListController ls = ((EntityListFragment) mCurrentFragment).getListController();
				if (patch != null && patch.privacy != null
						&& patch.privacy.equals(Constants.PRIVACY_PRIVATE)
						&& !patch.isVisibleToCurrentUser()) {
					ls.getFloatingActionController().fadeOut();
				}
				else {
					ls.getFloatingActionController().fadeIn();
				}
			}
		});
	}

	@Subscribe
	public void onNotificationReceived(final NotificationReceivedEvent event) {
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
					if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
						((EntityListFragment) mCurrentFragment).bind(BindingMode.AUTO);
					}
				}
			});
		}
	}

	@Override
	public void onAdd(Bundle extras) {

		if (MenuManager.canUserAdd(mEntity)) {
			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(mEntity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), mEntity.name);
			}
			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
			extras.putString(Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
			Patchr.router.route(this, Route.NEW, null, extras);
			return;
		}
		if (Type.isTrue(((Patch) mEntity).locked)) {
			Dialogs.locked(this, mEntity);
		}
	}

	@SuppressWarnings("ucd")
	public void onPlaceClick(View view) {
		Entity entity = (Entity) view.getTag();
		Patchr.router.route(PatchForm.this, Route.BROWSE, entity, null);
	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		if (mEntity == null) return;

		if (!mProcessing) {
			mProcessing = true;

			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				mProcessing = false;
				String message = StringManager.getString(R.string.alert_signin_message_watch, mEntity.schema);
				Dialogs.signinRequired(this, message);
				return;
			}

			/* Cancel request */
			if (mWatchStatus == WatchStatus.WATCHING) {
				if (((Patch) mEntity).isRestrictedForCurrentUser()) {
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
	}

	@SuppressWarnings("ucd")
	public void onWatchingListButtonClick(View view) {
		if (mEntity != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_WATCH);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_watching_list);
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_watcher);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.label_watchers_empty);
			Patchr.router.route(this, Route.USER_LIST, mEntity, extras);
		}
	}

	@SuppressWarnings("ucd")
	public void onLikeButtonClick(View view) {

		if (!mProcessing) {
			mProcessing = true;

			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				mProcessing = false;
				String message = StringManager.getString(R.string.alert_signin_message_like, mEntity.schema);
				Dialogs.signinRequired(this, message);
				return;
			}

			like(mLikeStatus == LikeStatus.NONE);
		}
	}

	@SuppressWarnings("ucd")
	public void onLikesListButtonClick(View view) {
		if (mEntity != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_LIKE);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_likes_list);
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_liker);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.label_likes_empty);
			Patchr.router.route(this, Route.USER_LIST, mEntity, extras);
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onToggleDescriptionButtonClick(View view) {
		TextView description = (TextView) findViewById(R.id.description);
		Button buttonMore = (Button) findViewById(R.id.button_more);
		if (description != null) {
			int maxLines = Integers.getInteger(R.integer.max_lines_patch_description);
			boolean collapsed = ((String) buttonMore.getTag()).equals("collapsed");
			description.setMaxLines(collapsed ? Integer.MAX_VALUE : maxLines);
			buttonMore.setText(StringManager.getString(collapsed
			                                           ? R.string.button_text_collapse
			                                           : R.string.button_text_expand));
			buttonMore.setTag(collapsed ? "expanded" : "collapsed");
		}
	}

	@SuppressWarnings("ucd")
	public void onFabButtonClick(View view) {
		if (!mClickEnabled) return;
		if (mEntity != null) {
			mClickEnabled = false;
			onAdd(new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onShareButtonClick(View view) {
		if (mEntity != null) {
			Patchr.router.route(this, Route.SHARE, mEntity, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		if (mEntity != null) {
			Patchr.router.route(this, Route.EDIT, mEntity, new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		TextView description = (TextView) findViewById(R.id.description);
		Button buttonMore = (Button) findViewById(R.id.button_more);
		if (description != null) {
			boolean collapsed = ((String) buttonMore.getTag()).equals("collapsed");
			if (!collapsed) {
				onToggleDescriptionButtonClick(null);
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
				ListController ls = ((EntityListFragment) mCurrentFragment).getListController();
				ls.getMessageController().position(null, header, (int) (screenWidth * typedValue.getFloat()));
				ls.getBusyController().position(header, (int) (screenWidth * typedValue.getFloat()));
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
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
		Logger.v(this, "Draw called");
		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		mFirstDraw = false;

		/* Some state management */
		mRestrictedForUser = ((Patch) mEntity).isRestrictedForCurrentUser();
		Link linkWatching = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		Link linkLike = mEntity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
		mWatchStatus = (linkWatching == null) ? WatchStatus.NONE : (linkWatching.enabled) ? WatchStatus.WATCHING : WatchStatus.REQUESTED;
		mLikeStatus = (linkLike == null) ? LikeStatus.NONE : LikeStatus.LIKE;

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

		/*--------------------------------------------------------------------------------------------
		 * Buttons start
		 *--------------------------------------------------------------------------------------------*/

		Boolean messaged = (mEntity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);

		if (mEntity.id != null && mEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.GONE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.VISIBLE);

			/* Watch button coloring */
			ViewAnimator watch = (ViewAnimator) view.findViewById(R.id.button_watch);
			if (watch != null) {
				watch.setDisplayedChild(0);
				UI.setVisibility(watch, View.VISIBLE);
				Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
				ImageView image = (ImageView) watch.findViewById(R.id.button_image);
				if (link != null && link.enabled) {
					final int color = Colors.getColor(R.color.brand_primary);
					image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					image.setColorFilter(null);
				}
			}

			/* Like button coloring */
			ViewAnimator like = (ViewAnimator) view.findViewById(R.id.button_like);
			if (like != null) {
				like.setDisplayedChild(0);
				if (((Patch) mEntity).isVisibleToCurrentUser()) {
					Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
					ImageView image = (ImageView) like.findViewById(R.id.button_image);
					if (link != null) {
						final int color = Colors.getColor(R.color.brand_primary);
						image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
					}
					else {
						image.setColorFilter(null);
					}
					UI.setVisibility(like, View.VISIBLE);
				}
				else {
					UI.setVisibility(like, View.GONE);
				}
			}

			/* Watching count */
			View watching = view.findViewById(R.id.button_watching);
			if (watching != null) {
				Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
				if (count == null) {
					count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, null, 0);
				}
				if (count.count.intValue() > 0) {
					TextView watchingCount = (TextView) view.findViewById(R.id.watching_count);
					TextView watchingLabel = (TextView) view.findViewById(R.id.watching_label);
					if (watchingCount != null) {
						String label = getResources().getQuantityString(R.plurals.label_watching, count.count.intValue(), count.count.intValue());
						watchingCount.setText(String.valueOf(count.count.intValue()));
						watchingLabel.setText(label);
						UI.setVisibility(watching, View.VISIBLE);
					}
				}
				else {
					UI.setVisibility(watching, View.GONE);
				}
			}

			/* Like count */
			View likes = view.findViewById(R.id.button_likes);
			if (likes != null) {
				Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, null, true, Direction.in);
				if (count == null) {
					count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, null, 0);
				}
				if (count.count.intValue() > 0) {
					TextView likesCount = (TextView) view.findViewById(R.id.likes_count);
					TextView likesLabel = (TextView) view.findViewById(R.id.likes_label);
					if (likesCount != null) {
						String label = getResources().getQuantityString(R.plurals.label_likes, count.count.intValue(), count.count.intValue());
						likesCount.setText(String.valueOf(count.count.intValue()));
						likesLabel.setText(label);
						UI.setVisibility(likes, View.VISIBLE);
					}
				}
				else {
					UI.setVisibility(likes, View.GONE);
				}
			}
		}

		Patch patch = (Patch) mEntity;

		ViewGroup alertGroup = (ViewGroup) view.findViewById(R.id.alert_group);

		Boolean isPublic = (patch != null
				&& patch.privacy != null
				&& patch.privacy.equals(Constants.PRIVACY_PUBLIC)
				&& patch.isVisibleToCurrentUser());

		if (alertGroup != null) {

			TextView buttonAlert = (TextView) view.findViewById(R.id.button_alert);
			if (buttonAlert == null) return;

			Count requestCount = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, false, Direction.in);

			/* Owner */

			if (patch.ownerId != null && patch.ownerId.equals(Patchr.getInstance().getCurrentUser().id)) {
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
							onWatchingListButtonClick(view);
						}
					});
				}
				else {
					buttonAlert.setText(StringManager.getString(R.string.button_list_share));
					buttonAlert.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onShareButtonClick(view);
						}
					});
				}
			}

			/* Members and non-members */

			else {
				if (isPublic) {
					if (!messaged) {
						buttonAlert.setText(StringManager.getString(R.string.button_no_message));
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onFabButtonClick(view);
							}
						});
					}
					else {
						buttonAlert.setText(StringManager.getString(R.string.button_list_share));
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onShareButtonClick(view);
							}
						});
					}
				}
				else {
					if (mWatchStatus == WatchStatus.REQUESTED) {
						buttonAlert.setText(R.string.button_list_watch_request_cancel);
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onWatchButtonClick(view);
							}
						});
					}
					else if (mWatchStatus == WatchStatus.NONE) {
						buttonAlert.setText(R.string.button_list_watch_request);
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onWatchButtonClick(view);
							}
						});
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
									onFabButtonClick(view);
								}
							});
						}
					}
					else if (!messaged) {
						buttonAlert.setText(StringManager.getString(R.string.button_no_message));
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onFabButtonClick(view);
							}
						});
					}
					else {
						buttonAlert.setText(StringManager.getString(R.string.button_list_share));
						buttonAlert.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onShareButtonClick(view);
							}
						});
					}
				}
			}
		}

		/*--------------------------------------------------------------------------------------------
		 * Buttons end
		 *--------------------------------------------------------------------------------------------*/

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

		if (userView != null) {
			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				if (mEntity.isOwnedBySystem()) {
					User admin = new User();
					admin.name = StringManager.getString(R.string.name_app);
					admin.id = Constants.ANONYMOUS_USER_ID;
					userView.setLabel(R.string.label_owned_by);
					userView.databind(admin);
				}
				else {
					userView.setTag(mEntity.owner);
					userView.setLabel(R.string.label_owned_by);
					userView.databind(mEntity.owner, mEntity.createdDate != null ? mEntity.createdDate.longValue() : null);
				}
				UI.setVisibility(userView, View.VISIBLE);
			}
			else {
				UI.setVisibility(userView, View.GONE);
			}
		}
	}

	@Override
	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
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

	public void watch(final boolean activate) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ViewAnimator animator = (ViewAnimator) findViewById(R.id.button_watch);
				if (animator != null) {
					animator.setDisplayedChild(1);  // Turned off in drawButtons
				}
			}
		});

		final boolean enabled = (!mRestrictedForUser || Type.isTrue(mPreApproved));

		if (activate) {

			/* Used as part of link management */
			Shortcut fromShortcut = Patchr.getInstance().getCurrentUser().getAsShortcut();
			Shortcut toShortcut = mEntity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(Patchr.getInstance().getCurrentUser().id)
					.setToId(mEntity.id)
					.setType(Constants.TYPE_LINK_WATCH)
					.setEnabled(enabled)
					.setFromShortcut(fromShortcut)
					.setToShortcut(toShortcut)
					.setActionEvent(((Patch) mEntity).isVisibleToCurrentUser() ? "watch_entity_patch" : "request_watch_entity")
					.setSkipCache(false);

			update.setActionType(ActionType.ACTION_LINK_INSERT_WATCH)
			      .setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
		else {

			LinkDeleteEvent update = new LinkDeleteEvent()
					.setFromId(Patchr.getInstance().getCurrentUser().id)
					.setToId(mEntity.id)
					.setType(Constants.TYPE_LINK_WATCH)
					.setEnabled(enabled)
					.setSchema(mEntity.schema)
					.setActionEvent("unwatch_entity_" + mEntity.schema.toLowerCase(Locale.US));

			update.setActionType(ActionType.ACTION_LINK_DELETE_WATCH)
			      .setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
	}

	protected void shareCheck() {

		ShareCheckEvent event = new ShareCheckEvent()
				.setEntityId(mEntity.id)
				.setUserId(Patchr.getInstance().getCurrentUser().id);

		event.setActionType(ActionType.ACTION_SHARE_CHECK)
		     .setTag(System.identityHashCode(this));

		Dispatcher.getInstance().post(event);
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
	protected int getLayoutId() {
		return R.layout.patch_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class WatchStatus {
		public static int NONE      = 0;
		public static int WATCHING  = 1;
		public static int REQUESTED = 2;
	}
}