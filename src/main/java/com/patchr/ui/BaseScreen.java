package com.patchr.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.NfcManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.Notification;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.MenuTint;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

public abstract class BaseScreen extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener
		, SwipeRefreshLayout.OnRefreshListener {

	public    Toolbar            toolbar;
	public    TextView           actionBarTitle;
	public    ActionBar          actionBar;
	public    View               actionBarGroup;
	public    AppBarLayout       appBarLayout;
	public    SwipeRefreshLayout swipeRefresh;
	protected BusyPresenter      busyPresenter;
	protected EmptyPresenter     emptyPresenter;

	protected View     rootView;
	public    Entity   entity;
	public    String   entityId;
	public    Fragment currentFragment;

	public    Boolean firstDraw  = true;
	protected Boolean processing = false;

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

		NfcManager.pushUri(Uri.parse("http://patchr.com"), this);   // Default

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
		}

		this.emptyPresenter = new EmptyPresenter(findViewById(R.id.list_message));
		this.busyPresenter = new BusyPresenter();
		this.busyPresenter.setProgressBar(findViewById(R.id.list_progress));
		this.busyPresenter.swipeRefreshLayout = this.swipeRefresh;

		this.toolbar = (Toolbar) this.rootView.findViewById(R.id.actionbar_toolbar);
		this.actionBarGroup = this.rootView.findViewById(R.id.toolbar);
		this.appBarLayout = (AppBarLayout) this.rootView.findViewById(R.id.appbar_layout);

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

		/* Party on! */
		initialize(savedInstanceState);

		String screenName = getScreenName();
		if (screenName != null) {
			Reporting.screen(AnalyticsCategory.VIEW, screenName);
		}
	}

	@Override protected void onResume() {
		super.onResume();

		if (this.appBarLayout != null) {
			this.appBarLayout.addOnOffsetChangedListener(this);
		}

		busyPresenter.onResume();
		processing = false;
		/*
		 * We always check to make sure play services are working properly. This call will finish
		 * the activity if play services are missing and can't be installed or if the user
		 * refuses to install them. If play services can be fixed, then resume will be called again.
		 */
		AndroidManager.checkPlayServices(this);
	}

	@Override protected void onPause() {
		busyPresenter.onPause();
		if (this.appBarLayout != null) {
			this.appBarLayout.removeOnOffsetChangedListener(this);
		}
		super.onPause();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		((EntityListFragment) this.currentFragment).fetch(FetchMode.MANUAL);
	}

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

	@Override public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
		if (this.swipeRefresh != null) {
			this.swipeRefresh.setEnabled(i == 0);
		}
	}

	public void submitAction() { /* Handled by child classes */}

	public void cancelAction(Boolean force) {   // Chance for activity to intercept to confirm
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, getTransitionBack(TransitionType.FORM_BACK));
	}

	public void onFetchComplete() {
		processing = false;
		if (busyPresenter != null) {
			busyPresenter.hide(false);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void unpackIntent() {
		/*
		 * Unpack all the inputs.
		 */
	}

	public void initialize(Bundle savedInstanceState) {
		/*
		 * Perform all setup that should only happen once and
		 * only after the view tree is available.
		 */
	}

	public void navigateToPhoto(Photo photo) {
		final String jsonPhoto = Json.objectToJson(photo);
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
		Patchr.router.route(this, Command.PHOTO, null, extras);
	}

	public void navigateToEntity(Entity entity) {
		final Bundle extras = new Bundle();

		if (entity instanceof Notification) {
			Notification notification = (Notification) entity;
			Patchr.router.browse(this, notification.targetId, extras, true);
		}
		else {
			Patchr.router.browse(this, entity.id, extras, true);
		}
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
		Link linkPlace = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);
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

	protected void delete() {

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_deleting, BaseScreen.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				return DataController.getInstance().deleteEntity(entity.id, false, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				processing = false;
				busyPresenter.hide(true);

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
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_removing, BaseScreen.this);
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

				busyPresenter.hide(true);
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
		public static String Editing    = "editing";
		public static String Creating   = "creating";
		public static String Onboarding = "onboarding";
		public static String Sharing    = "sharing";
		public static String Searching  = "searching";
	}
}