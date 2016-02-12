package com.patchr.components;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.ActivityRecognitionManager.ActivityState;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.events.ActivityStateEvent;
import com.patchr.events.BeaconsLockedEvent;
import com.patchr.events.EntitiesByProximityCompleteEvent;
import com.patchr.events.EntitiesUpdatedEvent;
import com.patchr.events.QueryWifiScanReceivedEvent;
import com.patchr.interfaces.IBusy;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Beacon;
import com.patchr.objects.Cursor;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Route;
import com.patchr.objects.ServiceData;
import com.patchr.objects.Session;
import com.patchr.objects.User;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.widgets.ListPreferenceMultiSelect;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Boolean authenticated() {
		return (this.userId != null && this.sessionKey != null);
	}

	public void signout() {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSignOut");
				final ModelResult result = DataController.getInstance().signoutComplete(NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@SuppressLint("NewApi")
			@Override
			protected void onPostExecute(Object response) {
				/* Set to anonymous user even if service call fails */
				Patchr.router.route(Patchr.applicationContext, Route.SPLASH, null, null);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void writeCredentials() {
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), this.jsonUser);
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), this.jsonSession);
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_last_email), this.currentUser.email);
		Patchr.settingsEditor.apply();
		BranchProvider.setIdentity(this.currentUser.id);
		Reporting.updateCrashUser(this.currentUser);
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

		/* Clear user settings */
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), null);
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), null);
		Patchr.settingsEditor.apply();  // Asynch

		Reporting.updateCrashUser(null);
		BranchProvider.logout();
	}

	public User getCurrentUser() {
		return this.currentUser;
	}

	@NonNull
	public Boolean setCurrentUser(@NonNull User user, @NonNull Boolean refreshUser) {

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

			/* Update settings */
			this.jsonUser = Json.objectToJson(user);
			this.jsonSession = Json.objectToJson(user.session);
			this.userName = user.name;
			this.userId = user.id;
			this.sessionKey = user.session.key;
			this.currentUser = user;

			writeCredentials();
		}

		return (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}