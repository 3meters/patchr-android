package com.aircandi.ui.base;

import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.R.color;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.Extras;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;
import com.nineoldandroids.view.ViewHelper;

public abstract class BaseActivity extends SherlockFragmentActivity implements OnRefreshListener, IForm, IBind {

	protected ActionBar		mActionBar;
	protected String		mActivityTitle;
	protected Entity		mEntity;
	protected String		mEntityId;
	protected SimpleMonitor	mEntityMonitor;

	/* Inputs */
	protected Extras		mParams						= new Extras();

	/* Resources */
	protected Integer		mDeleteProgressResId		= R.string.progress_deleting;
	protected Integer		mDeletedResId				= R.string.alert_deleted;
	protected Integer		mRemoveProgressResId		= R.string.progress_removing;
	protected Integer		mRemovedResId				= R.string.alert_removed;
	protected Integer		mLayoutResId;

	protected Boolean		mPrefChangeNewSearchNeeded	= false;
	protected Boolean		mPrefChangeRefreshUiNeeded	= false;
	protected Boolean		mPrefChangeReloadNeeded		= false;

	public Resources		mResources;
	public BusyManager		mBusy;
	public Boolean			mFirstDraw					= true;
	protected Boolean		mInvalidated				= false;
	protected Boolean		mClickEnabled				= false;						// NO_UCD (unused code)
	protected Boolean		mLoaded						= false;
	protected Button		mButtonSpecial;

	/* Theme */
	protected String		mPrefTheme;

	/* Menus */
	protected Menu			mMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/*
		 * We do all this here so the work is finished before subclasses start
		 * their create/initialize processing.
		 */
		Logger.d(this, "Activity created");
		if (Aircandi.firstStartApp) {
			Aircandi.firstStartIntent = getIntent();
			Aircandi.dispatch.route(this, Route.SPLASH, null, null, null);
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

			/* Set busy manager */
			mBusy = new BusyManager(this);

			/* Stash the action bar */
			mActionBar = getSupportActionBar();

			/* Fonts */
			final Integer titleId = getActionBarTitleId();
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(titleId));

			/* Super base ui */
			mButtonSpecial = (Button) findViewById(R.id.button_special);
			if (mButtonSpecial != null) {
				ViewHelper.setAlpha(mButtonSpecial, 0);
				mButtonSpecial.setClickable(false);
			}

