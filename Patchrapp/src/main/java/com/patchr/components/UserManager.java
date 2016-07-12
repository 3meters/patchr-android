package com.patchr.components;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.patchr.model.PhoneNumber;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Session;
import com.patchr.objects.User;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.Command;
import com.patchr.objects.enums.State;
import com.patchr.service.ProxibaseResponse;
import com.patchr.service.RestClient;
import com.patchr.ui.LobbyScreen;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import rx.Observable;

public class UserManager {

	public static RealmEntity currentUser;
	public static Session     currentSession;

	public static String sessionKey;        // promoted for convenience
	public static String userId;            // promoted for convenience
	public static String userName;          // promoted for convenience
	public static String userRole;          // promoted for convenience

	public static String authTypeHint;          // convenience
	public static Object authIdentifierHint;    // convenience
	public static Object authUserHint;          // convenience

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
					authIdentifierHint = Patchr.gson.fromJson(jsonPhone, PhoneNumber.class);
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

	public void setCurrentUser(RealmEntity user, Session session) {
		if (user == null) {
			discardCredentials();
		}
		else {
			captureCredentials(user, session);
			captureAuthHints(user, LobbyScreen.AuthType.Password);
		}
	}

	public void loginAuto() {
		/*
		 * Gets called on app create.
		 */
		String jsonUser = Patchr.settings.getString(StringManager.getString(R.string.setting_user), null);
		String jsonSession = Patchr.settings.getString(StringManager.getString(R.string.setting_user_session), null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto log in using cached user...");
			final RealmEntity user = Patchr.gson.fromJson(jsonUser, RealmEntity.class);
			final Session session = Patchr.gson.fromJson(jsonSession, Session.class);
			setCurrentUser(user, session);  // Does not block because of 'false', also updates persisted user
		}
	}

	public Boolean authenticated() {
		return (userId != null && currentSession != null);
	}

	public Boolean provisional() {
		return (userId != null && currentSession != null && userRole != null && userRole.equals("provisional"));
	}

	public Observable<ProxibaseResponse> login(String email, String password) {
		return RestClient.getInstance().login(email, password);
	}

	public void logout() {

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			AccountKit.logOut();
			setCurrentUser(null, null);
			Reporting.track(AnalyticsCategory.ACTION, "Logged Out");
			return;
		}

		RestClient.getInstance().logout(userId, sessionKey).subscribe(
			response -> {
				Logger.i(this, "Logout from service successful");
			},
			error -> {
				Logger.w(this, error.getLocalizedMessage());
			});

		Logger.i(this, "User logged out: " + UserManager.currentUser.id);
		setCurrentUser(null, null);
		Reporting.track(AnalyticsCategory.ACTION, "Logged out");
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
						extras.putString(Constants.EXTRA_ONBOARD_MODE, State.Login);
						Patchr.router.route(context, Command.LOGIN, null, extras);
					}
					else if (view.getId() == R.id.signup_button) {
						Bundle extras = new Bundle();
						extras.putString(Constants.EXTRA_ONBOARD_MODE, State.Signup);
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

	public void handlePasswordChange(ProxibaseResponse response) {

		Session session = response.session;
		currentSession = session;
		sessionKey = session.key;

		String jsonSession = Patchr.gson.toJson(session);
		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);
		editor.apply();
	}

	private void captureCredentials(RealmEntity user, Session session) {

		/* Update settings */
		currentSession = session;
		currentUser = user;
		sessionKey = session.key;
		userId = user.id;
		userName = user.name;
		userRole = user.role;

		String jsonUser = Patchr.gson.toJson(user);
		String jsonSession = Patchr.gson.toJson(session);

		Reporting.updateUser(user);  // Handles all frameworks

		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user), jsonUser);
		editor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);

		editor.apply();
	}

	public void captureAuthHints(RealmEntity user, String authType) {

		if (authType != null) {

			SharedPreferences.Editor editor = Patchr.settings.edit();
			String jsonUser = Patchr.gson.toJson(user);

			authTypeHint = authType;
			authUserHint = user;

			editor.putString(StringManager.getString(R.string.setting_last_auth_type), authTypeHint);
			editor.putString(StringManager.getString(R.string.setting_last_auth_user), jsonUser);

			if (user.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				if (authType.equals(LobbyScreen.AuthType.Email)
					|| authType.equals(LobbyScreen.AuthType.Password)) {
					authIdentifierHint = user.email;
					editor.putString(StringManager.getString(R.string.setting_last_auth_identifier), (String) authIdentifierHint);
				}
				else if (authType.equals(LobbyScreen.AuthType.PhoneNumber)) {
					authIdentifierHint = user.getPhone();
					editor.putString(StringManager.getString(R.string.setting_last_auth_identifier), user.phoneJson);
				}
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
		currentSession = null;
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