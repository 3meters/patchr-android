// $codepro.audit.disable fileComment
package com.aircandi;

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

import com.aircandi.components.AnimationManager;
import com.aircandi.components.ContainerManager;
import com.aircandi.components.DispatchManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.MenuManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.Stopwatch;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.Messages;
import com.aircandi.controllers.Notifications;
import com.aircandi.controllers.Patches;
import com.aircandi.controllers.Places;
import com.aircandi.controllers.Users;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Links;
import com.aircandi.objects.Session;
import com.aircandi.objects.User;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;
import com.amazonaws.auth.BasicAWSCredentials;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;

;

public class Patchr extends MultiDexApplication {

	public static BasicAWSCredentials awsCredentials = null;

	private static Patchr singletonObject;

	public static Intent firstStartIntent = null;

	@NonNull
	public static SharedPreferences        settings;
	@NonNull
	public static SharedPreferences.Editor settingsEditor;
	@NonNull
	public static Context                  applicationContext;
	@NonNull
	public static Handler                  mainThreadHandler;
	@NonNull
	public static PackageManager           packageManager;
	@NonNull
	public static DispatchManager          dispatch;
	@NonNull
	public static Integer                  memoryClass;
	@NonNull
	public static Stopwatch                stopwatch1;
	@NonNull
	public static Stopwatch                stopwatch2;

	@NonNull
	public static String                         themeTone                 = ThemeTone.LIGHT;
	@NonNull
	public static Map<String, IEntityController> controllerMap             = new HashMap<String, IEntityController>();
	@NonNull
	public static Boolean                        firstStartApp             = true;
	@NonNull
	public static Boolean                        debug                     = false;
	@NonNull
	public static Boolean                        usingEmulator             = false;
	@NonNull
	public static Integer                        wifiCount                 = 0;
	@NonNull
	public static Boolean                        applicationUpdateRequired = false;
	@NonNull
	public static Integer                        resultCode                = Activity.RESULT_OK;

	/* Container values */
	@NonNull
	public static String AWS_ACCESS_KEY  = "aws-access-key";
	@NonNull
	public static String AWS_SECRET_KEY  = "aws-secret-key";
	@NonNull
	public static String BING_ACCESS_KEY = "bing-access-key";
	@NonNull
	public static String USER_SECRET     = "user-secret";

	public Tracker mTracker;

	/* Current objects */
	private Entity   mCurrentPatch;
	private Activity mCurrentActivity;
	private User     mCurrentUser;

	/* Common preferences */
	@NonNull
	private String mPrefTheme;

	/* Dev preferences */
	@NonNull
	private Boolean mPrefEnableDev;
	@NonNull
	private String  mPrefTestingBeacons;

	/* Shared managers */
	@NonNull
	protected EntityManager    mEntityManager;
	@NonNull
	protected MenuManager      mMenuManager;
	@NonNull
	protected AnimationManager mAnimationManager;

	/* Install id components */
	private String mUniqueId;
	private Long   mUniqueDate;
	private String mUniqueType;

	public static Patchr getInstance() {
		return singletonObject;
	}

