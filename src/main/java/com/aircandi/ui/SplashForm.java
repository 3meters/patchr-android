package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.WindowManager;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.DataController;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NotificationManager;
import com.aircandi.objects.Route;
import com.aircandi.utilities.Dialogs;

@SuppressLint("Registered")
public class SplashForm extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(this, "Splash create");
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.splash_form);
		initialize();
	}

	@SuppressLint("ResourceAsColor")
	protected void initialize() {

		/* Always reset the entity cache */
		DataController.getInstance().clearStore();
		LocationManager.getInstance().stop();
		LocationManager.getInstance().setLocationLocked(null);

		/* Restart notification tracking */
		NotificationManager.getInstance().setNewNotificationCount(0);

		if (!Patchr.applicationUpdateRequired) {
			/*
			 * Check to make sure play services are working properly. This call will finish
			 * the activity if play services are missing and can't be installed or if the user
			 * refuses to install them. If play services can be fixed, then resume will be
			 * called again.
			 */
			if (AndroidManager.checkPlayServices(this)) {
				if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
					showButtons();
				}
				else {
					startHomeActivity();
				}
			}
		}
		else {
			updateRequired();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void startHomeActivity() {
		if (!Patchr.getInstance().getCurrentUser().isAnonymous() && Patchr.firstStartIntent != null) {
			/*
			 * Launching to handle an intent. This only happens if the app is being started from
			 * scratch to handle the intent. App could have been killed by Android for lots of
			 * reasons like memory pressure, etc. We init and then refire the intent.
			 *
			 * Could be from a notification or a shared message/patch/photo.
			 *
			 * NOTE: Broadcast receivers like the gcm won't get called if the app
			 * was force closed by the user in Settings->Apps. Will get called if the
			 * app was closed by any other method.
			 */
			TaskStackBuilder
					.create(Patchr.applicationContext)
					.addNextIntent(new Intent(Patchr.applicationContext, AircandiForm.class))
					.addNextIntent(Patchr.firstStartIntent)
					.startActivities();
			finish();
		}
		else {
			Patchr.router.route(this, Route.HOME, null, null);
			finish();
		}

		/* Always ok to make sure firstStartIntent isn't still around */
		Patchr.firstStartIntent = null;
	}

	private void showButtons() {
		findViewById(R.id.button_holder).setVisibility(View.VISIBLE);
	}

	private void updateRequired() {
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
		Patchr.router.route(this, Route.SIGNIN, null, null);
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Patchr.router.route(this, Route.REGISTER, null, null);
	}

	@SuppressWarnings("ucd")
	public void onStartButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		startHomeActivity();
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
}