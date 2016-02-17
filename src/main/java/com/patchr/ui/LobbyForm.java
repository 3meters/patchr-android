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
import com.patchr.objects.TransitionType;
import com.patchr.utilities.Dialogs;

import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchApp;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

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
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.lobby_form);
	}

	@Override protected void onStart() {
		super.onStart();
		initialize();
	}

	@Override protected void onResume() {
		super.onResume();
		Patchr.getInstance().setCurrentActivity(this);
	}

	@Override protected void onPause() {
		super.onPause();
		clearReferences();
	}

	@Override protected void onStop() {
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onNewIntent(Intent intent) {
		this.setIntent(intent);
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN && UserManager.getInstance().authenticated()) {
				startHomeActivity();
			}
		}
		else if (requestCode == AndroidManager.PLAY_SERVICES_RESOLUTION_REQUEST) {
			proceed();
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	public void onLoginButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Patchr.router.route(this, Route.LOGIN, null, null);
	}

	public void onSignupButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Patchr.router.route(this, Route.SIGNUP, null, null);
	}

	public void onGuestButtonClick(View view) {
		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		startHomeActivity();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {
		/*
		 * Check for a deep link.
		 */
		Branch.getInstance(Patchr.applicationContext).initSession(new Branch.BranchUniversalReferralInitListener() {

			@Override
			public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {

				if (branchUniversalObject != null) {
					Map metadata = branchUniversalObject.getMetadata();
					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, (String) metadata.get("entitySchema"));
					extras.putString(Constants.EXTRA_ENTITY_ID, (String) metadata.get("entityId"));
					extras.putString(Constants.EXTRA_INVITER_NAME, (String) metadata.get("referrerName"));
					extras.putString(Constants.EXTRA_INVITER_PHOTO_URL, (String) metadata.get("referrerPhotoUrl"));
					extras.putBoolean(Constants.EXTRA_SHOW_INVITER_WELCOME, true);
					extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
					Patchr.router.route(LobbyForm.this, Route.BROWSE, null, extras);
					finish();
					return;
				}
				else {
					if (error != null) {
						Logger.w(this, error.getMessage());
					}
					proceed();
				}
			}
		}, this.getIntent().getData(), this);
	}

	protected void proceed() {

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
		if (AndroidManager.checkPlayServices(LobbyForm.this)) {
			if (UserManager.getInstance().authenticated()) {
				startHomeActivity();
			}
			else {
				showButtons();
			}
		}
	}

	protected void startHomeActivity() {
		/*
		 * Check if someone wants to share something with Patchr users from another app
		 * and they were not already signed into Patchr. We need them to sign in and
		 * then we get them back to the activity to handle the share.
		 */
		if (UserManager.getInstance().authenticated() && Patchr.sendIntent != null) {
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
		findViewById(R.id.button_signin).setVisibility(View.VISIBLE);
		findViewById(R.id.button_signup).setVisibility(View.VISIBLE);
		findViewById(R.id.button_guest).setVisibility(View.VISIBLE);
	}

	private void updateRequired() {
		Dialogs.updateApp(this);
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