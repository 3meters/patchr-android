package com.aircandi.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Links;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseEntityForm extends BaseActivity {

	protected Integer mLinkProfile;

	/* Inputs */
	@SuppressWarnings("ucd")
	public    String mParentId;
	protected String mListLinkType;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		if (mEntityId != null) {
			mEntityMonitor = new EntityMonitor(mEntityId);
		}
	}

	public void beforeDatabind(final BindingMode mode) {
	    /*
	     * If cache entity is fresher than the one currently bound to or there is
		 * a cache entity available, go ahead and draw before we check against the service.
		 */
		mEntity = EntityManager.getCacheEntity(mEntityId);
		if (mEntity != null) {
			if (mEntity instanceof Place) {
				Patch.getInstance().setCurrentPlace(mEntity);
				Logger.v(this, "Setting current place to: " + mEntity.id);
			}
			if (mFirstDraw) {
				draw(null);
			}
		}
	}

	public void afterDatabind(final BindingMode mode, ModelResult result) {
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLoaded = true;
		}
	}

	@Override
	public void bind(final BindingMode mode) {
		Logger.d(this, "Binding: mode = " + mode.name().toLowerCase(Locale.US));

		final AtomicBoolean refreshNeeded = new AtomicBoolean(false);
		final AtomicBoolean redrawNeeded = new AtomicBoolean(mInvalidated);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				beforeDatabind(mode);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntity");
				ModelResult result = new ModelResult();

				if (mEntityMonitor.isChanged()) {
					refreshNeeded.set(true);
				}

				if (refreshNeeded.get() || mode == BindingMode.MANUAL) {
					mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
				}

				if (refreshNeeded.get()) {
					Links options = Patch.getInstance().getEntityManager().getLinks().build(mLinkProfile);
					result = Patch.getInstance().getEntityManager().getEntity(mEntityId, true, options);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				if (isFinishing()) return;

				final ModelResult result = (ModelResult) modelResult;
				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (refreshNeeded.get()) {
						if (result.data != null) {
							mEntity = (Entity) result.data;
							if (mParentId != null) {
								mEntity.toId = mParentId;
							}
							if (mEntity instanceof Place) {
								Patch.getInstance().setCurrentPlace(mEntity);
								Logger.v(this, "Setting current place to: " + mEntity.id);
							}
							draw(null);
						}
						else {
							mBusy.hideBusy(true);
							UI.showToastNotification("This item has been deleted", Toast.LENGTH_SHORT);
							finish();
						}
					}
					else if (redrawNeeded.get()) {
						if (mEntity != null) {
							draw(null);
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

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onRefresh() {
		bind(BindingMode.MANUAL); // Called from Routing
	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		if (Patch.getInstance().getCurrentUser().isAnonymous()) {
			String message = StringManager.getString(R.string.alert_signin_message_watch, mEntity.schema);
			Dialogs.signinRequired(this, message);
			return;
		}

		if (mEntity != null) {
			watch(false);
		}
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		Bundle extras = new Bundle();
		if (Type.isTrue(entity.autowatchable)) {
			if (Patch.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
					, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
				extras.putBoolean(Constants.EXTRA_AUTO_WATCH, true);
			}
		}
		Patch.dispatch.route(this, Route.BROWSE, entity, null, extras);
	}

	@Override
	@SuppressWarnings("ucd")
	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patch.dispatch.route(this, Route.PHOTO, null, null, extras);
		}
		else if (mEntity.photo != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mParentId);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, (mListLinkType == null) ? Constants.TYPE_LINK_CONTENT : mListLinkType);
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, mEntity.schema);
			Patch.dispatch.route(this, Route.PHOTOS, mEntity, null, extras);
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
		if (resultCode != Activity.RESULT_CANCELED || Patch.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				/*
				 * Guarantees that any cache stamp retrieved for parent entity from the service will not equal
				 * any cache stamp including itself. Cleared if parent entity is refreshed from the service.
				 */
				Patch.getInstance().getEntityManager().getCacheStampOverrides().put(mParentId, mParentId);
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					Patch.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
			else if (requestCode == Constants.ACTIVITY_APPLICATION_PICK) {

				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String entitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);

					if (entitySchema != null && !entitySchema.equals("")) {
						extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
						extras.putString(Constants.EXTRA_ENTITY_SCHEMA, entitySchema);
						super.onAdd(extras);
					}
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
		//		final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
		//		if (position != null && mScrollView != null) {
		//			mScrollView.post(new Runnable() {
		//				@Override
		//				public void run() {
		//					mScrollView.scrollTo(position[0], position[1]);
		//				}
		//			});
		//		}
	}

	/*--------------------------------------------------------------------------------------------
	 * UI
	 *--------------------------------------------------------------------------------------------*/

	public void draw(View view) {}

	protected void drawStats(View view) {}

	protected void drawButtons(View view) {

		if (mEntity.id.equals(Patch.getInstance().getCurrentUser().id)) {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.GONE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.button_holder), View.VISIBLE);

			ComboButton watched = (ComboButton) view.findViewById(R.id.button_watch);
			if (watched != null) {
				UI.setVisibility(watched, View.VISIBLE);
				Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
				if (link != null && link.enabled) {
					final int color = Colors.getColor(R.color.brand_primary);
					watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					watched.getImageIcon().setColorFilter(null);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void watch(final boolean autoWatch) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				((ComboButton) findViewById(R.id.button_watch)).getViewAnimator().setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncWatchEntity");
				ModelResult result;
				Patch.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
				Boolean enabled = mEntity.visibleToCurrentUser();
				if (!mEntity.byAppUser(Constants.TYPE_LINK_WATCH)) {

					Shortcut fromShortcut = Patch.getInstance().getCurrentUser().getShortcut();
					Shortcut toShortcut = mEntity.getShortcut();

					result = Patch.getInstance().getEntityManager().insertLink(Patch.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, enabled
							, fromShortcut
							, toShortcut
							, "watch_entity_" + mEntity.schema);
				}
				else {
					result = Patch.getInstance().getEntityManager().deleteLink(Patch.getInstance().getCurrentUser().id
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
				if (!afterWatch(result)) {
					((ComboButton) findViewById(R.id.button_watch)).getViewAnimator().setDisplayedChild(0);
					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						View view = findViewById(android.R.id.content);
						drawButtons(view);
						drawStats(view);
						if (autoWatch) {
							UI.showToastNotification(StringManager.getString(R.string.alert_auto_watch), Toast.LENGTH_SHORT, Gravity.CENTER);
						}
					}
					else {
						if (result.serviceResponse.statusCodeService != null
								&& result.serviceResponse.statusCodeService != ServiceConstants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
							Errors.handleError(BaseEntityForm.this, result.serviceResponse);
						}
					}
				}
			}
		}.execute();
	}

	protected Boolean afterWatch(ModelResult result) {
		return false;
	}

	@Override
	public Boolean related(String entityId) {
		return (mEntityId != null && entityId.equals(mEntityId));
	}

	@Override
	public Boolean related(Entity entity) {
		return (mEntity != null && entity.id.equals(mEntity.id));
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
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
			if (mEntity instanceof Place) {
				Patch.getInstance().setCurrentPlace(mEntity);
				Logger.v(this, "Setting current place to: " + mEntity.id);
			}

			Patch.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_BACK);

			bind(BindingMode.AUTO);    // check to see if the cache stamp is stale
		}
	}
}