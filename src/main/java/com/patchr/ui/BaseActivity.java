package com.patchr.ui;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

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
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.UiController;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

public abstract class BaseActivity extends AppCompatActivity {

	public    Toolbar      toolbar;
	public    ActionBar    actionBar;
	protected UiController uiController;
	public    Menu         optionMenu;
	protected View         rootView;
	public    Entity       entity;
	public    String       entityId;
	protected Fragment     currentFragment;

	protected Integer transitionType = TransitionType.FORM_TO;
	public    Boolean firstDraw      = true;
	protected Boolean processing     = false;

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

		this.uiController = new UiController();
		this.uiController.setBusyController(new BusyController());
		this.uiController.getBusyController().setProgressBar(findViewById(R.id.form_progress));
		this.uiController.setMessageController(new EmptyController(findViewById(R.id.form_message)));

		this.toolbar = (Toolbar) this.rootView.findViewById(R.id.toolbar);
		if (this.toolbar != null) {
			super.setSupportActionBar(toolbar);
			this.actionBar = super.getSupportActionBar();
			/*
			 * By default we show the nav indicator and the title.
			 */
			if (this.actionBar != null) {
				this.actionBar.setDisplayShowTitleEnabled(true);
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
	}

	@Override protected void onResume() {
		super.onResume();

		uiController.onResume();
		processing = false;
		/*
		 * We always check to make sure play services are working properly. This call will finish
		 * the activity if play services are missing and can't be installed or if the user
		 * refuses to install them. If play services can be fixed, then resume will be called again.
		 */
		AndroidManager.checkPlayServices(this);
	}

	@Override protected void onPause() {
		uiController.onPause();
		super.onPause();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onBackPressed() {
		if (BaseActivity.this instanceof AircandiForm) {
			super.onBackPressed();
		}
		else {
			Patchr.router.route(this, Route.CANCEL, null, null);    // Activities can use onCancel to confirm discard etc.
		}
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
		this.optionMenu = menu;
		configureStandardMenuItems(menu);
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		Bundle extras = null;
		if (item.getItemId() == R.id.remove && entity.patchId != null) {
		    /*
		     * We use patchId instead of toId so we can removed replies where
             * toId points to the root message.
             */
			extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, entity.patchId);
		}

		if (item.getItemId() == R.id.navigate) {
			AirLocation location = entity.getLocation();
			if (location == null) {
				throw new IllegalArgumentException("Tried to navigate without a location");
			}

			AndroidManager.getInstance().callMapNavigation(this
					, location.lat.doubleValue()
					, location.lng.doubleValue()
					, null
					, entity.name);
			return true;
		}

		Patchr.router.route(this, Patchr.router.routeForMenuId(item.getItemId()), entity, extras);
		return true;
	}

	public void onAdd(Bundle extras) {

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to post messages, make patches and more.");
			return;
		}

		/* Schema target is in the extras */
		Patchr.router.route(this, Route.NEW, entity, extras);
	}

	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity == null) return;

		Bundle extras = new Bundle();
		Patchr.router.route(this, Route.BROWSE, entity, extras);
	}

	public void onSubmit() {}

	public void onCancel(Boolean force) {   // Chance for activity to intercept to confirm
		finish();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
		}
	}

	public void initialize(Bundle savedInstanceState) {
		/*
		 * Perform all setup that should only happen once and
		 * only after the view tree is available.
		 */
	}

	protected void configureStandardMenuItems(Menu menu) {

		MenuItem menuItem = menu.findItem(R.id.edit);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.EDIT, entity));
		}

		menuItem = menu.findItem(R.id.delete);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.DELETE, entity));
		}

		menuItem = menu.findItem(R.id.remove);
		if (menuItem != null) {
			menuItem.setVisible(MenuManager.showAction(Route.REMOVE, entity));
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
			if (this instanceof PhotoForm) {
				menuItem.setVisible(MenuManager.showAction(Route.SHARE, entity));
			}
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
				uiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_deleting, BaseActivity.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				return DataController.getInstance().deleteEntity(entity.id, false, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				uiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + entity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					finish();
					AnimationManager.doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void remove(final String toId) {

		new AsyncTask() {

			@Override protected void onPreExecute() {
				uiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_removing, BaseActivity.this);
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

				uiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Removed entity: " + entity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(R.string.alert_removed), Toast.LENGTH_SHORT);
					finish();
					AnimationManager.doOverridePendingTransition(BaseActivity.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseActivity.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected int getLayoutId() {
		return 0;
	}
}