package com.patchr.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
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
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NotificationManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.RegisterInstallEvent;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.PhoneNumber;
import com.patchr.objects.Preference;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.edit.ProfileEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
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
	protected BottomSheetLayout bottomSheetLayout;
	protected Boolean restart    = false;
	protected String  authType   = AuthType.Password;
	protected String  authIntent = BaseScreen.State.Login;
	protected BusyPresenter busyPresenter;

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
				authIntent = BaseScreen.State.Login;
				verifyEmail(null);
			}
			else if (view.getId() == R.id.submit_button) {
				authIntent = BaseScreen.State.Signup;
				verifyEmail(null);
			}
			else if (view.getId() == R.id.user_button) {
				authIntent = BaseScreen.State.Login;
				verifyEmail((String) UserManager.authIdentifierHint);
			}
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

		this.busyPresenter = new BusyPresenter();
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

		//if (!registered || !registeredClientVersionCode.equals(clientVersionCode)) {
			Dispatcher.getInstance().post(new RegisterInstallEvent());  // Sets install registered flag only if successful
		//}
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

	public void completeProfile(Entity entity) {

		final String jsonEntity = Json.objectToJson(entity);
		Intent intent = new Intent(this, ProfileEdit.class);
		intent.putExtra(Constants.EXTRA_STATE, BaseScreen.State.CompleteProfile);
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
							intent.putExtra(Constants.EXTRA_ONBOARD_MODE, UserManager.authTypeHint != null ? LoginEdit.OnboardMode.Login : LoginEdit.OnboardMode.Signup);
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

		final int color = Colors.getColor(R.color.brand_primary);

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

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_logging_in, LobbyScreen.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLogin");
				ModelResult result = DataController.getInstance().tokenLogin(authorizationCode
						, authType
						, LoginEdit.class.getSimpleName()
						, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					if (authIntent.equals(BaseScreen.State.Login)) {
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

										@Override public void onClick(DialogInterface dialog, int which) {
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
					else if (authIntent.equals(BaseScreen.State.Signup)) {
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
					Errors.handleError(LobbyScreen.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void showButtons() {

		ImageWidget userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		TextView userName = (TextView) findViewById(R.id.user_name);
		TextView userAuthIdentifier = (TextView) findViewById(R.id.user_auth_identifier);
		View userGroup = findViewById(R.id.user_group);
		Button authButton = (Button) findViewById(R.id.auth_button);
		Button loginButton = (Button) findViewById(R.id.login_button);
		Button submitButton = (Button) findViewById(R.id.submit_button);
		Button guestButton = (Button) findViewById(R.id.guest_button);

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			UI.setVisibility(authButton, View.GONE);
			UI.setVisibility(guestButton, View.GONE);
			if (UserManager.authUserHint != null && UserManager.authUserHint.name != null) {
				UI.setImageWithEntity(userPhoto, UserManager.authUserHint);
				UI.setTextView(userName, String.format("Log in as %1$s", UserManager.authUserHint.name));
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
		ObjectAnimator anim = ObjectAnimator.ofFloat(dialog, "alpha", 0f, 1f);
		anim.setDuration(300);
		anim.start();
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