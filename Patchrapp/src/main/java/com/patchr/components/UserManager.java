package com.patchr.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.accountkit.AccountKit;
import com.google.gson.Gson;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnBackPressListener;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.patchr.BuildConfig;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.LinkSpecs;
import com.patchr.objects.PhoneNumber;
import com.patchr.objects.Session;
import com.patchr.objects.User;
import com.patchr.service.ProxibaseResponse;
import com.patchr.service.RestClient;
import com.patchr.ui.LobbyScreen;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import rx.Observable;

public class UserManager {

	public static User        currentUser;
	public static RealmEntity currentRealmUser;
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

	public void setCurrentRealmUser(RealmEntity user, Session session, Boolean refreshUser) {
		if (user == null) {
			discardCredentials();
		}
		else {
			captureRealmCredentials(user, session);
			captureRealmAuthHints(user, LobbyScreen.AuthType.Password);
		}
	}

	public Boolean setCurrentUser(User user, Session session, Boolean refreshUser) {

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
				LinkSpecs options = LinkSpecFactory.build(LinkSpecType.LINKS_FOR_USER_CURRENT);
				result = DataController.getInstance().getEntity(user.id, true, options, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}
			captureCredentials(user, session);
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
			Gson gson = new Gson();
			final RealmEntity user = gson.fromJson(jsonUser, RealmEntity.class);
			final Session session = gson.fromJson(jsonSession, Session.class);
			setCurrentRealmUser(user, session, false);  // Does not block because of 'false', also updates persisted user
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
			setCurrentUser(null, null, false);
			Reporting.track(AnalyticsCategory.ACTION, "Logged Out");
			return;
		}

		RestClient.getInstance().logout(userId, sessionKey).subscribe(
			response -> {
				Logger.i(this, "Logout from service successful");
			},
			throwable -> {
				Logger.w(this, "Logout from service failed");
			});

		Logger.i(this, "User logged out: " + UserManager.currentUser.id);
		setCurrentUser(null, null, false);
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

	private void captureCredentials(User user, Session session) {

		/* Update settings */
		currentSession = session;
		currentUser = user;
		sessionKey = session.key;
		userId = user.id;
		userName = user.name;
		userRole = user.role;

		String jsonUser = Json.objectToJson(user);
		String jsonSession = Json.objectToJson(session);

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

	private void captureRealmCredentials(RealmEntity user, Session session) {

		/* Update settings */
		currentSession = session;
		currentRealmUser = user;
		sessionKey = session.key;
		userId = user.id;
		userName = user.name;
		userRole = user.role;

		Gson gson = new Gson();

		String jsonUser = gson.toJson(user);
		String jsonSession = gson.toJson(session);

		//Reporting.updateUser(user);  // Handles all frameworks

		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user), jsonUser);
		editor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);

		editor.apply();
	}

	public void captureRealmAuthHints(RealmEntity user, String authType) {

		if (authType != null) {

			SharedPreferences.Editor editor = Patchr.settings.edit();
			String jsonUser = Json.objectToJson(user);

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
					authIdentifierHint = user.phone;
					editor.putString(StringManager.getString(R.string.setting_last_auth_identifier), ((PhoneNumber) authIdentifierHint).toJson());
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