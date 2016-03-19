package com.patchr.ui;

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
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.NotificationManager;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataQueryResultEvent;
import com.patchr.events.EntityQueryEvent;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Count;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.objects.TransitionType;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

public abstract class BaseEntityForm extends BaseActivity {

	/* Inputs */
	public String mParentId;
	@NonNull protected Integer mLinkProfile = LinkSpecType.NO_LINKS;
	protected Integer mTransitionType;
	protected String mListLinkType;
	protected String mNotificationId;
	protected Boolean mBound;

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			mNotificationId = extras.getString(Constants.EXTRA_NOTIFICATION_ID);
		}
	}

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
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
			if (entity instanceof Patch) {
				Patchr.getInstance().setCurrentPatch(entity);
			}
			fetch(FetchMode.AUTO);    // check to see if the cache stamp is stale
		}
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onDataResult(final DataQueryResultEvent event) {

		if (event.tag.equals(System.identityHashCode(this))
				&& (event.entity == null || event.entity.id.equals(entityId))) {

			Logger.v(this, "Data result accepted: " + event.actionType.name().toString());

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					if (event.actionType == ActionType.ACTION_GET_ENTITY) {

						mBound = true;
						if (event.entity != null) {
							entity = event.entity;

							if (mParentId != null) {
								entity.toId = mParentId;
							}

							if (entity instanceof Patch) {
								Patchr.getInstance().setCurrentPatch(entity);
							}
						}
						/*
						 * Possible to hit this before options menu has been set. If so then
						 * configureStandardMenuItems will be called in onCreateOptionsMenu.
						 */
						if (optionMenu != null) {
							configureStandardMenuItems(optionMenu);
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
			if (!mBound || event.mode == FetchMode.MANUAL || linkAction) {
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
		processing = false;
		uiController.getBusyController().hide(false);
	}

	public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		fetch(FetchMode.MANUAL); // Called from Routing
		if (currentFragment != null && currentFragment instanceof EntityListFragment) {
			((EntityListFragment) currentFragment).listPresenter.refresh();
		}
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED) {
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

	public void fetch(final FetchMode mode) {
		/*
		 * Called on main thread.
		 */
		Logger.v(this, "Binding: " + mode.name().toString());
		EntityQueryEvent request = new EntityQueryEvent();
		request.setLinkProfile(mLinkProfile)
				.setActionType(ActionType.ACTION_GET_ENTITY)
				.setFetchMode(mode)
				.setEntityId(entityId)
				.setTag(System.identityHashCode(this));

		if (mBound && entity != null && mode != FetchMode.MANUAL) {
			request.setCacheStamp(entity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	public void draw(View view) {}

	public void drawLikeWatch(View view) {

		/* We don't support like/watch for users */
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			UI.setVisibility(view.findViewById(R.id.toolbar), View.GONE);
			return;
		}

		UI.setVisibility(view.findViewById(R.id.toolbar), View.VISIBLE);

		/* Like button coloring */
		ViewAnimator like = (ViewAnimator) view.findViewById(R.id.button_like);
		if (like != null) {
			like.setDisplayedChild(0);
			if (entity instanceof Patch && !((Patch) entity).isVisibleToCurrentUser()) {
				UI.setVisibility(like, View.GONE);
			}
			else {
				Link link = entity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
				ImageView image = (ImageView) like.findViewById(R.id.mute_image);
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
			Count count = entity.getCount(Constants.TYPE_LINK_LIKE, null, true, Link.Direction.in);
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
		View watching = view.findViewById(R.id.members_button);
		if (watching != null) {
			Count count = entity.getCount(Constants.TYPE_LINK_WATCH, null, true, Link.Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, null, 0);
			}
			if (count.count.intValue() > 0) {
				TextView watchingCount = (TextView) view.findViewById(R.id.members_count);
				TextView watchingLabel = (TextView) view.findViewById(R.id.members_label);
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
}