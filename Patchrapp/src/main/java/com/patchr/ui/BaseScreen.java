package com.patchr.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.ResponseCode;
import com.patchr.objects.TransitionType;
import com.patchr.ui.collections.PatchScreen;
import com.patchr.ui.collections.ProfileScreen;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.MenuTint;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import io.realm.Realm;

public abstract class BaseScreen extends AppCompatActivity {

	protected FloatingActionButton fab;
	protected Toolbar              toolbar;
	protected AppBarLayout         appBarLayout;
	public    TextView             actionBarTitle;
	public    ActionBar            actionBar;
	protected BusyController       busyController;
	protected AirProgressBar       progressBar;

	public String      entityId;
	public RealmEntity entity; // Here to support broadly shared commands (report, edit, share, etc.)
	public Realm       realm;  // Always on main thread

	protected View     rootView;
	public    Fragment currentFragment;

	public    Boolean firstDraw  = true;
	protected Boolean processing = false;
	protected boolean executed;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * We institute our own lifecycle markers.
		 * Derived classes can expect that:
		 * - unpackIntent is called before the view has been inflated.
		 * - initialize() is called after the view is inflated and base controllers have been created.
		 */
		unpackIntent();

		this.rootView = LayoutInflater.from(this).inflate(getLayoutId(), null, false);
		setContentView(this.rootView);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setupUI();

		/* Party on! */
		this.realm = Realm.getDefaultInstance();
		initialize(savedInstanceState);
		didInitialize();

