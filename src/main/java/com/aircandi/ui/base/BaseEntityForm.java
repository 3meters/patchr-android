package com.aircandi.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Links;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseEntityForm extends BaseActivity {

	protected Integer mLinkProfile;
	protected Integer mTransitionType;

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

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		if (mEntityId != null) {
			mEntityMonitor = new EntityMonitor(mEntityId);
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
		Bundle extras = new Bundle();
		if (Type.isTrue(entity.autowatchable)) {
			extras.putBoolean(Constants.EXTRA_PRE_APPROVED, true);
		}
		Patchr.dispatch.route(this, Route.BROWSE, entity, extras);
	}

	@Override
	@SuppressWarnings("ucd")
	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patchr.dispatch.route(this, Route.PHOTO, null, extras);
		}
		else if (mEntity.photo != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mParentId);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, (mListLinkType == null) ? Constants.TYPE_LINK_CONTENT : mListLinkType);
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, mEntity.schema);
			Patchr.dispatch.route(this, Route.PHOTOS, mEntity, extras);
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
				/*
				 * Guarantees that any cache stamp retrieved for parent entity from the service will not equal
				 * any cache stamp including itself. Cleared if parent entity is refreshed from the service.
				 */
				Patchr.getInstance().getEntityManager().getCacheStampOverrides().put(mParentId, mParentId);
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(final BindingMode mode) {

		final AtomicBoolean refreshNeeded = new AtomicBoolean(false);
		final AtomicBoolean redrawNeeded = new AtomicBoolean(mInvalidated);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.show(BusyAction.Refreshing_Empty);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntity");

				beforeDatabind(mode);
				ModelResult result = new ModelResult();

				if (mEntityMonitor.isChanged()) {
					refreshNeeded.set(true);
				}

				if (refreshNeeded.get() || mode == BindingMode.MANUAL) {
					mBusy.show(mLoaded ? BusyAction.Refreshing : BusyAction.Refreshing_Empty);
				}

				if (refreshNeeded.get()) {
					Links options = Patchr.getInstance().getEntityManager().getLinks().build(mLinkProfile);
					result = Patchr.getInstance().getEntityManager().getEntity(mEntityId, true, options);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				if (isFinishing()) return;

				final ModelResult result = (ModelResult) modelResult;
				mBusy.hide(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (refreshNeeded.get()) {
						if (result.data != null) {
							mEntity = (Entity) result.data;

							if (mParentId != null) {
								mEntity.toId = mParentId;
							}
							if (mEntity instanceof Patch) {
								Patchr.getInstance().setCurrentPatch(mEntity);
							}

							configureStandardMenuItems(mOptionMenu);
							draw(null);
						}
						else {
							mBusy.hide(true);
							UI.showToastNotification("This item has been deleted", Toast.LENGTH_SHORT);
							finish();
						}
					}
					else if (redrawNeeded.get()) {
						if (mEntity != null) {
							configureStandardMenuItems(mOptionMenu);
							draw(null);
						}
					}

					/* Ensure this is flagged as read */
					if (mNotificationId != null) {
						if (NotificationManager.getInstance().getNotifications().containsKey(mNotificationId)) {
							NotificationManager.getInstance().getNotifications().get(mNotificationId).read = true;
						}
					}
				}
				else {
					Errors.handleError(BaseEntityForm.this, result.serviceResponse);
					return;
				}

				afterDatabind(mode, result);
				if (mEntityMonitor instanceof EntityMonitor) {
					/*
					 * Causes cache stamp checks to look clean so when we
					 * rebind the entity list, it thinks there is nothing to do because the
					 * cache stamp looks clean.
					 */
					((EntityMonitor) mEntityMonitor).updateCacheStamp(mEntity);
				}
			}
		}.execute();
	}

	public void beforeDatabind(final BindingMode mode) {
	    /*
	     * Called from background thread.
	     *
	     * If cache entity is fresher than the one currently bound to or there is
		 * a cache entity available, go ahead and draw before we check against the service.
		 */
		mEntity = EntityManager.getCacheEntity(mEntityId);
		if (mEntity != null) {
			if (mEntity instanceof Patch) {
				Patchr.getInstance().setCurrentPatch(mEntity);
				Logger.v(this, "Setting current patch to: " + mEntity.id);
			}
			if (mFirstDraw) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						draw(null);
					}
				});
			}
		}
	}

	public void afterDatabind(final BindingMode mode, ModelResult result) {
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLoaded = true;
		}
	}

	public void draw(View view) {}

	protected void drawStats(View view) {}

	protected void drawButtons(View view) {}

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
				Logger.v(this, "Setting current patch to: " + mEntity.id);
			}
			bind(BindingMode.AUTO);    // check to see if the cache stamp is stale
		}
	}
}