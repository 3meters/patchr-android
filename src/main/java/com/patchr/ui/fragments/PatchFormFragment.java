package com.patchr.ui.fragments;

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
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataQueryResultEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.events.ShareCheckEvent;
import com.patchr.events.WatchStatusChangedEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Count;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.objects.Shortcut;
import com.patchr.objects.WatchStatus;
import com.patchr.ui.views.CandiView;
import com.patchr.ui.views.ImageLayout;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

public class PatchFormFragment extends EntityFormFragment {

	ViewAnimator mHeaderViewAnimator;
	protected Boolean mJustApproved = false;               // Set in onMessage via notification
	protected Integer mWatchStatus  = WatchStatus.NONE;    // Set in draw
	protected Boolean mClickEnabled = false;                        // NO_UCD (unused code)

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(mLayoutResId, container, false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/


	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onDataResult(final DataQueryResultEvent event) {

		/* Can be called on background thread */
		if (event.tag.equals(System.identityHashCode(this))
				&& (event.entity == null || event.entity.id.equals(mEntityId))) {

			Logger.v(this, "Data result accepted: " + event.actionType.name());

			if (event.actionType == ActionType.ACTION_LINK_INSERT_WATCH
					|| event.actionType == ActionType.ACTION_LINK_DELETE_WATCH) {
				/*
				 * Rebind to capture the users watch state from the service and then draw.
				 */
				int watchStatus = (event.actionType == ActionType.ACTION_LINK_INSERT_WATCH) ? WatchStatus.REQUESTED : WatchStatus.NONE;
				Dispatcher.getInstance().post(new WatchStatusChangedEvent(watchStatus));
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
					bind(FetchMode.AUTO);
				}
			});
		}
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		/* Reset the image aspect ratio */
		if (getView() != null) {
			ImageLayout image = (ImageLayout) getView().findViewById(R.id.photo);
			TypedValue typedValue = new TypedValue();
			getResources().getValue(R.dimen.aspect_ratio_patch_image, typedValue, true);
			image.setAspectRatio(typedValue.getFloat());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(Bundle savedInstanceState) {
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

				mWatchStatus = ((Patch) mEntity).watchStatus();
				Boolean owner = (UserManager.shared().authenticated() && mEntity.ownerId != null && mEntity.ownerId.equals(UserManager.currentUser.id));

				/* Photo overlayed with info */

				final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
				final ImageLayout photoView = (ImageLayout) view.findViewById(R.id.photo);
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
						photoView.setImageWithPhoto(mEntity.photo);
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

				UI.setVisibility(view.findViewById(R.id.tune_button), (owner ? View.VISIBLE : View.GONE));
				drawButtons(view);
				drawActionView(view);

				final CandiView candiViewInfo = (CandiView) view.findViewById(R.id.candi_view_info);
				final TextView description = (TextView) view.findViewById(R.id.description);
				final TextView ownerName = (TextView) view.findViewById(R.id.owner_name);
				final Button buttonMore = (Button) view.findViewById(R.id.expando_button);

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
					UI.setVisibility(buttonMore, View.VISIBLE);
				}

				/* Creator (on info side) */

				if (ownerName != null) {
					if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						ownerName.setText(((Patch) mEntity).owner.name);
						UI.setVisibility(ownerName, View.VISIBLE);
					}
					else {
						UI.setVisibility(ownerName, View.GONE);
					}
				}
			}
		});
	}

	public void drawActionView(View view) {
		ViewGroup actionView = (ViewGroup) view.findViewById(R.id.action_group);

		if (actionView != null && mEntity != null) {

			Patch patch = (Patch) mEntity;
			Boolean owner = (UserManager.shared().authenticated() && patch.ownerId != null && patch.ownerId.equals(UserManager.currentUser.id));
			Boolean hasMessaged = (mEntity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);
			Boolean isPublic = (patch.privacy != null
					&& patch.privacy.equals(Constants.PRIVACY_PUBLIC)
					&& patch.isVisibleToCurrentUser());

			TextView buttonAlert = (TextView) view.findViewById(R.id.action_button);
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
					buttonAlert.setTag(R.id.members_button);
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
			Shortcut fromShortcut = UserManager.currentUser.getAsShortcut();
			Shortcut toShortcut = mEntity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(UserManager.currentUser.id)
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
					.setFromId(UserManager.currentUser.id)
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

	public void mute(final Boolean mute) {

		final Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		final String actionEvent = mute ? "mute_watch_entity" : "unmute_watch_entity";

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (getView() != null) {
					ViewAnimator animator = (ViewAnimator) getView().findViewById(R.id.mute_button);
					if (animator != null) {
						animator.setDisplayedChild(1);  // Turned off in drawButtons
					}
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncMuteLink");
				return DataController.getInstance().muteLink(link.id, mute, actionEvent);
			}

			@Override
			protected void onPostExecute(Object response) {
				bind(FetchMode.AUTO);
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
				.setUserId(UserManager.currentUser.id);

		event.setActionType(ActionType.ACTION_SHARE_CHECK)
				.setTag(System.identityHashCode(this));

		Dispatcher.getInstance().post(event);
	}

}