		String screenName = getScreenName();
		if (screenName != null) {
			Reporting.screen(AnalyticsCategory.VIEW, screenName);
		}
	}

	@Override protected void onResume() {
		super.onResume();

		/* Delete check */
		if (this.entity != null) {
			RealmEntity realmEntity = realm.where(RealmEntity.class).equalTo("id", this.entityId).findFirst();
			if (realmEntity == null) {  // Entity was deleted while we where gone
				finish();
				return;
			}
		}

		busyController.onResume();
		processing = false;
		/*
		 * We always check to make sure play services are working properly. This call will finish
		 * the activity if play services are missing and can't be installed or if the user
		 * refuses to install them. If play services can be fixed, then resume will be called again.
		 */
		AndroidManager.checkPlayServices(this);
	}

	@Override protected void onPause() {
		super.onPause();
		busyController.onPause();
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		if (!this.realm.isClosed()) {
			this.realm.close();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onBackPressed() {
		if (BaseScreen.this instanceof MainScreen) {
			super.onBackPressed();
		}
		else {
			cancelAction(false);
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		MenuTint.on(menu).setMenuItemIconColor(Colors.getColor(R.color.brand_primary)).apply(this);
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == android.R.id.home) {
			cancelAction(false);
		}
		else if (item.getItemId() == R.id.report) {
			Patchr.router.route(this, Command.REPORT, entity, null);
		}
		else if (item.getItemId() == R.id.logout) {
			UserManager.shared().logout();
			Patchr.router.route(Patchr.applicationContext, Command.LOBBY, null, null);
		}
		else {
			/* Handles: login, browse, map */
			Patchr.router.route(this, Patchr.router.routeForMenuId(item.getItemId()), entity, null);
		}

		return true;
	}

	@Override public boolean onPrepareOptionsMenu(Menu menu) {
		configureStandardMenuItems(menu);
		return true;
	}

	public void submitAction() { /* Handled by child classes */}

	public void cancelAction(Boolean force) {   // Chance for activity to intercept to confirm
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, getTransitionBack(TransitionType.FORM_BACK));
	}

	public void onFetchComplete() {
		processing = false;
		if (busyController != null) {
			busyController.hide(false);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void unpackIntent() {
		/*
		 * Unpack all the inputs.
		 */
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
		}
	}

	public void initialize(Bundle savedInstanceState) {
		/*
		 * Perform all setup that should only happen once and
		 * only after the view tree is available.
		 */
		Utils.guard(this.rootView != null, "Root view cannot be null");
	}

	public void didInitialize() {}

	public void setupUI() {

		this.fab = (FloatingActionButton) this.rootView.findViewById(R.id.fab);
		this.appBarLayout = (AppBarLayout) this.rootView.findViewById(R.id.appbar_layout);
		this.toolbar = (Toolbar) this.rootView.findViewById(R.id.actionbar_toolbar);
		this.progressBar = (AirProgressBar) this.rootView.findViewById(R.id.list_progress);

		this.busyController = new BusyController(this.progressBar, null);

		if (this.toolbar != null) {
			super.setSupportActionBar(toolbar);
			this.actionBar = super.getSupportActionBar();
			this.actionBarTitle = (TextView) this.toolbar.findViewById(R.id.toolbar_title);
			/*
			 * By default we show the nav indicator and the title.
			 */
			if (this.actionBar != null) {
				this.actionBar.setDisplayShowTitleEnabled(false);
				this.actionBar.setDisplayHomeAsUpEnabled(true);
			}

			this.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onBackPressed();
				}
			});
		}
	}

	public void navigateToPhoto(Photo photo) {
		final String jsonPhoto = Patchr.gson.toJson(photo);
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
		Patchr.router.route(this, Command.PHOTO, null, extras);
	}

	public void navigateToEntity(RealmEntity entity) {

		String targetId = entity.shortcutForId != null ? entity.shortcutForId : entity.id;
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
			targetId = entity.targetId;
		}

		String schema = RealmEntity.getSchemaForId(targetId);

		Class<?> browseClass = MessageScreen.class;
		if (Constants.SCHEMA_ENTITY_PATCH.equals(schema)) {
			browseClass = PatchScreen.class;
		}
		else if (Constants.SCHEMA_ENTITY_USER.equals(schema)) {
			browseClass = ProfileScreen.class;
		}

		Intent intent = new Intent(this, browseClass);
		intent.putExtra(Constants.EXTRA_ENTITY_ID, targetId);
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	protected void configureStandardMenuItems(Menu menu) {

		MenuItem menuItem = menu.findItem(R.id.edit);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Command.EDIT, entity));
		}

		menuItem = menu.findItem(R.id.delete);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Command.DELETE, entity));
		}

		menuItem = menu.findItem(R.id.remove);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Command.REMOVE, entity));
		}

		menuItem = menu.findItem(R.id.login);
		if (menuItem != null) {
			menuItem.setVisible(!UserManager.shared().authenticated());
		}

		menuItem = menu.findItem(R.id.logout);
		if (menuItem != null) {
			menuItem.setVisible(UserManager.shared().authenticated());
		}

		menuItem = menu.findItem(R.id.navigate);
		if (menuItem != null && UserManager.shared().authenticated()) {
			menuItem.setVisible(entity.getLocation() != null);
		}

		menuItem = menu.findItem(R.id.share);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Command.SHARE, entity));
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

		String message = StringManager.getString(R.string.alert_remove_message_single_no_name);
		if (entity.patch != null && entity.patch.name != null) {
			message = String.format(StringManager.getString(R.string.alert_remove_message_single), entity.patch.name);
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

	protected void delete() {

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_deleting, BaseScreen.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				return DataController.getInstance().deleteEntity(entity.id, false, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				processing = false;
				busyController.hide(true);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Reporting.track(AnalyticsCategory.EDIT, "Deleted " + Utils.capitalize(entity.schema));
					Logger.i(this, "Deleted entity: " + entity.id);
					UI.toast(StringManager.getString(R.string.alert_deleted));
					setResult(Constants.RESULT_ENTITY_DELETED);
					finish();
				}
				else {
					Errors.handleError(BaseScreen.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void remove(final String toId) {

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_removing, BaseScreen.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRemoveEntity");
				final ModelResult result = DataController.getInstance()
					.removeLinks(entity.id, toId, Constants.TYPE_LINK_CONTENT, entity.schema, "remove_entity_message", NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				isCancelled();
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyController.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Reporting.track(AnalyticsCategory.EDIT, "Removed " + Utils.capitalize(entity.schema));
					Logger.i(this, "Removed entity: " + entity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.toast(StringManager.getString(R.string.alert_removed));
					finish();
				}
				else {
					Errors.handleError(BaseScreen.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public static void position(final View view, final View header, final Integer headerHeightProjected) {

		if (view != null && header != null) {

			ViewTreeObserver vto = header.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
				@Override public void onGlobalLayout() {

					if (Patchr.getInstance().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						int headerHeight = (headerHeightProjected != null)
						                   ? headerHeightProjected
						                   : header.getHeight();
						params.topMargin = headerHeight + UI.getRawPixelsForDisplayPixels(24f);
						view.setLayoutParams(params);
					}
					else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						view.setLayoutParams(params);
					}

					if (Constants.SUPPORTS_JELLY_BEAN) {
						header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
					else {
						header.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
	}

	protected int getLayoutId() {
		return 0;
	}

	protected String getScreenName() {
		return getClass().getSimpleName();
	}

	protected int getTransitionBack(int transitionType) {
		return transitionType;
	}

	public static class State {
		public static String Editing         = "editing";
		public static String Creating        = "creating";
		public static String Login           = "login";
		public static String Signup          = "signup";
		public static String Onboarding      = "onboarding";
		public static String CompleteProfile = "complete_profile";
		public static String Sharing         = "sharing";
		public static String Searching       = "searching";
	}
}