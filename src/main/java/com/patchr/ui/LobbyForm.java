package com.patchr.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.NotificationManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Route;
import com.patchr.utilities.Dialogs;

@SuppressLint("Registered")
public class LobbyForm extends AppCompatActivity {
	/*
	 * Lobby acts as a sticky guard when:
	 *
	 * - Sharing requires a signed in user
	 * - Play services are not available
	 * - The app must be updated to be used
	 *
	 * Lobby always displays when the app user is anonymous, otherwise
	 * it gets finished before it usually has a chance to display.
	 *
	 * Lobby runs whenever the app is launched and opportunistically
	 * does some clean-up on the memory data store, forces a location reset, and
	 * clears the new notification count. Basically a soft restart.
	 *
	 * Lobby launches can be because of:
	 *
	 * - Start of app by user
	 * - Relaunch after backing out of app
	 * - Routing back to splash because user signs out.
	 * - Routing back to splash because SEND intent (share) needs user sign-in.
	 * - Routing back to splash because app update required.
	 * - Routing back to splash because play services required.
	 *
	 * Running Lobby is not required to start activities.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(this, "Splash create");
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//		getWindow().setStatusBarColor(Color.TRANSPARENT);
		setContentView(R.layout.lobby_form);
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
			if (!UserManager.getInstance().authenticated()) {
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
		if (UserManager.getInstance().authenticated() && Patchr.sendIntent != null) {
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
		//findViewById(R.id.button_holder).setVisibility(View.VISIBLE);
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
					&& UserManager.getInstance().authenticated()) {
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