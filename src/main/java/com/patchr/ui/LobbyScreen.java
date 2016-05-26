package com.patchr.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration.AccountKitConfigurationBuilder;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.applinks.AppLinkData;
import com.patchr.BuildConfig;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.NotificationManager;
import com.patchr.components.UserManager;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.Preference;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import java.util.Map;

import bolts.AppLinks;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class LobbyScreen extends AppCompatActivity {
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
		if (!isTaskRoot()
				&& getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
				&& getIntent().getAction() != null
				&& getIntent().getAction().equals(Intent.ACTION_MAIN)) {

			finish();
			return;
		}
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.screen_lobby);
	}

	@Override protected void onStart() {
		super.onStart();
		handleBranch();
	}

	@Override protected void onStop() {
		super.onStop();
		/* Hack to prevent false positives for deferred deep links */
		if (Patchr.settings.getBoolean(Preference.FIRST_RUN, true)) {
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
		if (requestCode == Constants.ACTIVITY_LOGIN_ACCOUNT_KIT) {
			if (intent != null) {
				AccountKitLoginResult loginResult = intent.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
				String toastMessage;
				if (loginResult.getError() != null) {
					toastMessage = loginResult.getError().getErrorType().getMessage();
				}
				else if (loginResult.wasCancelled()) {
					toastMessage = "Login Cancelled";
				}
				else {
					if (loginResult.getAccessToken() != null) {
						toastMessage = "Success:" + loginResult.getAccessToken().getAccountId();
					}
					else {
						toastMessage = String.format(
								"Success:%s...",
								loginResult.getAuthorizationCode().substring(0, 10));
					}

					UI.toast(toastMessage);
				}
				startHomeActivity();
			}
			else {
				proceed();
			}
		}
		else if (requestCode == Constants.ACTIVITY_LOGIN) {
			if (resultCode == Constants.RESULT_USER_LOGGED_IN && UserManager.shared().authenticated()) {
				startHomeActivity();
			}
		}
		else if (requestCode == AndroidManager.PLAY_SERVICES_RESOLUTION_REQUEST) {
			proceed();
		}
	}

	public void onClick(View view) {

		if (Patchr.applicationUpdateRequired) {
			updateRequired();
			return;
		}

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {

		}
		else {
			if (view.getId() == R.id.login_button) {
				Bundle extras = new Bundle();
				extras.putString(Constants.EXTRA_ONBOARD_MODE, LoginEdit.OnboardMode.Login);
				Patchr.router.route(this, Command.LOGIN, null, extras);
			}
			else if (view.getId() == R.id.submit_button) {
				Bundle extras = new Bundle();
				extras.putString(Constants.EXTRA_ONBOARD_MODE, LoginEdit.OnboardMode.Signup);
				Patchr.router.route(this, Command.LOGIN, null, extras);
			}
			else if (view.getId() == R.id.guest_button) {
				Reporting.track(AnalyticsCategory.ACTION, "Entered as Guest");
				startHomeActivity();
			}
		}

	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {
		/* Nothing to do! */
	}

	protected void handleBranch() {
		/*
		 * Check for a deep link.
		 */
		final Uri uri = this.getIntent().getData();

		Branch.getInstance(Patchr.applicationContext).initSession(new Branch.BranchUniversalReferralInitListener() {

			@Override public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {

				if (branchUniversalObject != null) {

					Map metadata = branchUniversalObject.getMetadata();

					if (linkProperties.getFeature().equals("reset_password")) {
						Bundle extras = new Bundle();
						extras.putString(Constants.EXTRA_RESET_TOKEN, (String) metadata.get("token"));
						extras.putString(Constants.EXTRA_RESET_USER_NAME, (String) metadata.get("userName"));
						extras.putString(Constants.EXTRA_RESET_USER_PHOTO, (String) metadata.get("userPhoto"));
						startActivity(new Intent(LobbyScreen.this, ResetEdit.class).putExtras(extras));
						finish();
						return;
					}

					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, (String) metadata.get("entitySchema"));
					extras.putString(Constants.EXTRA_ENTITY_ID, (String) metadata.get("entityId"));
					extras.putString(Constants.EXTRA_REFERRER_NAME, (String) metadata.get("referrerName"));
					extras.putString(Constants.EXTRA_REFERRER_PHOTO_URL, (String) metadata.get("referrerPhotoUrl"));
					extras.putBoolean(Constants.EXTRA_SHOW_REFERRER_WELCOME, true);

					Patchr.router.browse(LobbyScreen.this, (String) metadata.get("entityId"), extras, true);

					finish();
					return;
				}

				if (error != null) {
					Logger.w(this, error.getMessage());
				}

				handleFacebook();   // Chaining
			}
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
				AppLinkData.fetchDeferredAppLinkData(this,
						new AppLinkData.CompletionHandler() {

							@Override public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
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
							}
						});
			}
		}
	}

	protected void proceed() {

		runOnUiThread(new Runnable() {

			@Override public void run() {

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
				if (AndroidManager.checkPlayServices(LobbyScreen.this)) {
					if (UserManager.shared().authenticated()) {
						startHomeActivity();
					}
					else {
						showButtons();
					}
				}
			}
		});
	}

	protected void routeDeepLink(String entityId, String entitySchema, String referrerName, String referrerPhotoUrl) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, entitySchema);
		extras.putString(Constants.EXTRA_ENTITY_ID, entityId);
		extras.putString(Constants.EXTRA_REFERRER_NAME, referrerName);
		extras.putString(Constants.EXTRA_REFERRER_PHOTO_URL, referrerPhotoUrl);
		Patchr.router.browse(this, entityId, extras, true);
	}

	protected void startHomeActivity() {
		/*
		 * Check if someone wants to share something with Patchr users from another app
		 * and they were not already signed into Patchr. We need them to sign in and
		 * then we get them back to the activity to handle the share.
		 */
		if (UserManager.shared().authenticated() && Patchr.sendIntent != null) {
			this.startActivity(Patchr.sendIntent);
		}
		else {
			Patchr.router.route(this, Command.HOME, null, null);
		}

		finish();

		/* Always ok to make sure sendIntent is cleared */
		Patchr.sendIntent = null;
	}

	private void phoneLogin() {

		final Intent intent = new Intent(this, AccountKitActivity.class);

		AccountKitConfigurationBuilder configurationBuilder = new AccountKitConfigurationBuilder(LoginType.PHONE, AccountKitActivity.ResponseType.CODE);
		configurationBuilder.setFacebookNotificationsEnabled(true);

		intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, configurationBuilder.build());
		startActivityForResult(intent, Constants.ACTIVITY_LOGIN_ACCOUNT_KIT);
	}

	private void showButtons() {
		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			UI.setVisibility(findViewById(R.id.login_email_button), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.login_phone_button), View.VISIBLE);
		}
		else {
			UI.setVisibility(findViewById(R.id.login_button), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.submit_button), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.guest_button), View.VISIBLE);
		}
	}

	private void updateRequired() {
		Dialogs.updateApp(this);
	}
}