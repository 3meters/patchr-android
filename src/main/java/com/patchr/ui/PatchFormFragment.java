package com.patchr.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.ModelResult;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.events.ShareCheckEvent;
import com.patchr.events.WatchStatusChangedEvent;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.Shortcut;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.objects.WatchStatus;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.components.AnimationFactory;
import com.patchr.ui.widgets.AirPhotoView;
import com.patchr.ui.widgets.CandiView;
import com.patchr.ui.widgets.UserView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Integers;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Locale;

public class PatchFormFragment extends EntityFormFragment {

	ViewAnimator mHeaderViewAnimator;
	protected Boolean mJustApproved = false;               // Set in onMessage via notification
	protected Integer mWatchStatus  = WatchStatus.NONE;    // Set in draw
	protected Boolean mClickEnabled = false;                        // NO_UCD (unused code)

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		mHeaderViewAnimator = (ViewAnimator) (view != null ? view.findViewById(R.id.animator_header) : null);
		return view;
	}

	@Override public void onResume() {
		super.onResume();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onViewClick(ActionEvent event) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. This lets
		 * us handle view clicks inside fragments if we want.
		 */
		if (mProcessing) return;

		if (event.view != null) {
			mProcessing = true;
			Integer id = event.view.getId();

			/* Dynamic button we need to redirect */
			if (id == R.id.button_alert) {
				id = (Integer) event.view.getTag();
			}

			if (id == R.id.header_page_one
					|| id == R.id.header_page_two) {
				onHeaderClick(event.view);
			}
			else if (id == R.id.button_watching) {
				onWatchingListButtonClick(event.view);
			}
			else if (id == R.id.button_watch) {
				onWatchButtonClick(event.view);
			}
			else if (id == R.id.button_mute) {
				onMuteButtonClick(event.view);
			}
			else if (id == R.id.button_tune) {
				onTuneButtonClick(event.view);
			}
			else if (id == R.id.share) {
				onShareButtonClick(event.view);
			}
			else if (id == R.id.button_toggle) {
				onToggleDescriptionButtonClick(event.view);
			}
			else if (id == R.id.user_photo) {
				onToggleDescriptionButtonClick(event.view);
			}
			mProcessing = false;
		}
	}

	@Subscribe public void onDataResult(final DataResultEvent event) {

		/* Can be called on background thread */
		if (event.tag.equals(System.identityHashCode(this))
				&& (event.entity == null || event.entity.id.equals(mEntityId))) {

			Logger.v(this, "Data result accepted: " + event.actionType.name().toString());

			if (event.actionType == DataController.ActionType.ACTION_LINK_INSERT_WATCH
					|| event.actionType == DataController.ActionType.ACTION_LINK_DELETE_WATCH) {
				/*
				 * Rebind to capture the users watch state from the service and then draw.
				 */
				Dispatcher.getInstance().post(new WatchStatusChangedEvent());
				onProcessingComplete();
			}
			else {
				super.onDataResult(event);
			}
		}
	}

	@Subscribe public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	protected void onWatchButtonClick(View view) {

		if (mEntity == null) return;

		if (!UserManager.getInstance().authenticated()) {
			UserManager.getInstance().showGuestGuard(getActivity(), "Sign up for a free account to watch patches and more.");
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
			watch(true /* insert */);
		}
	}

	private void onWatchingListButtonClick(View view) {
		if (mEntity != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_WATCH);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_watching_list);
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_watcher);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.label_watchers_empty);
			Patchr.router.route(getActivity(), Route.USER_LIST, mEntity, extras);
		}
	}

	private void onMuteButtonClick(View view) {
		Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		mute(link.mute == null || !link.mute);
	}

	private void onLikesListButtonClick(View view) {
		if (mEntity != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_LIKE);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_likes_list);
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_liker);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.label_likes_empty);
			Patchr.router.route(getActivity(), Route.USER_LIST, mEntity, extras);
		}
	}

	private void onTuneButtonClick(View view) {
		Patchr.router.route(getActivity(), Route.TUNE, mEntity, null);
	}

	private void onShareButtonClick(View view) {

		if (!UserManager.getInstance().authenticated()) {
			UserManager.getInstance().showGuestGuard(getActivity(), "Sign up for a free account to send patch invites and more.");
			return;
		}

		if (mEntity != null) {
			Patchr.router.route(getActivity(), Route.SHARE, mEntity, null);
		}
	}

	private void onHeaderClick(View view) {
		TextView description = (TextView) view.findViewById(R.id.description);
		Button buttonMore = (Button) view.findViewById(R.id.button_toggle);
		if (description != null) {
			boolean collapsed = ((String) buttonMore.getTag()).equals("collapsed");
			if (!collapsed) {
				onToggleDescriptionButtonClick(null);
			}
		}
		AnimationFactory.flipTransition(mHeaderViewAnimator, AnimationFactory.FlipDirection.BOTTOM_TOP, 200);
	}

	private void onToggleDescriptionButtonClick(View view) {
		if (getView() != null) {
			TextView description = (TextView) getView().findViewById(R.id.description);
			Button buttonToggle = (Button) getView().findViewById(R.id.button_toggle);
			if (description != null) {
				int maxLines = Integers.getInteger(R.integer.max_lines_patch_description);
				boolean collapsed = ((String) buttonToggle.getTag()).equals("collapsed");
				description.setMaxLines(collapsed ? Integer.MAX_VALUE : maxLines);
				buttonToggle.setText(StringManager.getString(collapsed
				                                             ? R.string.button_text_collapse
				                                             : R.string.button_text_expand));
				buttonToggle.setTag(collapsed ? "expanded" : "collapsed");
			}
		}
	}

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {
		/*
		 * Refresh the form because something new has been added to it like a message.
		 */
		if ((event.notification.parentId != null && event.notification.parentId.equals(mEntityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(mEntityId))) {

			if (event.notification.event.equals("approve_watch_entity")) {
				mJustApproved = true;
			}

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					bind(BindingMode.AUTO);
				}
			});
		}
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		/* Reset the image aspect ratio */
		if (getView() != null) {
			AirPhotoView image = (AirPhotoView) getView().findViewById(R.id.photo);
			TypedValue typedValue = new TypedValue();
			getResources().getValue(R.dimen.aspect_ratio_patch_image, typedValue, true);
			image.setAspectRatio(typedValue.getFloat());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkSpecType.LINKS_FOR_PATCH;
	}

	@Override public void draw(final View view) {

		if (view == null) {
			Logger.w(this, "Draw called but no view");
			return;
		}

		Logger.v(this, "Draw called");

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {

				/* Some state management */

				Link linkWatching = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
				mWatchStatus = ((Patch) mEntity).watchStatus();
				Boolean owner = (UserManager.getInstance().authenticated() && mEntity.ownerId != null && mEntity.ownerId.equals(UserManager.getInstance().getCurrentUser().id));

				/* Photo overlayed with info */

				final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
				final AirPhotoView photoView = (AirPhotoView) view.findViewById(R.id.photo);
				final TextView name = (TextView) view.findViewById(R.id.name);
				final TextView type = (TextView) view.findViewById(R.id.type);

				/* Primary candi image */

				if (candiView != null) {
					/*
					 * This is a patch entity with a fancy image widget
					 */
					CandiView.IndicatorOptions options = new CandiView.IndicatorOptions();
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

					UI.setVisibility(type, View.GONE);
					if (type != null) {
						type.setText(null);
						if (!TextUtils.isEmpty(mEntity.type)) {
							type.setText(Html.fromHtml(mEntity.type));
							UI.setVisibility(type, View.VISIBLE);
						}
					}
				}

				/* Buttons */

				UI.setVisibility(view.findViewById(R.id.button_tune), (owner ? View.VISIBLE : View.GONE));
				drawButtons(view);
				drawAlertGroup(view);

				final CandiView candiViewInfo = (CandiView) view.findViewById(R.id.candi_view_info);
				final TextView description = (TextView) view.findViewById(R.id.description);
				final UserView userView = (UserView) view.findViewById(R.id.user);
				final Button buttonMore = (Button) view.findViewById(R.id.button_toggle);

				if (candiViewInfo != null) {
					/*
					 * This is a patch entity with a fancy image widget
					 */
					CandiView.IndicatorOptions options = new CandiView.IndicatorOptions();
					options.showIfZero = true;
					options.imageSizePixels = 15;
					options.iconsEnabled = false;
					candiViewInfo.databind(mEntity, options, null);
				}

				/* Patch specific info */

				final Patch patch = (Patch) mEntity;

				UI.setVisibility(description, View.GONE);
				UI.setVisibility(buttonMore, View.GONE);
				if (description != null && !TextUtils.isEmpty(patch.description)) {
					description.setText(Html.fromHtml(patch.description));
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
		});
	}

	public void drawAlertGroup(View view) {
		ViewGroup alertGroup = (ViewGroup) view.findViewById(R.id.alert_group);

		if (alertGroup != null && mEntity != null) {

			Patch patch = (Patch) mEntity;
			Boolean owner = (UserManager.getInstance().authenticated() && patch.ownerId != null && patch.ownerId.equals(UserManager.getInstance().getCurrentUser().id));
			Boolean hasMessaged = (mEntity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);
			Boolean isPublic = (patch.privacy != null
					&& patch.privacy.equals(Constants.PRIVACY_PUBLIC)
					&& patch.isVisibleToCurrentUser());

			TextView buttonAlert = (TextView) view.findViewById(R.id.button_alert);
			if (buttonAlert == null) return;

			Count requestCount = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, false, Link.Direction.in);

			if (mWatchStatus == WatchStatus.NONE) {
				buttonAlert.setText(R.string.button_list_watch_request);
				buttonAlert.setTag(R.id.button_watch);
				return;
			}

			/* Owner */

			if (owner) {
				/*
				 * - Member requests then alert to handle
				 * - No messages then alert to invite
				 */
				if (requestCount != null) {
					String requests = getResources().getQuantityString(R.plurals.button_pending_requests, requestCount.count.intValue(), requestCount.count.intValue());
					buttonAlert.setText(requests);
					buttonAlert.setTag(R.id.button_watching);
				}
				else {
					buttonAlert.setText(StringManager.getString(R.string.button_list_share));
					buttonAlert.setTag(R.id.share);
				}
			}

			/* Members */

			else {
				if (isPublic) {
					if (!hasMessaged) {
						buttonAlert.setText(StringManager.getString(R.string.button_no_message));
						buttonAlert.setTag(R.id.add);
					}
					else {
						buttonAlert.setText(StringManager.getString(R.string.button_list_share));
						buttonAlert.setTag(R.id.share);
					}
				}
				else {
					if (mWatchStatus == WatchStatus.REQUESTED) {
						buttonAlert.setText(R.string.button_list_watch_request_cancel);
						buttonAlert.setTag(R.id.button_watch);
					}
					else if (mWatchStatus == WatchStatus.WATCHING) {
						if (!hasMessaged) {
							buttonAlert.setText(StringManager.getString(R.string.button_no_message));
							buttonAlert.setTag(R.id.add);
						}
						else {
							buttonAlert.setText(StringManager.getString(R.string.button_list_share));
							buttonAlert.setTag(R.id.share);
						}
					}

					if (mJustApproved) {  // We add a little sugar by using a flag set by an 'approved' notification
						if (hasMessaged) {
							buttonAlert.setText(StringManager.getString(R.string.button_just_approved));
							buttonAlert.setTag(R.id.share);
						}
						else {
							buttonAlert.setText(StringManager.getString(R.string.button_just_approved_no_message));
							buttonAlert.setTag(R.id.add);
						}
					}
				}
			}
		}
	}

	public void watch(final boolean activate) {

		final boolean enabled = !(((Patch) mEntity).isRestrictedForCurrentUser());

		if (activate) {

			/* Used as part of link management */
			Shortcut fromShortcut = UserManager.getInstance().getCurrentUser().getAsShortcut();
			Shortcut toShortcut = mEntity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(UserManager.getInstance().getCurrentUser().id)
					.setToId(mEntity.id)
					.setType(Constants.TYPE_LINK_WATCH)
					.setEnabled(enabled)
					.setFromShortcut(fromShortcut)
					.setToShortcut(toShortcut)
					.setActionEvent(((Patch) mEntity).isVisibleToCurrentUser() ? "watch_entity_patch" : "request_watch_entity")
					.setSkipCache(false);

			update.setActionType(DataController.ActionType.ACTION_LINK_INSERT_WATCH)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
		else {

			LinkDeleteEvent update = new LinkDeleteEvent()
					.setFromId(UserManager.getInstance().getCurrentUser().id)
					.setToId(mEntity.id)
					.setType(Constants.TYPE_LINK_WATCH)
					.setEnabled(enabled)
					.setSchema(mEntity.schema)
					.setActionEvent("unwatch_entity_" + mEntity.schema.toLowerCase(Locale.US));

			update.setActionType(DataController.ActionType.ACTION_LINK_DELETE_WATCH)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
	}

	public void mute(final Boolean mute) {

		final Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		final String actionEvent = mute ? "mute_watch_entity" : "unmute_watch_entity";

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (getView() != null) {
					ViewAnimator animator = (ViewAnimator) getView().findViewById(R.id.button_mute);
					if (animator != null) {
						animator.setDisplayedChild(1);  // Turned off in drawButtons
					}
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncMuteLink");
				ModelResult result = DataController.getInstance().muteLink(link.id, mute, actionEvent);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				bind(BindingMode.AUTO);
				onProcessingComplete(); // Updates ui like floating button
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override public void configureStandardMenuItems(final Menu menu) {

		super.configureStandardMenuItems(menu);

		FragmentActivity fragmentActivity = getActivity();
		if (menu == null || fragmentActivity == null) return;

		fragmentActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				MenuItem menuItem = menu.findItem(R.id.leave_patch);
				if (menuItem != null) {
					if (mEntity != null) {
						Integer watchStatus = ((Patch) mEntity).watchStatus();
						menuItem.setVisible(watchStatus == WatchStatus.WATCHING);
					}
				}
			}
		});
	}

	protected void shareCheck() {

		ShareCheckEvent event = new ShareCheckEvent()
				.setEntityId(mEntity.id)
				.setUserId(UserManager.getInstance().getCurrentUser().id);

		event.setActionType(DataController.ActionType.ACTION_SHARE_CHECK)
				.setTag(System.identityHashCode(this));

		Dispatcher.getInstance().post(event);
	}

	protected void confirmJoin() {

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final AlertDialog dialog = Dialogs.alertDialog(null
						, null
						, StringManager.getString(R.string.alert_autowatch_message)
						, null
						, getActivity()
						, R.string.alert_autowatch_positive
						, R.string.alert_autowatch_negative
						, null
						, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							watch(true /* activate */);
						}
					}
				}, null);
				dialog.setCanceledOnTouchOutside(false);
			}
		});
	}

	protected void confirmLeave() {
		/* User (non-owner) wants to unwatch a private patch */
		final AlertDialog dialog = Dialogs.alertDialog(null
				, null
				, StringManager.getString(R.string.alert_unwatch_message)
				, null
				, getActivity()
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
			}
		}, null);
		dialog.setCanceledOnTouchOutside(false);
	}
}