// $codepro.audit.disable fileComment
package com.patchr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.widget.Toast;

import com.patchr.components.ActivityRecognitionManager;
import com.patchr.components.ContainerManager;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NotificationManager;
import com.patchr.components.Router;
import com.patchr.components.Stopwatch;
import com.patchr.components.StringManager;
import com.patchr.controllers.Messages;
import com.patchr.controllers.Notifications;
import com.patchr.controllers.Patches;
import com.patchr.controllers.Users;
import com.patchr.events.RegisterInstallEvent;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Session;
import com.patchr.objects.User;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utilities;
import com.amazonaws.auth.BasicAWSCredentials;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;
import com.kbeanie.imagechooser.api.BChooserPreferences;
import com.parse.Parse;
import com.parse.ParseInstallation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.branch.referral.Branch;
import io.fabric.sdk.android.Fabric;

public class Patchr extends MultiDexApplication {

	private static Patchr instance;

	public static Intent sendIntent;  // Used when we are started to handle a send intent

	public static Context                  applicationContext;
	public static PackageManager           packageManager;
	public static SharedPreferences        settings;
	public static SharedPreferences.Editor settingsEditor;

	@NonNull
	public static Handler   mainThreadHandler = new Handler(Looper.getMainLooper());
	@NonNull
	public static Router    router            = new Router();
	@NonNull
	public static Stopwatch stopwatch1        = new Stopwatch(); // $codepro.audit.disable stringLiterals;
	@NonNull
	public static Stopwatch stopwatch2        = new Stopwatch(); // $codepro.audit.disable stringLiterals;

	@NonNull
	public static String                         themeTone                 = ThemeTone.LIGHT;
	@NonNull
	public static Map<String, IEntityController> controllerMap             = new HashMap<String, IEntityController>();
	@NonNull
	public static Boolean                        debug                     = false;
	@NonNull
	public static Boolean                        applicationUpdateRequired = false;
	@NonNull
	public static Integer                        resultCode                = Activity.RESULT_OK; // Used to cascade up the activity chain

	public static BasicAWSCredentials awsCredentials = null;

	/* Container values */
	@NonNull
	public static String AWS_ACCESS_KEY  = "aws-access-key";
	@NonNull
	public static String AWS_SECRET_KEY  = "aws-secret-key";
	@NonNull
	public static String BING_ACCESS_KEY = "bing-access-key";
	@NonNull
	public static String USER_SECRET     = "user-secret";

	private Tracker mTracker;

	/* Current objects */
	private Entity   mCurrentPatch;
	private Activity mCurrentActivity;
	private User     mCurrentUser;

	/* Common preferences */
	private String mPrefTheme;

	/* Dev preferences */
	private Boolean mPrefEnableDev;
	private String  mPrefTestingBeacons;

	/* Install id components */
	private String mUniqueId;
	private Long   mUniqueDate;
	private String mUniqueType;

