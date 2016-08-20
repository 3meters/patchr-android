package com.patchr.components;

import android.content.SharedPreferences;

import com.onesignal.OneSignal;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Session;
import com.patchr.service.ProxibaseResponse;
import com.patchr.service.RestClient;

import rx.Observable;

public class UserManager {

	public static RealmEntity currentUser;
	public static Session     currentSession;

	public static String sessionKey;        // promoted for convenience
	public static String userId;            // promoted for convenience
	public static String userName;          // promoted for convenience
	public static String userRole;          // promoted for convenience

	static class UserManagerHolder {
		public static final UserManager instance = new UserManager();
	}

	public static UserManager shared() {
		return UserManagerHolder.instance;
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

	public Observable<ProxibaseResponse> login(String email, String password) {
		return RestClient.getInstance().login(email, password);
	}

	public void logout() {

		RestClient.getInstance().logout(userId, sessionKey).subscribe(
			response -> {
				Logger.i(this, "Logout from service successful");
				ReportingManager.getInstance().userLoggedOut();
			},
			error -> {
				Logger.w(this, error.getLocalizedMessage());
			});

		Logger.d(this, "User logging out: " + UserManager.currentUser.id);
		setCurrentUser(null, null);
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

		ReportingManager.getInstance().updateUser(user);  // Handles all frameworks

		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user), jsonUser);
		editor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);
		editor.putString(StringManager.getString(R.string.setting_last_email), user.email);
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
		OneSignal.clearOneSignalNotifications();

		ReportingManager.getInstance().updateUser(null); // Handles all frameworks

		/* Clear user settings */
		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_user), null);
		editor.putString(StringManager.getString(R.string.setting_user_session), null);
		editor.apply();
	}
}