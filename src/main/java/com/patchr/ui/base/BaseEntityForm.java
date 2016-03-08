package com.patchr.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController.ActionType;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.NotificationManager;
import com.patchr.components.UserManager;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.EntityRequestEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.Count;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.objects.Shortcut;
import com.patchr.objects.TransitionType;
import com.patchr.ui.EntityListFragment;
import com.patchr.utilities.Colors;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Locale;

public abstract class BaseEntityForm extends BaseActivity {

	/* Inputs */
	@SuppressWarnings("ucd")
	public String mParentId;
	@NonNull
	protected Integer mLinkProfile = LinkSpecType.NO_LINKS;
	protected Integer mTransitionType;
	/* Part of binding logic */
	protected Boolean mBound = false;
	protected String mListLinkType;
	protected String mNotificationId;

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			mNotificationId = extras.getString(Constants.EXTRA_NOTIFICATION_ID);
		}
	}

	@Override protected void onResume() {
		super.onResume();
		/*
		 * We have to be pretty aggressive about refreshing the UI because
		 * there are lots of actions that could have happened while this activity
		 * was stopped that change what the user would expect to see.
		 *
		 * - Entity deleted or modified
		 * - Entity children modified
		 * - New comments
		 * - Change in user which effects which candi and UI should be visible.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing()) {
			if (mEntity instanceof Patch) {
				Patchr.getInstance().setCurrentPatch(mEntity);
			}
			bind(BindingMode.AUTO);    // check to see if the cache stamp is stale
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onDataResult(final DataResultEvent event) {

		if (event.tag.equals(System.identityHashCode(this))
				&& (event.entity == null || event.entity.id.equals(mEntityId))) {

			Logger.v(this, "Data result accepted: " + event.actionType.name().toString());

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					if (event.actionType == ActionType.ACTION_GET_ENTITY) {

						mBound = true;
						if (event.entity != null) {
							mEntity = event.entity;

							if (mParentId != null) {
								mEntity.toId = mParentId;
							}

							if (mEntity instanceof Patch) {
								Patchr.getInstance().setCurrentPatch(mEntity);
							}
						}
						/*
						 * Possible to hit this before options menu has been set. If so then
						 * configureStandardMenuItems will be called in onCreateOptionsMenu.
						 */
						if (mOptionMenu != null) {
							configureStandardMenuItems(mOptionMenu);
						}

						/* Ensure this is flagged as read */
						if (mNotificationId != null) {
							if (NotificationManager.getInstance().getNotifications().containsKey(mNotificationId)) {
								NotificationManager.getInstance().getNotifications().get(mNotificationId).read = true;
							}
						}

						draw(null);
						onProcessingComplete(new ProcessingCompleteEvent());
					}
					else if (event.actionType == ActionType.ACTION_LINK_INSERT_LIKE
							|| event.actionType == ActionType.ACTION_LINK_DELETE_LIKE) {

						draw(null);
						onProcessingComplete(new ProcessingCompleteEvent());
					}
				}
			});
		}
	}

	@Subscribe public void onDataError(DataErrorEvent event) {
		if (event.tag.equals(System.identityHashCode(this))) {
			Logger.v(this, "Data error accepted: " + event.actionType.name().toString());

			Boolean linkAction = (event.actionType.name().toLowerCase(Locale.US).contains("link"));

			if (linkAction) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						draw(null);     // Chance to clear any embedded busy ui
					}
				});
			}

			onProcessingComplete(new ProcessingCompleteEvent());

			/* We eat errors for network operations the user didn't specifically initiate. */
			if (!mBound || event.mode == BindingMode.MANUAL || linkAction) {
				Errors.handleError(BaseEntityForm.this, event.errorResponse);
			}
		}
	}

	@Subscribe public void onDataNoop(DataNoopEvent event) {
		if (event.tag.equals(System.identityHashCode(this))) {
			Logger.v(this, "Data no-op accepted: " + event.actionType.name().toString());
			onProcessingComplete(new ProcessingCompleteEvent());
		}
	}

	@Subscribe public void onProcessingComplete(ProcessingCompleteEvent event) {
		mProcessing = false;
		mUiController.getBusyController().hide(false);
	}

	@Override public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		bind(BindingMode.MANUAL); // Called from Routing
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			((EntityListFragment) mCurrentFragment).onRefresh();
		}
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED || Patchr.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					AnimationManager.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.d(this, "Activity saving state");
	}

	@Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		/*
		 * Will only be called if the activity is destroyed and restored. Restore
		 * state could be handled in onCreate or here later in the lifecycle after
		 * everything has been initialized.
		 */
		super.onRestoreInstanceState(savedInstanceState);
		Logger.d(this, "Activity restoring state");
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(final BindingMode mode) {
		/*
		 * Called on main thread.
		 */
		Logger.v(this, "Binding: " + mode.name().toString());
		EntityRequestEvent request = new EntityRequestEvent()
				.setLinkProfile(mLinkProfile);

		request.setActionType(ActionType.ACTION_GET_ENTITY)
				.setMode(mode)
				.setEntityId(mEntityId)
				.setTag(System.identityHashCode(this));

		if (mBound && mEntity != null && mode != BindingMode.MANUAL) {
			request.setCacheStamp(mEntity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	public void draw(View view) {}

	public void drawLikeWatch(View view) {

		/* We don't support like/watch for users */
		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.GONE);
			return;
		}

		UI.setVisibility(view.findViewById(R.id.button_holder), View.VISIBLE);

		/* Like button coloring */
		ViewAnimator like = (ViewAnimator) view.findViewById(R.id.button_like);
		if (like != null) {
			like.setDisplayedChild(0);
			if (mEntity instanceof Patch && !((Patch) mEntity).isVisibleToCurrentUser()) {
				UI.setVisibility(like, View.GONE);
			}
			else {
				Link link = mEntity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
				ImageView image = (ImageView) like.findViewById(R.id.button_image);
				if (link != null) {
					final int color = Colors.getColor(R.color.brand_primary);
					image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
					image.setAlpha(1.0f);
				}
				else {
					image.setColorFilter(null);
					image.setAlpha(0.5f);
				}
				UI.setVisibility(like, View.VISIBLE);
			}
		}

		/* Like count */
		View likes = view.findViewById(R.id.button_likes);
		if (likes != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, null, true, Link.Direction.in);
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

		/* Watching count */
		View watching = view.findViewById(R.id.button_watching);
		if (watching != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Link.Direction.in);
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
	}

	public void like(final boolean activate) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ViewAnimator animator = (ViewAnimator) findViewById(R.id.button_like);
				if (animator != null) {
					animator.setDisplayedChild(1);  // Turned off in drawButtons
				}
			}
		});

		if (UserManager.getInstance().authenticated()) {
			UserManager.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
		}

		if (activate) {

			/* Used as part of link management */
			Shortcut fromShortcut = UserManager.getInstance().getCurrentUser().getAsShortcut();
			Shortcut toShortcut = mEntity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(UserManager.getInstance().getCurrentUser().id)
					.setToId(mEntity.id)
					.setType(Constants.TYPE_LINK_LIKE)
					.setEnabled(true)
					.setFromShortcut(fromShortcut)
					.setToShortcut(toShortcut)
					.setActionEvent("like_entity_" + mEntity.schema.toLowerCase(Locale.US))
					.setSkipCache(false);

			update.setActionType(ActionType.ACTION_LINK_INSERT_LIKE)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
		else {

			LinkDeleteEvent update = new LinkDeleteEvent()
					.setFromId(UserManager.getInstance().getCurrentUser().id)
					.setToId(mEntity.id)
					.setType(Constants.TYPE_LINK_LIKE)
					.setSchema(mEntity.schema)
					.setActionEvent("unlike_entity_" + mEntity.schema.toLowerCase(Locale.US));

			update.setActionType(ActionType.ACTION_LINK_DELETE_LIKE)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class LikeStatus {
		public static int NONE = 0;
		public static int LIKE = 1;
	}
}