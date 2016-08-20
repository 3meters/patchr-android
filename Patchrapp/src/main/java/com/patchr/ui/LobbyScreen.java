package com.patchr.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.facebook.applinks.AppLinkData;
import com.google.android.gms.maps.MapView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.AnimationManager;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.ReportingManager;
import com.patchr.components.UserManager;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.Preference;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;

import java.util.Map;

import bolts.AppLinks;
import io.branch.referral.Branch;

@SuppressLint("Registered")
public class LobbyScreen extends AppCompatActivity {

	protected Boolean restart = false;
	protected BusyController busyPresenter;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isTaskRoot()
			&& getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
			&& getIntent().getAction() != null
			&& getIntent().getAction().equals(Intent.ACTION_MAIN)) {

			finish();
			return;
		}
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.screen_lobby);
		ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "LobbyScreen");
		initialize();
	}

	@Override protected void onRestart() {
		super.onRestart();
		this.restart = true;
	}

	@Override protected void onStart() {
		super.onStart();
		if (!this.restart) {
			handleBranch();
		}
	}

	@Override public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			Logger.d(this, "Lobby visible");
		}
	}

	@Override protected void onStop() {
		super.onStop();
		/* Hack to prevent false positives for deferred deep links */
		if (Patchr.settings != null && Patchr.settings.getBoolean(Preference.FIRST_RUN, true)) {
			SharedPreferences.Editor editor = Patchr.settings.edit();
			editor.putBoolean(Preference.FIRST_RUN, false);
			editor.apply();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onNewIntent(Intent intent) {
		/*
		 * Because this activity is singleTask, it can be relaunched with a new intent. getIntent
		 * returns the original launch intent so we update it so all intent based processing
		 * sees the current one instead of the original. Facebook interactions can trigger new
		 * intents.
		 */
		this.setIntent(intent);
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_LOGIN) {
			if (resultCode == Constants.RESULT_USER_LOGGED_IN && UserManager.shared().authenticated()) {
				startHomeActivity();
			}
		}
		else if (requestCode == AndroidManager.PLAY_SERVICES_RESOLUTION_REQUEST) {
			proceed();
		}
	}

	public void onClick(View view) {

		if (Patchr.updateRequired) {
			updateRequired();
			return;
		}

		if (view.getId() == R.id.login_button) {
			Intent intent = new Intent(this, LoginEdit.class);
			intent.putExtra(Constants.EXTRA_STATE, State.Login);
			startActivityForResult(intent, Constants.ACTIVITY_LOGIN);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
		else if (view.getId() == R.id.signup_button) {
			Intent intent = new Intent(this, LoginEdit.class);
			intent.putExtra(Constants.EXTRA_STATE, State.Signup);
			startActivityForResult(intent, Constants.ACTIVITY_LOGIN);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		this.busyPresenter = new BusyController();

		/* Hack to preload map for later use */
		new Thread(() -> {
			try {
				MapView mapView = new MapView(Patchr.applicationContext);
				mapView.onCreate(null);
				mapView.onPause();
				mapView.onDestroy();
			}
			catch (Exception ignored){}
		}).start();
	}

	protected void handleBranch() {
		/*
		 * Check for a deep link.
		 */
		final Uri uri = this.getIntent().getData();

		Branch.getInstance(Patchr.applicationContext).initSession((branchUniversalObject, linkProperties, error) -> {

			if (branchUniversalObject != null) {

				Map metadata = branchUniversalObject.getMetadata();

				if (linkProperties.getFeature().equals("reset_password")) {
					Intent intent = new Intent(this, ResetEdit.class);
					intent.putExtra(Constants.EXTRA_RESET_TOKEN, (String) metadata.get("token"));
					intent.putExtra(Constants.EXTRA_RESET_USER_NAME, (String) metadata.get("userName"));
					intent.putExtra(Constants.EXTRA_RESET_USER_PHOTO, (String) metadata.get("userPhoto"));
					startActivity(intent);
					finish();
					return;
				}

				Intent intent = UI.browseEntity((String) metadata.get("entityId"), LobbyScreen.this, true);
				intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, (String) metadata.get("entitySchema"));
				intent.putExtra(Constants.EXTRA_ENTITY_ID, (String) metadata.get("entityId"));
				intent.putExtra(Constants.EXTRA_REFERRER_NAME, (String) metadata.get("referrerName"));
				intent.putExtra(Constants.EXTRA_REFERRER_PHOTO_URL, (String) metadata.get("referrerPhotoUrl"));
				intent.putExtra(Constants.EXTRA_SHOW_REFERRER_WELCOME, true);
				startActivity(intent);
				AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
				finish();
				return;
			}

			if (error != null) {
				Logger.w(this, error.getMessage());
			}

			handleFacebook();   // Chaining
		}, uri, this);
	}

	protected void handleFacebook() {
		/*
		 * Check for facebook deep link
		 */
		Uri targetUrl = AppLinks.getTargetUrl(getIntent());

		if (targetUrl != null && targetUrl.getHost().equals("fb.me")) {
			Logger.i(this, "Facebook applink target url: " + targetUrl.toString());
			UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(targetUrl.toString());

			routeDeepLink(sanitizer.getValue("entityId")
				, sanitizer.getValue("entitySchema")
				, sanitizer.getValue("referrerName").replaceAll("_", " ")
				, sanitizer.getValue("referrerPhotoUrl"));
			finish();
		}
		else {
			if (!Patchr.settings.getBoolean(Preference.FIRST_RUN, true)) {
				proceed();
			}
			else {
				AppLinkData.fetchDeferredAppLinkData(this, (appLinkData) -> {
					if (appLinkData != null) {
						String targetUrlString = appLinkData.getArgumentBundle().getString("target_url");
						if (targetUrlString != null) {
							Logger.i(this, "Facebook deferred applink target url: " + targetUrlString);
							UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(targetUrlString);

							routeDeepLink(sanitizer.getValue("entityId")
								, sanitizer.getValue("entitySchema")
								, sanitizer.getValue("referrerName").replaceAll("_", " ")
								, sanitizer.getValue("referrerPhotoUrl"));
							finish();
							return;
						}
					}
					proceed();
				});
			}
		}
	}

	protected void proceed() {

		runOnUiThread(() -> {
			/* Always clear the location */
			LocationManager.getInstance().stop();
			LocationManager.getInstance().setAndroidLocationLocked(null);

			if (Patchr.updateRequired) {
				updateRequired();
				return;
			}
			/*
			 * Check to make sure play services are working properly. This call will finish
			 * the activity if play services are missing and can't be installed or if the user
			 * refuses to install them. If play services can be fixed, then resume will be
			 * called again.
			 */
			if (AndroidManager.checkPlayServices(LobbyScreen.this)) {
				if (UserManager.shared().authenticated()) {
					startHomeActivity();
				}
				else {
					showButtons();
				}
			}
		});
	}

	protected void routeDeepLink(String entityId, String entitySchema, String referrerName, String referrerPhotoUrl) {

		Intent intent = UI.browseEntity(entityId, LobbyScreen.this, true);
		intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, entitySchema);
		intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
		intent.putExtra(Constants.EXTRA_REFERRER_NAME, referrerName);
		intent.putExtra(Constants.EXTRA_REFERRER_PHOTO_URL, referrerPhotoUrl);
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	protected void startHomeActivity() {
		/*
		 * Check if someone wants to share something with Patchr users from another app
		 * and they were not already signed into Patchr. We need them to sign in and
		 * then we get them back to the activity to handle the share.
		 */
		if (UserManager.shared().authenticated() && Patchr.sendIntent != null) {
			startActivity(Patchr.sendIntent);
			Patchr.sendIntent = null;
		}
		else {
			UI.routeHome(this);
		}
		finish();
	}

	private void showButtons() {

		View logoStartup = findViewById(R.id.logo_startup);
		ObjectAnimator.ofFloat(logoStartup, "alpha", 1f, 0f).setDuration(300).start();

		View dialog = findViewById(R.id.dialog);
		ObjectAnimator.ofFloat(dialog, "alpha", 0f, 1f).setDuration(300).start();
	}

	private void updateRequired() {
		Dialogs.updateApp(this);
	}
}