	public static Patchr getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		/*
		 * Application starts for all basic cases but also when not running and
		 * a broadcast receiver is activated.
		 */
		debug = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));

		super.onCreate();
		instance = this;
		instance.initializeInstance();
		Logger.d(this, "Application created");
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("CommitPrefEdits")
	protected void initializeInstance() {

		/* Must have this so activity rerouting works. */
		applicationContext = getApplicationContext();
		packageManager = applicationContext.getPackageManager();
		settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		settingsEditor = settings.edit();

		Logger.i(this, "First run configuration");

		/* Make sure setting defaults are initialized */
		PreferenceManager.setDefaultValues(this, R.xml.preferences_dev, true);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		/* Make sure unique id is initialized */
		initializeInstallInfo();

		/* Turn on crash reporting */
		Fabric.with(this, new Crashlytics());

		/* Turn on branch */
		Branch.getAutoInstance(this);

		/* Turn on parse */
		Parse.initialize(this
				, StringManager.getString(R.string.parse_app_id)        // application id
				, StringManager.getString(R.string.parse_client_key));  // client key
		Parse.setLogLevel(Constants.LOG_LEVEL);
		ParseInstallation.getCurrentInstallation().saveInBackground();

		/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
		snapshotPreferences();

		/* Establish device memory class */
		Logger.i(this, "Device memory class: " + String.valueOf(Utilities.maxMemoryMB()));

		/* Inject configuration */
		openContainer(StringManager.getString(R.string.id_container));

		/* Set the folder used by image chooser for all files */
		BChooserPreferences preferences = new BChooserPreferences(this);
		preferences.setFolderName("Pictures/Patchr");

		/* Initialize managers */
		initializeManagers();

		/* Warmup DataController */
		DataController.getInstance().warmup();

		/* Required to deserialize notifications */
		controllerMap.put(Constants.SCHEMA_ENTITY_PATCH, new Patches());
		controllerMap.put(Constants.SCHEMA_ENTITY_USER, new Users());
		controllerMap.put(Constants.SCHEMA_ENTITY_MESSAGE, new Messages());
		controllerMap.put(Constants.SCHEMA_ENTITY_NOTIFICATION, new Notifications());

		/* Must come after managers are initialized */
		signinAuto();

		/* Start activity recognition */
		ActivityRecognitionManager.getInstance().initialize(applicationContext);

		/* Ensure install is registered. */
		Boolean registered = Patchr.settings.getBoolean(StringManager.getString(R.string.setting_install_registered), false);
		if (!registered) {
			Dispatcher.getInstance().post(new RegisterInstallEvent());
		}
	}

	protected void initializeManagers() {

		/* Warmup media manager */
		MediaManager.warmup();

		/* Connectivity monitoring */
		NetworkManager.getInstance().initialize();
	}

	public void snapshotPreferences() {
		mPrefTheme = settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default));
		mPrefEnableDev = settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false);
		mPrefTestingBeacons = settings.getString(StringManager.getString(R.string.pref_testing_beacons), "natural");
	}

	public void openContainer(String containerId) {
		/*
		 * TagManager always starts by loading the defaults bundled with app. Next,
		 * it looks for the latest version of the container on disk (which it saved when pulling
		 * a fresh container version from the network). If none or stale, it tries to retrieve
		 * the current version from the network.
		 */
		TagManager tagManager = TagManager.getInstance(this);
		if (Patchr.debug) {
			tagManager.setVerboseLoggingEnabled(true);
		}
		PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(containerId, R.raw.gtm_default_container);

		pending.setResultCallback(new ResultCallback<ContainerHolder>() {

			@Override
			public void onResult(@NonNull ContainerHolder containerHolder) {

				if (!containerHolder.getStatus().isSuccess()) {
					// Called when a refresh failed for the given refresh type.
					Logger.v(this, "Container refresh failed");
					return;
				}

				/* Called when a successful refresh occurred for the given refresh type. */
				Logger.v(this, "Container refresh success");

				if (settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
						&& Patchr.getInstance().getCurrentUser() != null && Type.isTrue(Patchr.getInstance().getCurrentUser().developer)) {
					UI.showToastNotification("Container refreshed", Toast.LENGTH_SHORT);
				}

				activateContainer(containerHolder);

				containerHolder.setContainerAvailableListener(new ContainerHolder.ContainerAvailableListener() {
					@Override
					public void onContainerAvailable(@NonNull ContainerHolder containerHolder, String s) {
						activateContainer(containerHolder);
					}
				});
			}
		}, 2, TimeUnit.SECONDS);

		Logger.v(this, "Container set using default");
	}

	private void activateContainer(@NonNull ContainerHolder containerHolder) {
		ContainerManager.setContainerHolder(containerHolder);
		Container container = containerHolder.getContainer();
		if (!container.isDefault()) {
			String accessKey = container.getString(AWS_ACCESS_KEY);
			String secretKey = container.getString(AWS_SECRET_KEY);
			awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		}
	}

	public void signinAuto() {
		/*
		 * Gets called on app create.
		 */
		final String jsonUser = settings.getString(StringManager.getString(R.string.setting_user), null);
		final String jsonSession = settings.getString(StringManager.getString(R.string.setting_user_session), null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto log in using cached user...");
			final User user = (User) Json.jsonToObject(jsonUser, Json.ObjectType.ENTITY);
			user.session = (Session) Json.jsonToObject(jsonSession, Json.ObjectType.SESSION);
			setCurrentUser(user, false);  // Does not block because of 'false', also updates persisted user
			return;
		}

		/* Couldn't auto signin so fall back to anonymous */
		final User anonymous = (User) DataController.getInstance().loadEntityFromResources(R.raw.user_entity, Json.ObjectType.ENTITY);
		setCurrentUser(anonymous, false);  // Does not block because of 'false'
		Logger.i(this, "Auto log in using anonymous user");
	}

	@NonNull
	public IEntityController getControllerForSchema(@NonNull String schema) {
		if (!controllerMap.containsKey(schema)) {
			throw new IllegalArgumentException("No controller for schema: " + schema.toString());
		}
		return controllerMap.get(schema);
	}

	@NonNull
	public IEntityController getControllerForClass(@NonNull Class<?> clazz) {
		String schema = clazz.getSimpleName().toLowerCase(Locale.US);
		if (!controllerMap.containsKey(schema)) {
			throw new IllegalArgumentException("No controller for schema: " + schema.toString());
		}
		return controllerMap.get(schema);
	}

	@NonNull
	public IEntityController getControllerForEntity(@NonNull Entity entity) {
		String schema = entity.schema;
		if (!controllerMap.containsKey(schema)) {
			throw new IllegalArgumentException("No controller for schema: " + schema.toString());
		}
		return controllerMap.get(schema);
	}

	@NonNull
	public synchronized String getInstallType() {
		return mUniqueType;
	}

	@NonNull
	public synchronized Long getInstallDate() {
		return mUniqueDate;
	}

	@NonNull
	public synchronized String getinstallId() {
		if (mUniqueId == null) {
			initializeInstallInfo();
		}
		return mUniqueId;
	}

	private void initializeInstallInfo() {
		mUniqueId = settings.getString(StringManager.getString(R.string.setting_unique_id), null);
		mUniqueDate = settings.getLong(StringManager.getString(R.string.setting_unique_id_date), 0);
		mUniqueType = settings.getString(StringManager.getString(R.string.setting_unique_id_type), null);
		if (mUniqueId == null || mUniqueType == null) {
			if (Build.SERIAL != null && !Build.SERIAL.equals("unknown")) {
				mUniqueId = Build.SERIAL;
				mUniqueType = Constants.INSTALL_TYPE_SERIAL;
			}
			else {
				String androidId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
				if (androidId != null) {
					mUniqueId = androidId;
					mUniqueType = Constants.INSTALL_TYPE_ANDROID_ID;
				}
				else {
					mUniqueId = UUID.randomUUID().toString();
					mUniqueType = Constants.INSTALL_TYPE_RANDOM;
				}
			}
			mUniqueDate = DateTime.nowDate().getTime();
			settingsEditor.putString(StringManager.getString(R.string.setting_unique_id_type), mUniqueType);
			settingsEditor.putString(StringManager.getString(R.string.setting_unique_id), mUniqueId);
			settingsEditor.putLong(StringManager.getString(R.string.setting_unique_id_date), mUniqueDate);
			settingsEditor.commit();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Statics
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public static String getVersionName(@NonNull Context context, @NonNull Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = packageManager.getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
		}
		throw new IllegalArgumentException("Failed to get version name");
	}

	@NonNull
	public static Integer getVersionCode(@NonNull Context context, @NonNull Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = packageManager.getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionCode;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
		}
		throw new IllegalArgumentException("Failed to get version code");
	}

	public static class ThemeTone {
		@NonNull
		public static String DARK  = "dark";
		@NonNull
		public static String LIGHT = "light";
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public Boolean setCurrentUser(@NonNull User user, @NonNull Boolean refreshUser) {

		mCurrentUser = user;
		ModelResult result = new ModelResult();

		if (user.isAnonymous()) {

			Logger.i(this, "Activating anonymous user");

			/* Cancel any current notifications in the status bar */
			NotificationManager.getInstance().cancelAllNotifications();

			/* Clear user settings */
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), null);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), null);
			Patchr.settingsEditor.commit();
		}
		else {

			Logger.i(this, "Activating authenticated user: " + Patchr.getInstance().getCurrentUser().id);

			/* Load user data */
			if (refreshUser) {
				LinkSpec options = LinkSpecFactory.build(LinkSpecType.LINKS_FOR_USER_CURRENT);
				result = DataController.getInstance().getEntity(user.id, true, options, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			/* Update settings */
			final String jsonUser = Json.objectToJson(user);
			final String jsonSession = Json.objectToJson(user.session);

			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), jsonUser);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_last_email), user.email);
			Patchr.settingsEditor.commit();
		}

		Reporting.updateCrashUser(user);
		return (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS);
	}

	public void setCurrentActivity(Activity currentActivity) {
		mCurrentActivity = currentActivity;
	}

	public void setCurrentPatch(Entity currentPatch) {
		mCurrentPatch = currentPatch;
		Logger.v(this, "Setting current patch to: " + currentPatch);
	}

	synchronized public Tracker getTracker() {
		/* Setup the analytics tracker */
		if (mTracker == null) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			/*
			 * Set how often auto dispatch gets fired in seconds.
			 * Note: The default is 30 minutes which is crazy. iOS default is 2 minutes so we are
			 * going to be a bit aggresive and make it one minute for ship.
			 */
			analytics.setLocalDispatchPeriod(Constants.TIME_ONE_MINUTE);
			mTracker = analytics.newTracker(R.xml.analytics);
		}
		return mTracker;
	}

	public User getCurrentUser() {
		return mCurrentUser;
	}

	@NonNull
	public Boolean getPrefEnableDev() {
		return mPrefEnableDev;
	}

	@NonNull
	public String getPrefTestingBeacons() {
		return mPrefTestingBeacons;
	}

	public Activity getCurrentActivity() {
		return mCurrentActivity;
	}

	public Entity getCurrentPatch() {
		return mCurrentPatch;
	}
}
