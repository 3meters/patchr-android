package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.actionbarsherlock.view.Window;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.ActivityDecorator;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.MenuManager;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.ShortcutManager;
import com.aircandi.controllers.Applinks;
import com.aircandi.controllers.Beacons;
import com.aircandi.controllers.Comments;
import com.aircandi.controllers.Maps;
import com.aircandi.controllers.Pictures;
import com.aircandi.controllers.Places;
import com.aircandi.controllers.Users;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

@SuppressLint("Registered")
public class SplashForm extends Activity {

	public BusyManager mBusy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(this, "Splash create");
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		requestWindowFeature((int) Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_form);
		initialize();
	}

	protected void initialize() {

		mBusy = new BusyManager(this);

		/* Brand coloring */
		ImageView image = (ImageView) findViewById(R.id.logo);
		UI.colorDrawable(image.getDrawable(), R.color.brand_primary, Mode.SRC_ATOP);
		ImageView background = (ImageView) findViewById(R.id.image_background);
		UI.colorDrawable(background.getDrawable(), Mode.MULTIPLY);

		/* Always reset the entity cache */
		EntityManager.getEntityCache().clear();

		if (!Aircandi.applicationUpdateRequired) {
			if (AndroidManager.checkPlayServices(this)) {
				prepareToRun();
			}
		}
		else {
			updateRequired();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	private void prepareToRun() {

		mBusy.showBusy(BusyAction.Refreshing);

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncPrepareToRun");
				ModelResult result = new ModelResult();

				if (Aircandi.firstStartApp) {
					configure();
					int maxAttempts = 5;
					int attempts = 1;
					while (attempts <= maxAttempts) {
						result.serviceResponse = MessagingManager.getInstance().registerInstallWithGCM();
						if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
							Logger.i(this, "Install registered with Gcm");
							break;
						}
						else {
							Logger.w(this, "Install failed to register with Gcm, attempt = " + attempts);
							try {
								Thread.sleep(2000);
							}
							catch (InterruptedException exception) {
								return result;
							}
						}
						attempts++;
					}
					Aircandi.firstStartApp = false;
				}

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					User user = Aircandi.getInstance().getCurrentUser();
					if (!user.isAnonymous()) {
						Links options = Aircandi.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_USER_CURRENT);
						result = Aircandi.getInstance().getEntityManager().getEntity(Aircandi.getInstance().getCurrentUser().id, true, options);
					}
				}

				/*
				 * We register installs even if the user is anonymous.
				 */
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					result = MessagingManager.getInstance().registerInstallWithAircandi();
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Splash initialized");
					if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
						showButtons(Buttons.ACCOUNT);
					}
					else {
						startHomeActivity();
					}
				}
				else {
					if (Errors.isNetworkError(result.serviceResponse)) {
						Errors.handleError(SplashForm.this, result.serviceResponse);
						showButtons(Buttons.RETRY);
					}
					else {
						Errors.handleError(SplashForm.this, result.serviceResponse);
						if (Aircandi.applicationUpdateRequired) {
							updateRequired();
							return;
						}
						showButtons(Buttons.ACCOUNT);
					}
				}
			}

		}.execute();
	}

	protected void configure() {
		/*
		 * Only called when app is first started
		 */
		Aircandi.getInstance()
		        .setMenuManager(new MenuManager())
		        .setActivityDecorator(new ActivityDecorator())
		        .setShortcutManager(new ShortcutManager())
		        .setEntityManager(new EntityManager().setLinks(new Links()))
		        .setMediaManager(new MediaManager().initSoundPool())
		        .setAnimationManager(new AnimationManager());

		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_APPLINK, new Applinks());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_BEACON, new Beacons());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_COMMENT, new Comments());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_PICTURE, new Pictures());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_PLACE, new Places());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_USER, new Users());
		Aircandi.controllerMap.put(Constants.TYPE_APP_MAP, new Maps());

		/* Start out with anonymous user then upgrade to signed in user if possible */
		Aircandi.getInstance().initializeUser();

		/* Stash last known location but doesn't start location updates */
		LocationManager.getInstance().initialize(getApplicationContext());

		Logger.i(this, "First run configuration completed");
	}

	protected void startHomeActivity() {
		if (!Aircandi.getInstance().getCurrentUser().isAnonymous() && Aircandi.firstStartIntent != null) {
			startActivity(Aircandi.firstStartIntent);
		}
		else {
			Aircandi.dispatch.route(this, Route.HOME, null, null, null);
		}

		/* Always ok to make sure firstStartIntent isn't still around */
		Aircandi.firstStartIntent = null;

		/*
		 * This is a hack to delay the finish. When executed immediately, we
		 * are getting a warning about lost windows because the activity hadn't completely
		 * started before it was being killed.
		 */
		Aircandi.mainThreadHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				finish();
			}
		}, 1000);
	}

	private void showButtons(Buttons buttons) {
		mBusy.hideBusy(false);
		if (buttons == Buttons.NONE) {
			findViewById(R.id.button_retry_holder).setVisibility(View.GONE);
			findViewById(R.id.button_holder).setVisibility(View.GONE);
		}
		else if (buttons == Buttons.RETRY) {
			findViewById(R.id.button_retry_holder).setVisibility(View.VISIBLE);
			findViewById(R.id.button_holder).setVisibility(View.GONE);
		}
		else if (buttons == Buttons.ACCOUNT) {
			findViewById(R.id.button_retry_holder).setVisibility(View.GONE);
			findViewById(R.id.button_holder).setVisibility(View.VISIBLE);
		}
	}

	private void updateRequired() {
		mBusy.hideBusy(false);
		Dialogs.updateApp(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Dialogs
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@SuppressWarnings("ucd")
	public void onSigninButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Aircandi.dispatch.route(this, Route.SIGNIN, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onStartButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		startHomeActivity();
	}

	@SuppressWarnings("ucd")
	public void onRetryButtonClick(View view) {
		showButtons(Buttons.NONE);
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
		}
		else {
			prepareToRun();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				/*
				 * Sign in handled
				 * - registered install with aircandi
				 * - loads data for signed in user
				 */
				startHomeActivity();
			}
		}
		else if (requestCode == AndroidManager.PLAY_SERVICES_RESOLUTION_REQUEST) {
			initialize();
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onStart() {
		super.onStart();
		Logger.d(this, "Splash start");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.d(this, "Splash resume");
		Aircandi.getInstance().setCurrentActivity(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(this, "Splash pause");
		clearReferences();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Logger.d(this, "Splash stop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Logger.d(this, "Splash destroy");
	}

	private void clearReferences() {
		Activity currentActivity = Aircandi.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.equals(this)) {
			Aircandi.getInstance().setCurrentActivity(null);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    private enum Buttons {
		ACCOUNT,
		RETRY,
		NONE
	}
}