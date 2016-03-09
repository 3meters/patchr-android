package com.patchr.ui.base;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.NfcManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.interfaces.IBind;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.interfaces.IForm;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Photo;
import com.patchr.objects.Preference;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.AircandiForm;
import com.patchr.ui.PhotoForm;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.MessageController;
import com.patchr.ui.components.UiController;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity
		implements SwipeRefreshLayout.OnRefreshListener, IForm, IBind {

	private   Toolbar mActionBarToolbar;
	protected Menu    mOptionMenu;
	protected final UiController mUiController = new UiController();

	protected View     mNotificationsBadgeGroup;
	protected TextView mNotificationsBadgeCount;
	protected View     mNotificationActionIcon;

	protected DrawerLayout          mDrawerLayout;
	protected View                  mDrawerLeft;
	protected View                  mDrawerRight;
	protected ActionBarDrawerToggle mDrawerToggle;

	protected String mActivityTitle;
	protected Entity mEntity;
	protected String mEntityId;
	public    String mForId;

	/* Fragments */
	protected Map<String, Fragment> mFragments = new HashMap<>();
	protected Fragment mCurrentFragment;
	protected String   mCurrentFragmentTag;

	/* Inputs */
	protected Integer mTransitionType = TransitionType.FORM_TO;

	/* Resources */
	protected Integer mDeleteProgressResId = R.string.progress_deleting;
	protected Integer mDeletedResId        = R.string.alert_deleted;
	protected Integer mRemoveProgressResId = R.string.progress_removing;
	protected Integer mRemovedResId        = R.string.alert_removed;
	protected Integer mLayoutResId;

	protected Boolean mPrefChangeNewSearchNeeded = false;
	protected Boolean mPrefChangeRefreshUiNeeded = false;

	public Resources mResources;
	public    Boolean mFirstDraw  = true;
	protected Boolean mProcessing = false;
	protected Boolean mRestarting = false;

	@Override public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mLayoutResId = extras.getInt(Constants.EXTRA_LAYOUT_RESID);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
		}
	}

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		unpackIntent();
		setTheme(R.style.patchr_theme);
		setContentView(getLayoutId());
		initialize(savedInstanceState);
	}

	@Override protected void onPostCreate(Bundle savedInstanceState) {
		configureActionBar();
		super.onPostCreate(savedInstanceState);
	}

	@Override protected void onRestart() {
		super.onRestart();
		checkForPreferenceChanges();
	}

	@Override protected void onResume() {
		super.onResume();

		Dispatcher.getInstance().register(this);
		Patchr.getInstance().setCurrentActivity(this);
		mUiController.resume();
		mProcessing = false;
		/*
		 * We always check to make sure play services are working properly. This call will finish
		 * the activity if play services are missing and can't be installed or if the user
		 * refuses to install them. If play services can be fixed, then resume will be called again.
		 */
		AndroidManager.checkPlayServices(this);
	}

	@Override protected void onPause() {
		super.onPause();
		Dispatcher.getInstance().unregister(this);
		mUiController.pause();
		clearReferences();
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

			if (id == R.id.photo_view) {
				onPhotoClick(event.view);
			}
			else if (id == R.id.share_entity
					|| id == R.id.item_row
					|| id == R.id.holder_user
					|| id == R.id.user_photo
					|| id == R.id.user_current) {
				onEntityClick(event.view);
			}
			mProcessing = false;
		}
	}

	@Override public void onRefresh() {}

	@Override public void onAdd(Bundle extras) {

		if (!UserManager.getInstance().authenticated()) {
			UserManager.getInstance().showGuestGuard(this, "Sign up for a free account to post messages, make patches and more.");
			return;
		}

		/* Schema target is in the extras */
		Patchr.router.route(this, Route.NEW, mEntity, extras);
	}

	@Override public void onBackPressed() {
		if (BaseActivity.this instanceof AircandiForm) {
			super.onBackPressed();
		}
		else {
			Patchr.router.route(this, Route.CANCEL, null, null);
		}
	}

	@Override public void onHelp() {}

	@Override public void onError() {}

	@Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Patchr.resultCode = Activity.RESULT_OK;
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		Logger.v(this, "Creating options menu");
		/*
		 * - activity is first started.
		 * - switching fragments.
		 * - after invalidateOptionsMenu.
		 * - after onResume in lifecycle.
		 */
		MenuManager.onCreateOptionsMenu(this, menu);
		mOptionMenu = menu;
		configureStandardMenuItems(menu);
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		Bundle extras = null;
		if (item.getItemId() == android.R.id.home) {
			if (mDrawerToggle != null) {
				mDrawerToggle.onOptionsItemSelected(item);
			}
			if (mDrawerLayout != null) {
				if (mDrawerRight != null && mDrawerLayout.isDrawerOpen(mDrawerRight)) {
					mNotificationActionIcon.animate().rotation(0f).setDuration(200);
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
			}
			return true;
		}

		if (item.getItemId() == R.id.remove && mEntity.patchId != null) {
		    /*
		     * We use patchId instead of toId so we can removed replies where
             * toId points to the root message.
             */
			extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntity.patchId);
		}

		if (item.getItemId() == R.id.navigate) {
			AirLocation location = mEntity.getLocation();
			if (location == null) {
				throw new IllegalArgumentException("Tried to navigate without a location");
			}

			AndroidManager.getInstance().callMapNavigation(this
					, location.lat.doubleValue()
					, location.lng.doubleValue()
					, null
					, mEntity.name);
			return true;
		}

		Patchr.router.route(this, Patchr.router.routeForMenuId(item.getItemId()), mEntity, extras);
		return true;
	}

	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity == null) return;

		Bundle extras = new Bundle();
		Patchr.router.route(this, Route.BROWSE, entity, extras);
	}

	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patchr.router.route(this, Route.PHOTO, null, extras);
		}
	}

	public void onAccept() {}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, getExitTransitionType());
	}

	public void onViewClick(View view) {
		Dispatcher.getInstance().post(new ActionEvent()
				.setActionType(DataController.ActionType.ACTION_VIEW_CLICK)
				.setView(view));
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {

		mResources = getResources();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		NfcManager.pushUri(Uri.parse("http://patchr.com"), this);

		mUiController.setBusyController(new BusyController());
		mUiController.getBusyController().setProgressBar(findViewById(R.id.form_progress));
		mUiController.setMessageController(new MessageController(findViewById(R.id.form_message)));

		getActionBarToolbar();
	}

	@Override public void draw(View view) {}

	@Override public void bind(BindingMode mode) {}

	@Override public void share() {}

	protected void configureStandardMenuItems(Menu menu) {

		MenuItem menuItem = menu.findItem(R.id.edit);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.EDIT, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.add);
		if (menuItem != null && mEntity != null) {
			menuItem.setVisible(MenuManager.showAction(Route.ADD, mEntity, mForId));
			menuItem.setTitle(StringManager.getString(R.string.menu_item_add_entity, mEntity.schema));
		}

		menuItem = menu.findItem(R.id.delete);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.DELETE, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.remove);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.REMOVE, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.signin);
		if (menuItem != null) {
			menuItem.setVisible(!UserManager.getInstance().authenticated());
		}

		menuItem = menu.findItem(R.id.signout);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.EDIT, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.navigate);
		if (menuItem != null && UserManager.getInstance().authenticated()) {
			menuItem.setVisible(mEntity.getLocation() != null);
		}

		menuItem = menu.findItem(R.id.share);
		if (menuItem != null) {
			if (this instanceof PhotoForm) {
				menuItem.setVisible(MenuManager.showAction(Route.SHARE, mEntity, mForId));
			}
		}

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			mNotificationActionIcon = MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_image);
			MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_frame).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
						mDrawerLayout.closeDrawer(mDrawerLeft);
					}
					if (mDrawerRight != null) {
						if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
							mNotificationActionIcon.animate().rotation(0f).setDuration(200);
							mDrawerLayout.closeDrawer(mDrawerRight);
						}
						else {
							mNotificationActionIcon.animate().rotation(90f).setDuration(200);
							mDrawerLayout.openDrawer(mDrawerRight);
						}
					}
				}
			});

			if (UserManager.getInstance().authenticated()) {
				View view = MenuItemCompat.getActionView(notifications);
				mNotificationsBadgeGroup = view.findViewById(R.id.badge_group);
				mNotificationsBadgeCount = (TextView) view.findViewById(R.id.badge_count);
			}
		}
	}

	protected void configureActionBar() {
		/*
		 * By default we show the nav indicator and the title.
		 */
		if (super.getSupportActionBar() != null) {
			super.getSupportActionBar().setDisplayShowTitleEnabled(true);
			super.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		getActionBarToolbar().setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}

	public void confirmDelete() {
		final AlertDialog dialog = Dialogs.alertDialog(null
				, StringManager.getString(R.string.alert_delete_title)
				, StringManager.getString(R.string.alert_delete_message_single)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					delete();
				}
			}
		}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	public void confirmRemove(final String toId) {

		String message = String.format(StringManager.getString(R.string.alert_remove_message_single_no_name), mEntity.name);
		Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);
		if (linkPlace != null) {
			message = String.format(StringManager.getString(R.string.alert_remove_message_single), linkPlace.shortcut.name);
		}

		final AlertDialog dialog = Dialogs.alertDialog(null
				, StringManager.getString(R.string.alert_remove_title)
				, message
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					remove(toId);
				}
			}
		}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void clearReferences() {
		Activity currentActivity = Patchr.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.equals(this)) {
			Patchr.getInstance().setCurrentActivity(null);
		}
	}

	protected void delete() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, mDeleteProgressResId, BaseActivity.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				return DataController.getInstance().deleteEntity(mEntity.id, false, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + mEntity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(mDeletedResId), Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_ENTITY_DELETED);
					finish();
					AnimationManager.doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void remove(final String toId) {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, mRemoveProgressResId, BaseActivity.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRemoveEntity");
				final ModelResult result = DataController.getInstance()
						.removeLinks(mEntity.id, toId, Constants.TYPE_LINK_CONTENT, mEntity.schema, "remove_entity_message", NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				isCancelled();
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Removed entity: " + mEntity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(mRemovedResId), Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_ENTITY_REMOVED);
					finish();
					AnimationManager.doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public Boolean related(@NonNull String entityId) {
		return (mEntityId != null && entityId.equals(mEntityId));
	}

	public void checkForPreferenceChanges() {

		mPrefChangeNewSearchNeeded = false;
		mPrefChangeRefreshUiNeeded = false;

		/* Dev prefs */

		if (!Patchr.getInstance().getPrefEnableDev()
				.equals(Patchr.settings.getBoolean(Preference.ENABLE_DEV, false))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: dev ui");
		}

		if (!Patchr.getInstance().getPrefTestingBeacons().equals(Patchr.settings.getString(Preference.TESTING_BEACONS, StringManager.getString(R.string.pref_testing_beacons_default)))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
		}

		if (mPrefChangeNewSearchNeeded || mPrefChangeRefreshUiNeeded) {
			Patchr.getInstance().snapshotPreferences();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public void setResultCode(int resultCode) {
		setResult(resultCode);
		Patchr.resultCode = resultCode;
	}

	public void setResultCode(int resultCode, Intent intent) {
		setResult(resultCode, intent);
		Patchr.resultCode = resultCode;
	}

	public void setActivityTitle(String title) {
		mActivityTitle = title;
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(title);
		}
	}

	public String getActivityTitle() {
		return (String) ((mActivityTitle != null) ? mActivityTitle : getTitle());
	}

	public Menu getOptionMenu() {
		return mOptionMenu;
	}

	public String getEntityId() {
		return mEntityId;
	}

	public Entity getEntity() {
		return mEntity;
	}

	public Fragment getCurrentFragment() {
		return null;
	}

	public Boolean getRestarting() {
		return mRestarting;
	}

	protected int getLayoutId() {
		return 0;
	}

	protected Toolbar getActionBarToolbar() {
		if (mActionBarToolbar == null) {
			mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
			if (mActionBarToolbar != null) {
				super.setSupportActionBar(mActionBarToolbar);
			}
		}
		return mActionBarToolbar;
	}

	protected Integer getExitTransitionType() {
		Integer transitionType = TransitionType.FORM_BACK;
		if (mTransitionType != TransitionType.FORM_TO) {
			if (mTransitionType == TransitionType.DRILL_TO) {
				transitionType = TransitionType.DRILL_BACK;
			}
			else if (mTransitionType == TransitionType.BUILDER_TO) {
				transitionType = TransitionType.BUILDER_BACK;
			}
			else if (mTransitionType == TransitionType.VIEW_TO) {
				transitionType = TransitionType.VIEW_BACK;
			}
			else if (mTransitionType == TransitionType.DIALOG_TO) {
				transitionType = TransitionType.DIALOG_BACK;
			}
		}
		return transitionType;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public enum ServiceOperation {
		SIGNIN,
		PASSWORD_CHANGE,
	}
}