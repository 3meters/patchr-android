package com.aircandi.ui.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NfcManager;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBind;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.interfaces.IForm;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.MapListFragment;
import com.aircandi.ui.PhotoForm;
import com.aircandi.ui.components.BusyController;
import com.aircandi.ui.components.MessageController;
import com.aircandi.ui.components.UiController;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends ActionBarActivity
		implements OnRefreshListener, IForm, IBind {

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

	protected String        mActivityTitle;
	protected Entity        mEntity;
	protected String        mEntityId;
	public    String        mForId;
	protected SimpleMonitor mEntityMonitor;

	/* Fragments */
	protected Map<String, Fragment> mFragments = new HashMap<String, Fragment>();
	protected Fragment mCurrentFragment;
	protected String   mCurrentFragmentTag;
	protected String   mNextFragmentTag;
	protected String   mPrevFragmentTag;

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
	protected Boolean mPrefChangeReloadNeeded    = false;

	public Resources mResources;
	public    Boolean mFirstDraw    = true;
	protected Boolean mClickEnabled = false;                        // NO_UCD (unused code)
	protected Boolean mNotEmpty     = false;
	protected Boolean mProcessing   = false;
	protected Boolean mRestarting   = false;

	/* Theme */
	protected String mPrefTheme;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    /*
	     * We do all this here so the work is finished before subclasses start
		 * their create/initialize processing.
		 */
		Logger.d(this, "Activity created");
		if (Patchr.firstStartApp) {
			Logger.d(this, "App unstarted: redirecting to splash");
			Patchr.firstStartIntent = getIntent();
			Patchr.dispatch.route(this, Route.SPLASH, null, null);
			super.onCreate(savedInstanceState);
			finish();
		}
		else {
			mResources = getResources();
			/*
			 * Theme must be set before contentView is processed.
			 */
			setTheme(isDialog(), isTransparent());
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

			/* Cheat some data from the extras early */
			final Bundle extras = getIntent().getExtras();
			if (extras != null) {
				mLayoutResId = extras.getInt(Constants.EXTRA_LAYOUT_RESID);
			}

			super.setContentView(getLayoutId());
			super.onCreate(savedInstanceState);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

			/* View layout event */
			final View view = getWindow().getDecorView().findViewById(android.R.id.content);
			if (view != null && view.getViewTreeObserver().isAlive()) {
				view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

					public void onGlobalLayout() {
						//noinspection deprecation
						view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						onViewLayout();  // Only used by PatchForm
					}
				});
			}

			/* Nfc */
			Uri uri = Uri.parse("http://3meters.com/qrcode");
			NfcManager.pushUri(uri, this);

			/* Event sequence */
			unpackIntent();
			initialize(savedInstanceState);
			setCurrentFragment(mNextFragmentTag);
			getActionBarToolbar(); // Init the toolbar as action bar
		}
	}

	protected void onPostCreate(Bundle savedInstanceState) {
		configureActionBar();
		super.onPostCreate(savedInstanceState);
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mLayoutResId = extras.getInt(Constants.EXTRA_LAYOUT_RESID);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		mUiController.setBusyController(new BusyController());
		mUiController.getBusyController().setProgressBar(findViewById(R.id.form_progress));
		mUiController.setMessageController(new MessageController(findViewById(R.id.form_message)));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onViewLayout() {
		/*
		 * Called when initial view layout has completed and
		 * views have been measured and sized.
		 */
		Logger.d(this, "Activity view layout completed");
	}

	@SuppressWarnings("ucd")
	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patchr.dispatch.route(this, Route.PHOTO, null, extras);
		}
	}

	@Override
	public void onRefresh() {}

	public void onAccept() {}

	@Override
	public void onAdd(Bundle extras) {
		/* Schema target is in the extras */
		Patchr.dispatch.route(this, Route.NEW, mEntity, extras);
	}

	@Override
	public void onBackPressed() {
		if (mDrawerLayout != null) {
			if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
				mNotificationActionIcon.animate().rotation(0f).setDuration(200);
				mDrawerLayout.closeDrawer(mDrawerRight);
				return;
			}
			else if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
				mDrawerLayout.closeDrawer(mDrawerLeft);
				return;
			}
		}

		if (mCurrentFragmentTag != null && mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
			String listFragment = ((MapListFragment) getCurrentFragment()).getListFragment();
			if (listFragment != null) {
				Bundle extras = new Bundle();
				extras.putString(Constants.EXTRA_FRAGMENT_TYPE, listFragment);
				Patchr.dispatch.route(this, Route.VIEW_AS_LIST, null, extras);
			}
			return;
		}

		if (BaseActivity.this instanceof AircandiForm) {
			super.onBackPressed();
		}
		else {
			Patchr.dispatch.route(this, Route.CANCEL, null, null);
		}
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, getExitTransitionType());
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	@SuppressWarnings("ucd")
	public void onCancelButtonClick(View view) {
		Patchr.dispatch.route(this, Route.CANCEL, null, null);
	}

	@Override
	public void onHelp() {}

	@Override
	public void onError() {}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Patchr.resultCode = Activity.RESULT_OK;
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void setCurrentFragment(String fragmentType) {}

	protected void configureActionBar() {
		/*
		 * By default we show the nav indicator and the title.
		 */
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		getActionBarToolbar().setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}

	@Override
	public void draw(View view) {}

	@Override
	public void bind(BindingMode mode) {}

	protected Toolbar getActionBarToolbar() {
		if (mActionBarToolbar == null) {
			mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
			if (mActionBarToolbar != null) {
				setSupportActionBar(mActionBarToolbar);
			}
		}
		return mActionBarToolbar;
	}

	public void signout() {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_signing_out, BaseActivity.this);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncSignOut");
						final ModelResult result = Patchr.getInstance().getEntityController().signoutComplete(NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
						return result;
					}

					@SuppressLint("NewApi")
					@Override
					protected void onPostExecute(Object response) {
						/* Set to anonymous user even if service call fails */
						UI.showToastNotification(StringManager.getString(R.string.alert_signed_out), Toast.LENGTH_SHORT);
						mUiController.getBusyController().hide(false);
						Patchr.dispatch.route(BaseActivity.this, Route.SPLASH, null, null);
					}
				}.executeOnExecutor(Constants.EXECUTOR);
			}
		};

		runOnUiThread(task);
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

	public void setResultCode(int resultCode) {
		setResult(resultCode);
		Patchr.resultCode = resultCode;
	}

	public void setResultCode(int resultCode, Intent intent) {
		setResult(resultCode, intent);
		Patchr.resultCode = resultCode;
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
				final ModelResult result = Patchr.getInstance().getEntityController().deleteEntity(mEntity.id, false, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
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
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
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
				final ModelResult result = Patchr.getInstance().getEntityController()
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
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public Boolean related(String entityId) {
		return false;
	}

	@Override
	public void share() {}

	public void setTheme(Boolean isDialog, Boolean isTransparent) {
		mPrefTheme = Patchr.settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default));
		/*
		 * Need to use application context so our app level themes and attributes are available to actionbarsherlock
		 */
		Integer themeId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		if (isDialog) {
			themeId = R.style.aircandi_theme_dialog_dark;
			if (mPrefTheme.equals("aircandi_theme_snow")) {
				themeId = R.style.aircandi_theme_dialog_light;
			}
		}
		else if (isTransparent) {
			themeId = R.style.aircandi_theme_midnight_transparent;
			if (mPrefTheme.equals("aircandi_theme_snow")) {
				themeId = R.style.aircandi_theme_snow_transparent;
			}
		}

		setTheme(themeId);
		final TypedValue resourceName = new TypedValue();
		if (getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			if (resourceName.coerceToString() != null) {
				Patchr.themeTone = (String) resourceName.coerceToString();
			}
		}
	}

	public void checkForPreferenceChanges() {

		mPrefChangeNewSearchNeeded = false;
		mPrefChangeRefreshUiNeeded = false;
		mPrefChangeReloadNeeded = false;

		/* Common prefs */

		if (!mPrefTheme.equals(Patchr.settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default)))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			mPrefChangeReloadNeeded = true;
		}

		/* Dev prefs */

		if (!Patchr.getInstance().getPrefEnableDev()
		           .equals(Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: dev ui");
		}

		if (!Patchr.getInstance().getPrefTestingBeacons().equals(Patchr.settings.getString(StringManager.getString(R.string.pref_testing_beacons),
				StringManager.getString(R.string.pref_testing_beacons_default)))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
		}

		if (mPrefChangeNewSearchNeeded || mPrefChangeRefreshUiNeeded || mPrefChangeReloadNeeded) {
			Patchr.getInstance().snapshotPreferences();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	protected int getLayoutId() {
		return 0;
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

	protected Boolean isDialog() {
		return false;
	}

	protected Boolean isTransparent() {
		return false;
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

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Logger.v(this, "Creating options menu");
		/*
		 * - activity is first started.
		 * - switching fragments.
		 * - after invalidateOptionsMenu.
		 * - after onResume in lifecycle.
		 */
		Patchr.getInstance().getMenuManager().onCreateOptionsMenu(this, menu);
		mOptionMenu = menu;
		configureStandardMenuItems(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Bundle extras = null;
		if (item.getItemId() == android.R.id.home) {
			if (mDrawerToggle != null) {
				mDrawerToggle.onOptionsItemSelected(item);
			}
			if (mDrawerLayout != null) {
				if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
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
			String address = null;
			if (mEntity instanceof Place && mEntity.fuzzy) {
				address = ((Place) mEntity).getAddressString(true);
			}

			AndroidManager.getInstance().callMapNavigation(this
					, location.lat.doubleValue()
					, location.lng.doubleValue()
					, address
					, mEntity.name);
			return true;
		}

		Patchr.dispatch.route(this, Patchr.dispatch.routeForMenuId(item.getItemId()), mEntity, extras);
		return true;
	}

	protected void configureStandardMenuItems(Menu menu) {

		MenuItem menuItem = menu.findItem(R.id.edit);
		if (menuItem != null) {
			menuItem.setVisible(Patchr.getInstance().getMenuManager().showAction(Route.EDIT, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.add);
		if (menuItem != null && mEntity != null) {
			menuItem.setVisible(Patchr.getInstance().getMenuManager().showAction(Route.ADD, mEntity, mForId));
			menuItem.setTitle(StringManager.getString(R.string.menu_item_add_entity, mEntity.schema));
		}

		menuItem = menu.findItem(R.id.delete);
		if (menuItem != null) {
			menuItem.setVisible(Patchr.getInstance().getMenuManager().showAction(Route.DELETE, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.remove);
		if (menuItem != null) {
			menuItem.setVisible(Patchr.getInstance().getMenuManager().showAction(Route.REMOVE, mEntity, mForId));
		}

		menuItem = menu.findItem(R.id.signin);
		if (menuItem != null && Patchr.getInstance().getCurrentUser() != null) {
			menuItem.setVisible(Patchr.getInstance().getCurrentUser().isAnonymous());
		}

		menuItem = menu.findItem(R.id.navigate);
		if (menuItem != null && Patchr.getInstance().getCurrentUser() != null) {
			menuItem.setVisible(mEntity.getLocation() != null);
		}

		menuItem = menu.findItem(R.id.share);
		if (menuItem != null) {
			if (this instanceof PhotoForm) {
				menuItem.setVisible(Patchr.getInstance().getMenuManager().showAction(Route.SHARE, mEntity, mForId));
			}
		}

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			mNotificationActionIcon = MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_image);
			MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_frame).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
						mDrawerLayout.closeDrawer(mDrawerLeft);
					}
					if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
						mNotificationActionIcon.animate().rotation(0f).setDuration(200);
						mDrawerLayout.closeDrawer(mDrawerRight);
					}
					else {
						mNotificationActionIcon.animate().rotation(90f).setDuration(200);
						mDrawerLayout.openDrawer(mDrawerRight);
					}
				}
			});

			if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
				View view = MenuItemCompat.getActionView(notifications);
				mNotificationsBadgeGroup = view.findViewById(R.id.badge_group);
				mNotificationsBadgeCount = (TextView) view.findViewById(R.id.badge_count);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onRestart() {
		Logger.d(this, "Activity restarting");
		super.onRestart();
		checkForPreferenceChanges();
	}

	@Override
	protected void onStart() {
		/* Called everytime the activity is started or restarted. */
		super.onStart();
		if (!isFinishing()) {
			Logger.d(this, "Activity starting");

			if (mPrefChangeReloadNeeded) {
				/*
				 * Restarts this activity using the same intent as used for the previous start.
				 */
				mRestarting = true;
				final Intent originalIntent = getIntent();
				originalIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				finish();
				overridePendingTransition(0, 0);
				startActivity(originalIntent);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.d(this, "Activity resuming");
		Dispatcher.getInstance().register(this);
		Patchr.getInstance().setCurrentActivity(this);
		mClickEnabled = true;
		/*
		 * We always check to make sure play services are working properly. This call will finish 
		 * the activity if play services are missing and can't be installed or if the user
		 * refuses to install them. If play services can be fixed, then resume will be called again.
		 */
		AndroidManager.checkPlayServices(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(this, "Activity pausing");
		Dispatcher.getInstance().unregister(this);
		clearReferences();
	}

	@Override
	protected void onStop() {
		Logger.d(this, "Activity stopping");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "Activity destroying");
		super.onDestroy();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public enum ServiceOperation {
		SIGNIN,
		PASSWORD_CHANGE,
	}
}