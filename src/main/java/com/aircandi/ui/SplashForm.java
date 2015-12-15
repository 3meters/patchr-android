package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
	/*
	 * Splash acts as a sticky guard when:
	 *
	 * - Sharing requires a signed in user
	 * - Play services are not available
	 * - The app must be updated to be used
	 *
	 * Splash always displays when the app user is anonymous, otherwise
	 * it gets finished before it usually has a chance to display.
	 *
	 * Splash runs whenever the app is launched and opportunistically
	 * does some clean-up on the memory data store, forces a location reset, and
	 * clears the new notification count. Basically a soft restart.
	 *
	 * Splash launches can be because of:
	 *
	 * - Start of app by user
	 * - Relaunch after backing out of app
	 * - Routing back to splash because user signs out.
	 * - Routing back to splash because SEND intent (share) needs user sign-in.
	 * - Routing back to splash because app update required.
	 * - Routing back to splash because play services required.
	 *
	 * Running splash is not required to start activities.
	 */
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

		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void startHomeActivity() {
		if (!Patchr.getInstance().getCurrentUser().isAnonymous() && Patchr.sendIntent != null) {
			/*
			 * Someone wants to share something with Patchr users and they were not
			 * already signed into Patchr. We need them to sign in and then we get them
			 * back to the activity to handle the share.
			 */
			this.startActivity(Patchr.sendIntent);
		}
		else {
			Patchr.router.route(this, Route.HOME, null, null);
		}

		finish();

		/* Always ok to make sure sendIntent is cleared */
		Patchr.sendIntent = null;
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
	public void onGuestButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		startHomeActivity();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN
					&& !Patchr.getInstance().getCurrentUser().isAnonymous()) {
				/*
				 * Log in handled
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