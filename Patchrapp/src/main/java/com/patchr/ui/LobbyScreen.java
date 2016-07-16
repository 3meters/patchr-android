package com.patchr.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration.AccountKitConfigurationBuilder;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.applinks.AppLinkData;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.patchr.BuildConfig;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.AnimationManager;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.NotificationManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.PhoneNumber;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.Command;
import com.patchr.objects.enums.Preference;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.edit.ProfileEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import java.util.Map;

import bolts.AppLinks;
import io.branch.referral.Branch;

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
	protected BottomSheetLayout bottomSheetLayout;
	protected Boolean restart    = false;
	protected String  authType   = AuthType.Password;
	protected String  authIntent = State.Login;
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
		if (requestCode == Constants.ACTIVITY_LOGIN_ACCOUNT_KIT) {
			if (intent != null) {
				AccountKitLoginResult loginResult = intent.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
				String toastMessage;
				if (loginResult.getError() != null) {
					toastMessage = loginResult.getError().getErrorType().getMessage();
					UI.toast(toastMessage);
				}
				else if (loginResult.wasCancelled()) {
					toastMessage = "Login Cancelled";
					UI.toast(toastMessage);
				}
				else {
					loginUsingAuthCode(loginResult.getAuthorizationCode());
				}
			}
		}
		else if (requestCode == Constants.ACTIVITY_LOGIN) {
			if (resultCode == Constants.RESULT_USER_LOGGED_IN && UserManager.shared().authenticated()) {
				startHomeActivity();
			}
		}
		else if (requestCode == Constants.ACTIVITY_COMPLETE_PROFILE) {
			if (!UserManager.shared().provisional()) {
				startHomeActivity();
			}
			else {
				UserManager.shared().logout();
				proceed();
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
			if (view.getId() == R.id.login_button) {
				authIntent = State.Login;
				verifyEmail(null);
			}
			else if (view.getId() == R.id.signup_button) {
				authIntent = State.Signup;
				verifyEmail(null);
			}
			else if (view.getId() == R.id.user_button) {
				authIntent = State.Login;
				verifyEmail((String) UserManager.authIdentifierHint);
			}
		}
		else {
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

		this.busyPresenter = new BusyController();
		this.bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);

		if (this.bottomSheetLayout != null)
			this.bottomSheetLayout.setPeekOnDismiss(true);
		/*
		 * Ensure install is registered with service. Only done once unless something like a system update clears
		 * the app preferences.
		 */
		Boolean registered = Patchr.settings.getBoolean(StringManager.getString(R.string.setting_install_registered), false);
		Integer registeredClientVersionCode = Patchr.settings.getInt(StringManager.getString(R.string.setting_install_registered_version_code), 0);
		Integer clientVersionCode = Patchr.getVersionCode(Patchr.applicationContext, MainScreen.class);

		if (!registered || !registeredClientVersionCode.equals(clientVersionCode)) {
			//Dispatcher.getInstance().post(new RegisterInstallEvent());  // Sets install registered flag only if successful
		}
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

						@Override
						public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
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
				LocationManager.getInstance().stop();
				LocationManager.getInstance().setAndroidLocationLocked(null);

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
					if (UserManager.shared().authenticated() && !UserManager.shared().provisional()) {
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

	public void completeProfile(RealmEntity entity) {

		final String jsonEntity = Patchr.gson.toJson(entity);
		Intent intent = new Intent(this, ProfileEdit.class);
		intent.putExtra(Constants.EXTRA_STATE, State.CompleteProfile);
		intent.putExtra(Constants.EXTRA_ENTITY, jsonEntity);
		startActivityForResult(intent, Constants.ACTIVITY_COMPLETE_PROFILE);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	public void login() {

		MenuSheetView menuSheetView = new MenuSheetView(this, MenuSheetView.MenuType.GRID, "Login or create an account using...", new MenuSheetView.OnMenuItemClickListener() {

			@Override public boolean onMenuItemClick(final MenuItem item) {

				bottomSheetLayout.addOnSheetDismissedListener(new OnSheetDismissedListener() {

					@Override public void onDismissed(BottomSheetLayout bottomSheetLayout) {

						if (item.getItemId() == R.id.login_using_phone) {
							verifyPhoneNumber();
						}
						else if (item.getItemId() == R.id.login_using_email) {
							verifyEmail(null);
						}
						else if (item.getItemId() == R.id.login_using_password) {

							authType = AuthType.Password;

							Intent intent = new Intent(LobbyScreen.this, LoginEdit.class);
							intent.putExtra(Constants.EXTRA_ONBOARD_MODE, UserManager.authTypeHint != null ? State.Login : State.Signup);
							startActivityForResult(intent, Constants.ACTIVITY_LOGIN);
							AnimationManager.doOverridePendingTransition(LobbyScreen.this, TransitionType.FORM_TO);
						}
					}
				});

				bottomSheetLayout.dismissSheet();
				return true;
			}
		});

		menuSheetView.inflateMenu(R.menu.menu_login_sheet);

		menuSheetView.getMenu().getItem(0).getIcon().setAlpha((int) (256 * 0.5));
		menuSheetView.getMenu().getItem(1).getIcon().setAlpha((int) (256 * 0.5));
		menuSheetView.getMenu().getItem(2).getIcon().setAlpha((int) (256 * 0.5));

		bottomSheetLayout.setPeekOnDismiss(true);
		bottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	public void verifyPhoneNumber() {

		authType = AuthType.PhoneNumber;
		final Intent intent = new Intent(LobbyScreen.this, AccountKitActivity.class);
		AccountKitConfigurationBuilder configurationBuilder = new AccountKitConfigurationBuilder(LoginType.PHONE, AccountKitActivity.ResponseType.CODE);
		configurationBuilder.setFacebookNotificationsEnabled(true);
		configurationBuilder.setReadPhoneStateEnabled(true);
		configurationBuilder.setReceiveSMS(true);

		if (UserManager.authTypeHint != null && UserManager.authTypeHint.equals(AuthType.PhoneNumber)) {
			if (UserManager.authIdentifierHint != null) {
				PhoneNumber phoneNumber = (PhoneNumber) UserManager.authIdentifierHint;
				configurationBuilder.setInitialPhoneNumber(new com.facebook.accountkit.PhoneNumber(phoneNumber.countryCode, phoneNumber.number));
			}
		}

		intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, configurationBuilder.build());
		startActivityForResult(intent, Constants.ACTIVITY_LOGIN_ACCOUNT_KIT);
	}

	public void verifyEmail(String initialEmail) {

		authType = AuthType.Email;
		final Intent intent = new Intent(LobbyScreen.this, AccountKitActivity.class);
		AccountKitConfigurationBuilder configurationBuilder = new AccountKitConfigurationBuilder(LoginType.EMAIL, AccountKitActivity.ResponseType.CODE);
		configurationBuilder.setFacebookNotificationsEnabled(true);

		if (initialEmail != null) {
			configurationBuilder.setInitialEmail(initialEmail);
		}

		intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, configurationBuilder.build());
		startActivityForResult(intent, Constants.ACTIVITY_LOGIN_ACCOUNT_KIT);
	}

	public void loginUsingAuthCode(final String authorizationCode) {

		busyPresenter.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_logging_in, LobbyScreen.this);

		RestClient.getInstance().tokenLogin(authorizationCode, authType)
			.subscribe(
				response -> {
					busyPresenter.hide(true);
					if (response.isSuccessful()) {
						if (authIntent.equals(State.Login)) {
							if (UserManager.shared().provisional()) {
								/* User meant to login but got a new account instead. */
								Logger.i(this, "User tried to login but got new account instead");
								final AlertDialog dialog = Dialogs.alertDialog(R.drawable.ic_launcher
									, "Log in"
									, String.format("No account exists for %1$s. Enter the same email address you entered when you created your account.", UserManager.currentUser.email)
									, null
									, LobbyScreen.this
									, R.string.dialog_no_account_exists_positive
									, R.string.dialog_no_account_exists_cancel
									, null
									, new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which) {
											if (which == DialogInterface.BUTTON_POSITIVE) {
												dialog.dismiss();
											}
											else if (which == DialogInterface.BUTTON_NEGATIVE) {
												completeProfile(UserManager.currentUser);
												dialog.dismiss();
											}
										}
									}
									, null);

								dialog.setCanceledOnTouchOutside(false);
								dialog.show();
							}
							else {
								Logger.i(this, "User logged in: " + UserManager.currentUser.name);
								UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
								startHomeActivity();
							}
						}
						else if (authIntent.equals(State.Signup)) {
							if (!UserManager.shared().provisional()) {
							/* User meant to signup but got an existing account instead. */
								Logger.i(this, "User tried to sign up but got an existing account instead");
								Logger.i(this, "User logged in: " + UserManager.currentUser.name);
								UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
								startHomeActivity();
								finish();
							}
							else {
								completeProfile(UserManager.currentUser);
							}
						}
					}
					else {
						Logger.w(this, response.error.message);
					}
				},
				error -> {
					busyPresenter.hide(true);
					Logger.w(this, error.getLocalizedMessage());
				});
	}

	private void showButtons() {

		View userGroup = findViewById(R.id.user_group);
		Button authButton = (Button) findViewById(R.id.auth_button);
		Button guestButton = (Button) findViewById(R.id.guest_button);

		View logoStartup = findViewById(R.id.logo_startup);
		ObjectAnimator.ofFloat(logoStartup, "alpha", 1f, 0f).setDuration(300).start();

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			ImageWidget userPhoto = (ImageWidget) findViewById(R.id.user_photo);
			TextView userName = (TextView) findViewById(R.id.user_name);
			TextView userAuthIdentifier = (TextView) findViewById(R.id.user_auth_identifier);
			UI.setVisibility(authButton, View.GONE);
			UI.setVisibility(guestButton, View.GONE);
			if (UserManager.authUserHint != null && ((RealmEntity) UserManager.authUserHint).name != null) {
				UI.setImageWithEntity(userPhoto, (RealmEntity) UserManager.authUserHint);
				UI.setTextView(userName, String.format("Log in as %1$s", ((RealmEntity) UserManager.authUserHint).name));
				UI.setTextView(userAuthIdentifier, (String) UserManager.authIdentifierHint);
			}
			else {
				UI.setVisibility(userGroup, View.GONE);
			}
		}
		else {
			UI.setVisibility(userGroup, View.GONE);
			UI.setVisibility(authButton, View.GONE);
			UI.setVisibility(guestButton, View.VISIBLE);
		}

		View dialog = findViewById(R.id.dialog);
		ObjectAnimator.ofFloat(dialog, "alpha", 0f, 1f).setDuration(300).start();
	}

	private void updateRequired() {
		Dialogs.updateApp(this);
	}

	public static class AuthType {
		public static String Password    = "password";
		public static String PhoneNumber = "phone_number";
		public static String Email       = "email";
	}
}