	@Override
	public void onCreate() {
		/*
		 * Application starts for all basic cases but also when not running and
		 * a broadcast receiver is activated.
		 */
		super.onCreate();
		singletonObject = this;
		debug = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
		singletonObject.initializeInstance();
		Logger.d(this, "Application created");
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("CommitPrefEdits")
	protected void initializeInstance() {

		/* Must have this so activity rerouting works. */
		Patchr.applicationContext = getApplicationContext();

		stopwatch1 = new Stopwatch(); // $codepro.audit.disable stringLiterals
		stopwatch2 = new Stopwatch(); // $codepro.audit.disable stringLiterals

		mainThreadHandler = new Handler(Looper.getMainLooper());
		packageManager = applicationContext.getPackageManager();

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		settingsEditor = settings.edit();

		/* Make sure setting defaults are initialized */
		PreferenceManager.setDefaultValues(this, R.xml.preferences_dev, true);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		/* Make sure unique id is initialized */
		initializeInstallInfo();

		/* Turn on crash reporting */
		Fabric.with(this, new Crashlytics());

		/* Setup the analytics tracker */
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		if (analytics != null) {
			/*
			 * Set how often auto dispatch gets fired in seconds.
			 * Note: The default is 30 minutes which is crazy. iOS default is 2 minutes so we are
			 * going to be a bit aggresive and make it one minute for ship.
			 */
			analytics.setLocalDispatchPeriod(Constants.TIME_ONE_MINUTE);

			/* Info on what is being tracked is output to logcat */
			analytics.getLogger().setLogLevel(com.google.android.gms.analytics.Logger.LogLevel.VERBOSE);
			Patchr.getInstance().setTracker(analytics.newTracker(R.xml.analytics));
		}

		/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
		snapshotPreferences();

		if (Build.PRODUCT.contains("sdk")) {
			usingEmulator = true;
		}

		/* Establish device memory class */
		Patchr.memoryClass = Utilities.maxMemoryMB();
		Logger.i(this, "Device memory class: " + String.valueOf(memoryClass));

		/* Inject configuration */
		openContainer(StringManager.getString(R.string.id_container));

		/* Initialize managers */
		initializeManagers();

		/* Required to deserialize notifications */
		controllerMap.put(Constants.SCHEMA_ENTITY_PATCH, new Patches());
		controllerMap.put(Constants.SCHEMA_ENTITY_PLACE, new Places());
		controllerMap.put(Constants.SCHEMA_ENTITY_USER, new Users());
		controllerMap.put(Constants.SCHEMA_ENTITY_MESSAGE, new Messages());
		controllerMap.put(Constants.SCHEMA_ENTITY_NOTIFICATION, new Notifications());

		/* Start out with anonymous user then upgrade to signed in user if possible */
		Patchr.getInstance().initializeUser();
	}

	protected void initializeManagers() {

		/* Warmup media manager */
		MediaManager.warmup();

		/* Inject minimum managers required for notifications */
		mEntityManager = new EntityManager().setLinks(new Links());
		mMenuManager = new MenuManager();
		mAnimationManager = new AnimationManager();

		/* Inject dispatch manager */
		dispatch = new DispatchManager();

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();
	}

	public void initializeUser() {
		signinAuto();
		Logger.i(this, "User initialized");
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
		tagManager.setVerboseLoggingEnabled(true);
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
		 * Gets called on app create and after restart and ending with the back key.
		 */
		final String jsonUser = Patchr.settings.getString(StringManager.getString(R.string.setting_user), null);
		final String jsonSession = Patchr.settings.getString(StringManager.getString(R.string.setting_user_session), null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto sign in...");
			final User user = (User) Json.jsonToObject(jsonUser, Json.ObjectType.ENTITY);
			user.session = (Session) Json.jsonToObject(jsonSession, Json.ObjectType.SESSION);
			Patchr.getInstance().setCurrentUser(user, false);
			return;
		}

		/* Couldn't auto signin so fall back to anonymous */
		final User anonymous = (User) mEntityManager.loadEntityFromResources(R.raw.user_entity, Json.ObjectType.ENTITY);
		setCurrentUser(anonymous, false);
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
		mUniqueId = Patchr.settings.getString(StringManager.getString(R.string.setting_unique_id), null);
		mUniqueDate = Patchr.settings.getLong(StringManager.getString(R.string.setting_unique_id_date), 0);
		mUniqueType = Patchr.settings.getString(StringManager.getString(R.string.setting_unique_id_type), null);
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
			mUniqueId += "." + applicationContext.getPackageName();
			mUniqueDate = DateTime.nowDate().getTime();
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_unique_id_type), mUniqueType);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_unique_id), mUniqueId);
			Patchr.settingsEditor.putLong(StringManager.getString(R.string.setting_unique_id_date), mUniqueDate);
			Patchr.settingsEditor.commit();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Statics
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public static String getVersionName(@NonNull Context context, @NonNull Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
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
			final PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
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

	public void setTracker(@NonNull Tracker tracker) {
		mTracker = tracker;
	}

	@NonNull
	public Boolean setCurrentUser(@NonNull User user, @NonNull Boolean refreshUser) {
		mCurrentUser = user;
		ModelResult result = mEntityManager.activateCurrentUser(refreshUser);
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

	public Tracker getTracker() {
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

	@NonNull
	public MenuManager getMenuManager() {
		return mMenuManager;
	}

	@NonNull
	public EntityManager getEntityManager() {
		return mEntityManager;
	}

	@NonNull
	public AnimationManager getAnimationManager() {
		return mAnimationManager;
	}
}
