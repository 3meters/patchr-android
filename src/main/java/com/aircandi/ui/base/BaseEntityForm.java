package com.aircandi.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ViewAnimator;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataReadyEvent;
import com.aircandi.events.EntityRequestEvent;
import com.aircandi.objects.ActionType;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.squareup.otto.Subscribe;

import java.util.Locale;

public abstract class BaseEntityForm extends BaseActivity {

	@NonNull
	protected Integer mLinkProfile;
	protected Integer mTransitionType;
	protected Integer mLikeStatus = LikeStatus.NONE;     // Set in draw
	protected AsyncTask mTaskGetEntity;

	/* Inputs */
	@SuppressWarnings("ucd")
	public    String mParentId;
	protected String mListLinkType;
	protected String mNotificationId;

	@Override
	public void unpackIntent() {
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

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onRefresh() {
		bind(BindingMode.MANUAL); // Called from Routing
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity == null) return;

		Bundle extras = new Bundle();
		if (Type.isTrue(entity.autowatchable)) {
			extras.putBoolean(Constants.EXTRA_PRE_APPROVED, true);
		}
		Patchr.router.route(this, Route.BROWSE, entity, extras);
	}

	@Override
	@SuppressWarnings("ucd")
	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patchr.router.route(this, Route.PHOTO, null, extras);
		}
		else if (mEntity.photo != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mParentId);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, (mListLinkType == null) ? Constants.TYPE_LINK_CONTENT : mListLinkType);
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, mEntity.schema);
			Patchr.router.route(this, Route.PHOTOS, mEntity, extras);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Cases that use activity result
		 * 
		 * - Candi picker returns entity id for a move
		 * - Template picker returns type of candi to add as a child
		 */
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

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.d(this, "Activity saving state");
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		/*
		 * Will only be called if the activity is destroyed and restored. Restore
		 * state could be handled in onCreate or here later in the lifecycle after
		 * everything has been initialized.
		 */
		super.onRestoreInstanceState(savedInstanceState);
		Logger.d(this, "Activity restoring state");
	}

	@Subscribe
	public void onDataReady(final DataReadyEvent event) {

		if (event.entity != null && event.tag.equals(System.identityHashCode(this)) && event.entity.id.equals(mEntityId)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mUiController.getBusyController().hide(false);
					mEntity = event.entity;

					if (mParentId != null) {
						mEntity.toId = mParentId;
					}

					if (mEntity instanceof Patch) {
						Patchr.getInstance().setCurrentPatch(mEntity);
					}
					/*
					 * Possible to hit this before options menu has been set. If so then
					 * configureStandardMenuItems will be called in onCreateOptionsMenu.
					 */
					if (mOptionMenu != null) {
						configureStandardMenuItems(mOptionMenu);
					}

					draw(null);

					/* Ensure this is flagged as read */
					if (mNotificationId != null) {
						if (NotificationManager.getInstance().getNotifications().containsKey(mNotificationId)) {
							NotificationManager.getInstance().getNotifications().get(mNotificationId).read = true;
						}
					}
					onProcessingComplete(ResponseCode.SUCCESS);
				}
			});
		}
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		onProcessingComplete(ResponseCode.FAILED);
		Errors.handleError(BaseEntityForm.this, event.errorResponse);
	}

	protected void onProcessingComplete(final ResponseCode responseCode) {
		mUiController.getBusyController().hide(false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(final BindingMode mode) {
		/*
		 * Called on main thread.
		 */
		EntityRequestEvent request = new EntityRequestEvent()
				.setLinkProfile(mLinkProfile);

		request.setActionType(ActionType.GET_ENTITY)
		       .setEntityId(mEntityId)
		       .setTag(System.identityHashCode(this));
		/*
		 * Providing a CacheStamp means we won't get called back unless something fresher is available.
		 */
		if (mEntity != null) {
			request.setCacheStamp(mEntity.getCacheStamp());
		}
		Dispatcher.getInstance().post(request);
	}

	public void afterDatabind(final BindingMode mode, ModelResult result) {
		if (result == null || result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mNotEmpty = true;
		}
	}

	public void draw(View view) {}

	protected void drawStats(View view) {}

	protected void drawButtons(View view) {}

	public void like(final boolean activate) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				((ViewAnimator) findViewById(R.id.button_like)).setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLikeEntity");
				ModelResult result;
				Patchr.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();

				if (activate) {

					/* Used as part of link management */
					Shortcut fromShortcut = Patchr.getInstance().getCurrentUser().getAsShortcut();
					Shortcut toShortcut = mEntity.getAsShortcut();

					result = DataController.getInstance().insertLink(null
							, Patchr.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_LIKE
							, null
							, fromShortcut
							, toShortcut
							, "like_entity_" + mEntity.schema.toLowerCase(Locale.US)
							, false, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				}
				else {
					result = DataController.getInstance().deleteLink(Patchr.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_LIKE
							, null
							, mEntity.schema
							, "unlike_entity_" + mEntity.schema.toLowerCase(Locale.US), NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;

				((ViewAnimator) findViewById(R.id.button_like)).setDisplayedChild(0);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					bind(BindingMode.AUTO); // Triggers redraw including buttons and updates state
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != Constants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(BaseEntityForm.this, result.serviceResponse);
					}
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override
	public Boolean related(String entityId) {
		return (mEntityId != null && entityId != null && entityId.equals(mEntityId));
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
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

	@Override
	protected void onStop() {

		Logger.d(this, "Activity stopping");
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class LikeStatus {
		public static int NONE = 0;
		public static int LIKE = 1;
	}
}