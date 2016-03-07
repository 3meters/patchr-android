package com.patchr.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnBackPressListener;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Route;
import com.patchr.objects.Session;
import com.patchr.objects.User;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

public class UserManager {

	protected User   currentUser;
	protected String userName;
	protected String userId;
	protected String sessionKey;
	protected String jsonUser;
	protected String jsonSession;

	static class UserManagerHolder {
		public static final UserManager instance = new UserManager();
	}

	public static UserManager getInstance() {
		return UserManagerHolder.instance;
	}

	private UserManager() {
		this.jsonUser = Patchr.settings.getString(StringManager.getString(R.string.setting_user), null);
		this.jsonSession = Patchr.settings.getString(StringManager.getString(R.string.setting_user_session), null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void signinAuto() {
		/*
		 * Gets called on app create.
		 */
		if (this.jsonUser != null && this.jsonSession != null) {
			Logger.i(this, "Auto log in using cached user...");

			final User user = (User) Json.jsonToObject(this.jsonUser, Json.ObjectType.ENTITY);
			user.session = (Session) Json.jsonToObject(this.jsonSession, Json.ObjectType.SESSION);

			setCurrentUser(user, false);  // Does not block because of 'false', also updates persisted user
		}
	}

	public Boolean authenticated() {
		return (this.userId != null && this.sessionKey != null);
	}

	public void signout() {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSignOut");
				return DataController.getInstance().signoutComplete(NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@SuppressLint("NewApi")
			@Override
			protected void onPostExecute(Object response) {
				/* Set to anonymous user even if service call fails */
				Patchr.router.route(Patchr.applicationContext, Route.SPLASH, null, null);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public void showGuestGuard(final Context context, Integer resId) {
		String message = StringManager.getString((resId == null) ? R.string.alert_signin_message : resId);
		showGuestGuard(context, message);
	}

	public void showGuestGuard(final Context context, String message) {

		View view = LayoutInflater.from(Patchr.applicationContext).inflate(R.layout.guest_guard_view, null, false);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams((int) UI.getScreenWidthRawPixels(context), (int) UI.getScreenHeightRawPixels(context));
		view.setLayoutParams(params);

		((TextView) view.findViewById(R.id.message)).setText(message);

		DialogPlus dialog = DialogPlus.newDialog(context)
				.setOnClickListener(new OnClickListener() {
					@Override public void onClick(DialogPlus dialog, View view) {
						if (view.getId() == R.id.button_login) {
							Patchr.router.route(context, Route.LOGIN, null, null);
						}
						else if (view.getId() == R.id.button_signup) {
							Patchr.router.route(context, Route.SIGNUP, null, null);
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
		this.jsonUser = Json.objectToJson(user);
		this.jsonSession = Json.objectToJson(user.session);
		this.userName = user.name;
		this.userId = user.id;
		this.sessionKey = user.session.key;
		this.currentUser = user;

		BranchProvider.setIdentity(this.userId);
		Reporting.updateCrashUser(this.currentUser);

		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), this.jsonUser);
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), this.jsonSession);
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_last_email), this.currentUser.email);
		Patchr.settingsEditor.commit();
	}

	private void discardCredentials() {

		this.currentUser = null;
		this.userName = null;
		this.userId = null;
		this.sessionKey = null;
		this.jsonSession = null;
		this.jsonUser = null;

		/* Cancel any current notifications in the status bar */
		NotificationManager.getInstance().cancelAllNotifications();

		Reporting.updateCrashUser(null);
		BranchProvider.logout();

		/* Clear user settings */
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), null);
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), null);
		Patchr.settingsEditor.commit();  // Asynch
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public User getCurrentUser() {
		return this.currentUser;
	}

	@NonNull public Boolean setCurrentUser(User user, @NonNull Boolean refreshUser) {

		ModelResult result = new ModelResult();

		if (user == null) {
			discardCredentials();
		}
		else {
			/* Log in does not return a complete user so do a regular data fetch to get a complete user. */
			if (refreshUser) {
				LinkSpec options = LinkSpecFactory.build(LinkSpecType.LINKS_FOR_USER_CURRENT);
				result = DataController.getInstance().getEntity(user.id, true, options, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}
			captureCredentials(user);
		}

		return (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}