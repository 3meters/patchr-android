package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.ActivityRecognitionManager;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.MenuManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;

@SuppressLint("Registered")
public class SplashForm extends Activity {

	protected SwipeRefreshLayout  mSwipeRefreshLayout;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(this, "Splash create");
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		requestWindowFeature((int) Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_form);
		initialize();
	}

	@SuppressLint("ResourceAsColor")
	protected void initialize() {

		mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);
		if (mSwipeRefreshLayout != null) {
			mSwipeRefreshLayout.setProgressBackgroundColor(R.color.brand_primary);
			mSwipeRefreshLayout.setColorSchemeResources(R.color.white);
			mSwipeRefreshLayout.setEnabled(false);
			mSwipeRefreshLayout.setProgressViewOffset(true, UI.getRawPixelsForDisplayPixels(48f), UI.getRawPixelsForDisplayPixels(48f));
		}

		/* Always reset the entity cache */
		EntityManager.getEntityCache().clear();

		/* Restart notification tracking */
		NotificationManager.getInstance().setNewNotificationCount(0);

		/* Restart crashlytics to force upload of non-fatal crashes */
		Reporting.startCrashReporting(this);

		if (!Patchr.applicationUpdateRequired) {
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

		mSwipeRefreshLayout.setProgressViewOffset(true, UI.getRawPixelsForDisplayPixels(48f), UI.getRawPixelsForDisplayPixels(48f));
		mSwipeRefreshLayout.setRefreshing(true);

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncPrepareToRun");
				ModelResult result = new ModelResult();

				if (Patchr.firstStartApp) {
					configure();
					int maxAttempts = 5;
					int attempts = 1;
					while (attempts <= maxAttempts) {
						result.serviceResponse = NotificationManager.getInstance().registerInstallWithGCM();
						if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
							Logger.i(SplashForm.this, "Install registered with Gcm");
							break;
						}
						else {
							Logger.w(SplashForm.this, "Install failed to register with Gcm, attempt = " + attempts);
							try {
								Thread.sleep(2000);
							}
							catch (InterruptedException exception) {
								return result;
							}
						}
						attempts++;
					}
					Patchr.firstStartApp = false;
				}

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					User user = Patchr.getInstance().getCurrentUser();
					if (!user.isAnonymous()) {
						Links options = Patchr.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_USER_CURRENT);
						result = Patchr.getInstance().getEntityManager().getEntity(Patchr.getInstance().getCurrentUser().id, true, options);
					}
				}

				/*
				 * We register installs even if the user is anonymous.
				 */
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					result = NotificationManager.getInstance().registerInstallWithAircandi();
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(SplashForm.this, "Splash initialized");
					if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
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
						if (Patchr.applicationUpdateRequired) {
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
		Patchr.getInstance()
		      .setMenuManager(new MenuManager())
		      .setMediaManager(new MediaManager().initSoundPool())
		      .setAnimationManager(new AnimationManager());


		/* Stash last known location but doesn't start location updates */
		//LocationManager.getInstance().initialize(getApplicationContext());

		/* Starts activity recognition */
		ActivityRecognitionManager.getInstance().initialize(getApplicationContext());

		Logger.i(this, "First run configuration completed");
	}

	protected void startHomeActivity() {
		if (!Patchr.getInstance().getCurrentUser().isAnonymous() && Patchr.firstStartIntent != null) {
			startActivity(Patchr.firstStartIntent);
		}
		else {
			Patchr.dispatch.route(this, Route.HOME, null, null, null);
		}

		/* Always ok to make sure firstStartIntent isn't still around */
		Patchr.firstStartIntent = null;

		/*
		 * This is a hack to delay the finish. When executed immediately, we
		 * are getting a warning about lost windows because the activity hadn't completely
		 * started before it was being killed.
		 */
		Patchr.mainThreadHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				finish();
			}
		}, 1000);
	}

	private void showButtons(Buttons buttons) {
		mSwipeRefreshLayout.setRefreshing(false);
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
		mSwipeRefreshLayout.setRefreshing(false);
		Dialogs.updateApp(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Dialogs
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onSigninButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Patchr.dispatch.route(this, Route.SIGNIN, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Patchr.dispatch.route(this, Route.REGISTER, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onStartButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		startHomeActivity();
	}

	@SuppressWarnings("ucd")
	public void onRetryButtonClick(View view) {
		showButtons(Buttons.NONE);
		if (Patchr.applicationUpdateRequired) {
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
		Patchr.getInstance().setCurrentActivity(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(this, "Splash pause");
		clearReferences();
		mSwipeRefreshLayout.setRefreshing(false);
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
		Activity currentActivity = Patchr.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.equals(this)) {
			Patchr.getInstance().setCurrentActivity(null);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private enum Buttons {
		ACCOUNT,
		RETRY,
		NONE
	}
}