			/* Event sequence */
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			unpackIntent();
			initialize(savedInstanceState);
			if (!isFinishing()) {
				configureActionBar();
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {}

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mLayoutResId = extras.getInt(Constants.EXTRA_LAYOUT_RESID);
		}
	}

	protected void configureActionBar() {

		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
			Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_logo_dark);
			icon.setColorFilter(Colors.getColor(color.white), PorterDuff.Mode.SRC_ATOP);
			mActionBar.setIcon(icon);
		}

		/*
		 * Force the display of the action bar overflow item
		 */
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}
		catch (Exception ignore) {}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onOverflowButtonClick(View view) {
		popupMenu(view);
	}

	@SuppressWarnings("ucd")
	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Aircandi.dispatch.route(this, Route.PHOTO, null, null, extras);
		}
	}

	@Override
	public void onRefresh() {}

	public void onAccept() {}

	@Override
	public void onAdd(Bundle extras) {
		/* Schema target is in the extras */
		Aircandi.dispatch.route(this, Route.NEW, mEntity, null, extras);
	}

	@Override
	public void onBackPressed() {
		if (BaseActivity.this instanceof AircandiForm) {
			super.onBackPressed();
		}
		else {
			Aircandi.dispatch.route(this, Route.CANCEL, null, null, null);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	@SuppressWarnings("ucd")
	public void onCancelButtonClick(View view) {
		Aircandi.dispatch.route(this, Route.CANCEL, null, null, null);
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_BACK);
	}

	@Override
	public void onHelp() {}

	@Override
	public void onError() {}

	@Override
	public void onLowMemory() {
		Aircandi.tracker.sendEvent(TrackerCategory.SYSTEM, "memory_low", null, Utilities.getMemoryAvailable());
		super.onLowMemory();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

//		if (requestCode == AndroidManager.PLAY_SERVICES_RESOLUTION_REQUEST) {
//			if (resultCode == RESULT_CANCELED) {
//				UI.showToastNotification(StringManager.getString(R.string.error_google_play_services_unavailable), Toast.LENGTH_LONG);
//				finish();
//				return;
//			}
//		}
		Aircandi.resultCode = Activity.RESULT_OK;
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	protected int getLayoutId() {
		return 0;
	}

	public int getActionBarTitleId() {
		Integer actionBarTitleId = null;
		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				actionBarTitleId = Class.forName("com.actionbarsherlock.R$id").getField("abs__action_bar_title").getInt(null);
			}
			else {
				// Use reflection to get the actionbar title TextView and set the custom font. May break in updates.
				actionBarTitleId = Class.forName("com.android.internal.R$id").getField("action_bar_title").getInt(null);
			}
		}
		catch (Exception e) {
			if (Aircandi.DEBUG) {
				e.printStackTrace();
			}
		}
		return actionBarTitleId;
	}

	public void setActivityTitle(String title) {
		mActivityTitle = title;
		mActionBar.setTitle(title);
	}

	public String getActivityTitle() {
		return (String) ((mActivityTitle != null) ? mActivityTitle : getTitle());
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

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {};

	@Override
	public void bind(BindingMode mode) {}

	protected void showButtonSpecial(Boolean visible, String message) {
		if (mButtonSpecial != null) {
			if (message != null) {
				mButtonSpecial.setText(message);
			}
			UI.animateView(mButtonSpecial, visible, false, AnimationManager.DURATION_MEDIUM);
		}
	}

	public BusyManager getBusy() {
		return mBusy;
	}

	public static void signout(final Activity activity, final Boolean silent) {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						if (!silent && activity instanceof BaseActivity) {
							((BaseActivity) activity).mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_signing_out);
						}
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncSignOut");
						final ModelResult result = Aircandi.getInstance().getEntityManager().signoutComplete();
						return result;
					}

					@SuppressLint("NewApi")
					@Override
					protected void onPostExecute(Object response) {
						if (!silent) {
							/* Notify interested parties */
							UI.showToastNotification(StringManager.getString(R.string.alert_signed_out), Toast.LENGTH_SHORT);
							if (activity instanceof BaseActivity) {
								((BaseActivity) activity).mBusy.hideBusy(false);
							}
							Aircandi.dispatch.route(activity, Route.SPLASH, null, null, null);
						}
					}
				}.execute();
			}
		};

		if (!silent && activity != null) {
			activity.runOnUiThread(task);
		}
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
		Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE);
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
		Activity currentActivity = Aircandi.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.equals(this)) {
			Aircandi.getInstance().setCurrentActivity(null);
		}
	}

	public void setResultCode(int resultCode) {
		setResult(resultCode);
		Aircandi.resultCode = resultCode;
	}

	public void setResultCode(int resultCode, Intent intent) {
		setResult(resultCode, intent);
		Aircandi.resultCode = resultCode;
	}

	protected void delete() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, mDeleteProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				final ModelResult result = Aircandi.getInstance().getEntityManager().deleteEntity(mEntity.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + mEntity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(mDeletedResId), Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_ENTITY_DELETED);
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
			}

		}.execute();
	}

	protected void remove(final String toId) {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, mRemoveProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRemoveEntity");
				final ModelResult result = Aircandi.getInstance().getEntityManager()
						.removeLinks(mEntity.id, toId, Constants.TYPE_LINK_CONTENT, mEntity.schema, "remove");
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Removed entity: " + mEntity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(mRemovedResId), Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_ENTITY_REMOVED);
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
			}

		}.execute();
	}

	public Boolean related(Entity entity) {
		return false;
	}

	public Boolean related(String entityId) {
		return false;
	}

	@Override
	public void share() {}

	public void setTheme(Boolean isDialog, Boolean isTransparent) {
		mPrefTheme = Aircandi.settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default));
		/*
		 * ActionBarSherlock takes over the title area if version < 4.0 (Ice Cream Sandwich).
		 */
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
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
			Aircandi.themeTone = (String) resourceName.coerceToString();
		}
	}

	public void checkForPreferenceChanges() {

		mPrefChangeNewSearchNeeded = false;
		mPrefChangeRefreshUiNeeded = false;
		mPrefChangeReloadNeeded = false;

		/* Common prefs */

		if (!mPrefTheme.equals(Aircandi.settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default)))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			mPrefChangeReloadNeeded = true;
		}

		if (!Aircandi
				.getInstance()
				.getPrefSearchRadius()
				.equals(Aircandi.settings.getString(StringManager.getString(R.string.pref_search_radius),
						StringManager.getString(R.string.pref_search_radius_default)))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: search radius");
		}

		/* Dev prefs */

		if (!Aircandi.getInstance().getPrefEnableDev()
				.equals(Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: dev ui");
		}

		if (!Aircandi.getInstance().getPrefTestingBeacons().equals(Aircandi.settings.getString(StringManager.getString(R.string.pref_testing_beacons),
				StringManager.getString(R.string.pref_testing_beacons_default)))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
		}

		if (mPrefChangeNewSearchNeeded || mPrefChangeRefreshUiNeeded || mPrefChangeReloadNeeded) {
			Aircandi.getInstance().snapshotPreferences();
		}
	}
	
	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Logger.v(this, "Creating options menu");
		/*
		 * Android 2.3 or lower: called when user hits the menu button for the first time.
		 * 
		 * Android 3.0 or higher:
		 * 1) called when activity is first started,
		 * 2) every time we tab to a new fragment.
		 * 
		 * onCreate->onStart->onResume->onCreateOptionsMenu-
		 */
		Aircandi.getInstance().getMenuManager().onCreateOptionsMenu(this, menu);
		mMenu = menu;

		return true;
	}

	public boolean onCreatePopupMenu(android.view.Menu menu, Entity entity) {
		return Aircandi.getInstance().getMenuManager().onCreatePopupMenu(this, menu, (entity != null) ? entity : mEntity);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Logger.v(this, "Preparing options menu");
		/*
		 * Android 2.3 or lower:
		 * 1) called after every call to onCreateOptionsMenu,
		 * 
		 * Android 3.0 or higher:
		 * 1) called after every call to onCreateOptionsMenu,
		 * 2) every time overflow menu is opened,
		 * 3) when invalidateOptionsMenu is called.
		 */
		MenuItem menuItem = menu.findItem(R.id.edit);
		if (menuItem != null) {
			menuItem.setVisible(Aircandi.getInstance().getMenuManager().showAction(Route.EDIT, mEntity));
		}

		menuItem = menu.findItem(R.id.add);
		if (menuItem != null && mEntity != null) {
			menuItem.setVisible(Aircandi.getInstance().getMenuManager().showAction(Route.ADD, mEntity));
			menuItem.setTitle(StringManager.getString(R.string.menu_item_add_entity, mEntity.schema));
		}

		menuItem = menu.findItem(R.id.delete);
		if (menuItem != null) {
			menuItem.setVisible(Aircandi.getInstance().getMenuManager().showAction(Route.DELETE, mEntity));
		}

		menuItem = menu.findItem(R.id.remove);
		if (menuItem != null) {
			menuItem.setVisible(Aircandi.getInstance().getMenuManager().showAction(Route.REMOVE, mEntity));
		}

		menuItem = menu.findItem(R.id.signin);
		if (menuItem != null) {
			menuItem.setVisible(Aircandi.getInstance().getCurrentUser().isAnonymous());
		}

		final MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusy != null) {
				mBusy.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusy.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
			}

			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					/*
					 * We always start the actionbar busy when the user clicks it.
					 */
					mBusy.startActionbarBusyIndicator();
					onOptionsItemSelected(refresh);
				}
			});
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Bundle extras = null;
		if (item.getItemId() == R.id.remove && mEntity.placeId != null) {
			extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntity.placeId);
		}
		Aircandi.dispatch.route(this, Aircandi.dispatch.routeForMenuId(item.getItemId()), mEntity, null, extras);
		return true;
	}

	@Override
	public void supportInvalidateOptionsMenu() {
		Logger.v(this, "Invalidating options menu");
		super.supportInvalidateOptionsMenu();
	}

	@SuppressLint("NewApi")
	public void popupMenu(View view) {

		if (Constants.SUPPORTS_HONEYCOMB) {

			final Entity entity = (Entity) view.getTag();
			PopupMenu popupMenu = new PopupMenu(this, view);
			onCreatePopupMenu(popupMenu.getMenu(), (entity != null) ? entity : mEntity);
			popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(android.view.MenuItem item) {

					if (item.getItemId() == R.id.report) {
						Bundle extras = new Bundle();
						extras.putString(Constants.EXTRA_ENTITY_SCHEMA, mEntity.getSchemaMapped());
						Aircandi.dispatch.route(BaseActivity.this, Route.REPORT, (entity != null) ? entity : mEntity, null, extras);
						return true;
					}
					else {
						Aircandi.dispatch.route(BaseActivity.this, Aircandi.dispatch.routeForMenuId(item.getItemId())
								, (entity != null) ? entity : mEntity, null, new Bundle());
						return true;
					}
				}
			});

			popupMenu.show();
		}
		else {
			gingerbreadPopupMenu();
		}
	}

	public void gingerbreadPopupMenu() {
		/*
		 * Builder constructor that takes theme requires api level 11.
		 */
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setInverseBackgroundForced(true);

		builder.setItems(R.array.more_options_entity, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0) {
					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, mEntity.getSchemaMapped());
					Aircandi.dispatch.route(BaseActivity.this, Route.REPORT, mEntity, null, extras);
				}
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

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
			Aircandi.tracker.activityStart(this);

			if (mPrefChangeReloadNeeded) {

				final Intent intent = getIntent();
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				finish();
				overridePendingTransition(0, 0);
				startActivity(intent);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.d(this, "Activity resuming");
		Aircandi.activityResumed();
		BusProvider.getInstance().register(this);
		Aircandi.getInstance().setCurrentActivity(this);
		mClickEnabled = true;

		/*
		 * We always check to make sure play services are working properly. This call will finish 
		 * the activity if play services are missing and can't be installed. If play services can
		 * be fixed, then resume will be called again.
		 */
		AndroidManager.checkPlayServices(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(this, "Activity pausing");
		BusProvider.getInstance().unregister(this);
		clearReferences();
		Aircandi.activityPaused();
	}

	@Override
	protected void onStop() {
		Logger.d(this, "Activity stopping");
		super.onStop();
		Aircandi.tracker.activityStop(this);
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "Activity destroying");
		super.onDestroy();
		clearReferences();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		final Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public Boolean getInvalidated() {
		return mInvalidated;
	}

	public void setInvalidated(Boolean invalidated) {
		mInvalidated = invalidated;
	}

	@SuppressWarnings("ucd")
	public enum ServiceOperation {
		SIGNIN,
		PASSWORD_CHANGE,
	}

	public static class SimpleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}