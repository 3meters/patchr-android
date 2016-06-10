package com.patchr.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.accountkit.AccountKit;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnBackPressListener;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.patchr.BuildConfig;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.PhoneNumber;
import com.patchr.objects.Session;
import com.patchr.objects.User;
import com.patchr.ui.LobbyScreen;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

public class UserManager {

	public static User   currentUser;

	public static String sessionKey;        // promoted for convenience
	public static String userId;            // promoted for convenience
	public static String userName;          // promoted for convenience
	public static String userRole;          // promoted for convenience

	public static String authTypeHint;          // convenience
	public static Object authIdentifierHint;    // convenience
	public static User   authUserHint;          // convenience

	static class UserManagerHolder {
		public static final UserManager instance = new UserManager();
	}

	public static UserManager shared() {
		return UserManagerHolder.instance;
	}

	private UserManager() {
		authTypeHint = Patchr.settings.getString(StringManager.getString(R.string.setting_last_auth_type), null);

		if (authTypeHint != null) {
			String jsonAuthUser = Patchr.settings.getString(StringManager.getString(R.string.setting_last_auth_user), null);
			if (jsonAuthUser != null) {
				authUserHint = (User) Json.jsonToObject(jsonAuthUser, Json.ObjectType.ENTITY);
			}

			if (authTypeHint.equals(LobbyScreen.AuthType.Email) || authTypeHint.equals(LobbyScreen.AuthType.Password)) {
				authIdentifierHint = Patchr.settings.getString(StringManager.getString(R.string.setting_last_auth_identifier), null);
			}
			else if (authTypeHint.equals(LobbyScreen.AuthType.PhoneNumber)) {
				String jsonPhone = Patchr.settings.getString(StringManager.getString(R.string.setting_last_auth_identifier), null);
				if (jsonPhone != null) {
					authIdentifierHint = PhoneNumber.fromJson(jsonPhone);
				}
			}
		}
		else {
			authIdentifierHint = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
			if (authIdentifierHint != null) {
				authTypeHint = LobbyScreen.AuthType.Password;
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public Boolean setCurrentUser(User user, Boolean refreshUser) {

		ModelResult result = new ModelResult();

		if (user == null) {
			discardCredentials();
		}
		else {
			/*
			 * Password reset and update do not return a complete user so do a regular
			 * data fetch to get a complete user.
			 */
			if (refreshUser) {
				LinkSpec options = LinkSpecFactory.build(LinkSpecType.LINKS_FOR_USER_CURRENT);
				result = DataController.getInstance().getEntity(user.id, true, options, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}
			captureCredentials(user);
			captureAuthHints(user);
		}

		return (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS);
	}

	public void loginAuto() {
		/*
		 * Gets called on app create.
		 */
		String jsonUser = Patchr.settings.getString(StringManager.getString(R.string.setting_user), null);
		String jsonSession = Patchr.settings.getString(StringManager.getString(R.string.setting_user_session), null);
		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto log in using cached user...");

			final User user = (User) Json.jsonToObject(jsonUser, Json.ObjectType.ENTITY);
			user.session = (Session) Json.jsonToObject(jsonSession, Json.ObjectType.SESSION);

			setCurrentUser(user, false);  // Does not block because of 'false', also updates persisted user
		}
	}

	public Boolean authenticated() {
		return (userId != null && sessionKey != null);
	}

	public Boolean provisional() {
		return (userId != null && sessionKey != null && userRole != null && userRole.equals("provisional"));
	}

	public void logout() {

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			AccountKit.logOut();
			UserManager.shared().setCurrentUser(null, false);
			Reporting.track(AnalyticsCategory.ACTION, "Logged Out");
			return;
		}

		new AsyncTask() {

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSignOut");
				return DataController.getInstance().signoutComplete(NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@SuppressLint("NewApi")
			@Override protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				/* Set to anonymous user even if service call fails */
				if (result.serviceResponse.responseCode != NetworkManager.ResponseCode.SUCCESS) {
					Logger.w(this, "User sign out but service call failed: " + UserManager.currentUser.id);
				}

				Reporting.track(AnalyticsCategory.ACTION, "Logged Out");
				Patchr.router.route(Patchr.applicationContext, Command.LOBBY, null, null);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public void showGuestGuard(final Context context, Integer resId) {
		String message = StringManager.getString((resId == null) ? R.string.alert_signin_message : resId);
		showGuestGuard(context, message);
	}

	public void showGuestGuard(final Context context, String message) {

		View view = LayoutInflater.from(Patchr.applicationContext).inflate(R.layout.dialog_guest_guard, null, false);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams((int) UI.getScreenWidthRawPixels(context), (int) UI.getScreenHeightRawPixels(context));
		view.setLayoutParams(params);

		((TextView) view.findViewById(R.id.message)).setText(message);

		DialogPlus dialog = DialogPlus.newDialog(context)
				.setOnClickListener(new OnClickListener() {
					@Override public void onClick(DialogPlus dialog, View view) {
						if (view.getId() == R.id.button_login) {
							Bundle extras = new Bundle();
							extras.putString(Constants.EXTRA_ONBOARD_MODE, LoginEdit.OnboardMode.Login);
							Patchr.router.route(context, Command.LOGIN, null, extras);
						}
						else if (view.getId() == R.id.submit_button) {
							Bundle extras = new Bundle();
							extras.putString(Constants.EXTRA_ONBOARD_MODE, LoginEdit.OnboardMode.Signup);
							Patchr.router.route(context, Command.LOGIN, null, extras);
						}
						dialog.dismiss();
					}
				})
				.setOnBackPressListener(new OnBackPressListener() {
					@Override public void onBackPressed(DialogPlus dialog) {
						dialog.dismiss();
					}
				})
				.setContentHolder(new ViewHolder(view))
				.setContentWidth((int) UI.getScreenWidthRawPixels(context))
				.setContentHeight((int) UI.getScreenHeightRawPixels(context))
				.setContentBackgroundResource(R.color.transparent)
				.setOverlayBackgroundResource(R.color.scrim_70_pcnt)
				.setCancelable(false)
				.create();

		dialog.show();
	}

	private void captureCredentials(User user) {

		/* Update settings */
		currentUser = user;
		sessionKey = user.session.key;
		userId = user.id;
		userName = user.name;
		userRole = user.role;

		String jsonUser = Json.objectToJson(user);
		String jsonSession = Json.objectToJson(user.session);

		Reporting.updateUser(user);  // Handles all frameworks

		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user), jsonUser);
		editor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);

		editor.apply();
	}

	public void captureAuthHints(User user) {

		if (user.authType != null) {

			SharedPreferences.Editor editor = Patchr.settings.edit();
			String jsonUser = Json.objectToJson(user);

			authTypeHint = user.authType;
			authUserHint = user;

			editor.putString(StringManager.getString(R.string.setting_last_auth_type), authTypeHint);
			editor.putString(StringManager.getString(R.string.setting_last_auth_user), jsonUser);
			if (user.authType.equals(LobbyScreen.AuthType.Email)
					|| user.authType.equals(LobbyScreen.AuthType.Password)) {
				authIdentifierHint = user.email;
				editor.putString(StringManager.getString(R.string.setting_last_auth_identifier), (String) authIdentifierHint);
			}
			else if (user.authType.equals(LobbyScreen.AuthType.PhoneNumber)) {
				authIdentifierHint = user.phone;
				editor.putString(StringManager.getString(R.string.setting_last_auth_identifier), ((PhoneNumber) authIdentifierHint).toJson());
			}
			editor.apply();
		}
	}

	public void discardAuthHints() {
		/*
		 * Only get cleared when user account is deleted.
		 */
		authTypeHint = null;
		authIdentifierHint = null;
		authUserHint = null;

		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_last_auth_type), null);
		editor.putString(StringManager.getString(R.string.setting_last_auth_identifier), null);
		editor.putString(StringManager.getString(R.string.setting_last_auth_user), null);
		editor.apply();
	}

	private void discardCredentials() {

		currentUser = null;
		sessionKey = null;
		userId = null;
		userName = null;
		userRole = null;

		/* Cancel any current notifications in the status bar */
		NotificationManager.getInstance().cancelAllNotifications();

		Reporting.updateUser(null); // Handles all frameworks

		/* Clear user settings */
		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user), null);
		editor.putString(StringManager.getString(R.string.setting_user_session), null);
		editor.apply();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}