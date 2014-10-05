// $codepro.audit.disable fileComment
package com.aircandi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
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
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.aircandi.components.ActivityDecorator;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DispatchManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.MenuManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.Stopwatch;
import com.aircandi.components.StringManager;
import com.aircandi.components.TrackerDelegate;
import com.aircandi.components.TrackerGoogleEasy;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Session;
import com.aircandi.objects.User;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.tagmanager.Container;
import com.google.tagmanager.Container.RefreshFailure;
import com.google.tagmanager.Container.RefreshType;
import com.google.tagmanager.Logger.LogLevel;
import com.google.tagmanager.TagManager;
import com.google.tagmanager.TagManager.RefreshMode;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Patch extends Application {

	public static BasicAWSCredentials awsCredentials = null;

	private static Patch singletonObject;

	public static SharedPreferences        settings;
	public static SharedPreferences.Editor settingsEditor;

	public static Context applicationContext;

	public static Handler         mainThreadHandler;
	public static PackageManager  packageManager;
	public static TrackerDelegate tracker;
	public static GoogleAnalytics analytics;
	public static DispatchManager dispatch;
	public static Map<String, IEntityController> controllerMap = new HashMap<String, IEntityController>();

	public static DisplayMetrics displayMetrics;
	public static Integer        memoryClass;

	public static Stopwatch stopwatch1;
	public static Stopwatch stopwatch2;

	public static  Boolean firstStartApp    = true;
	public static  Boolean DEBUG            = false;
	public static  Intent  firstStartIntent = null;
	private static Boolean activityVisible  = false;
	public static  Boolean usingEmulator    = false;
	public static  Integer wifiCount        = 0;

	public static String themeTone;

	public static Boolean applicationUpdateRequired = false;

	private static String  uniqueId   = null;
	private static Long    uniqueDate = null;
	private static String  uniqueType = null;
	public static  Integer resultCode = Activity.RESULT_OK;

	/* Container values */
	public static String AWS_ACCESS_KEY  = "aws-access-key";
	public static String AWS_SECRET_KEY  = "aws-secret-key";
	public static String BING_ACCESS_KEY = "bing-access-key";
	public static String USER_SECRET     = "user-secret";

	/* Current objects */
	private Entity   mCurrentPlace;
	private Activity mCurrentActivity;
	private User     mCurrentUser;

	/* Common preferences */
	private String mPrefTheme;
	private String mPrefSearchRadius;

	/* Dev preferences */
	private Boolean mPrefEnableDev;
	private Boolean mPrefEntityFencing;
	private String  mPrefTestingBeacons;

	/* Shared managers */
	protected MenuManager       mMenuManager;
	protected EntityManager     mEntityManager;
	protected ActivityDecorator mActivityDecorator;
	protected MediaManager      mMediaManager;
	private   AnimationManager  mAnimationManager;

	/* Injected configuration */
	protected Container mContainer;

	public static Patch getInstance() {
		return singletonObject;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singletonObject = this;
		DEBUG = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
		singletonObject.initializeInstance();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("CommitPrefEdits")
	protected void initializeInstance() {

		/* Must have this so activity rerouting works. */
		Patch.applicationContext = getApplicationContext();

		/* Start crashlytics reporting */
		Reporting.startCrashReporting(this);

		stopwatch1 = new Stopwatch(); // $codepro.audit.disable stringLiterals
		stopwatch2 = new Stopwatch(); // $codepro.audit.disable stringLiterals

		if (applicationContext == null) {
			applicationContext = getApplicationContext();
		}

		mainThreadHandler = new Handler(Looper.getMainLooper());
		packageManager = applicationContext.getPackageManager();

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		settingsEditor = settings.edit();

		/* Make sure setting defaults are initialized */
		PreferenceManager.setDefaultValues(this, R.xml.preferences_dev, true);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		/* Make sure unique id is initialized */
		Patch.getinstallId();

		/* Setup the analytics tracker */
		tracker = new TrackerGoogleEasy();
		analytics = GoogleAnalytics.getInstance(this);
		tracker.applicationStart();

		/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
		snapshotPreferences();

		if (Build.PRODUCT.contains("sdk")) {
			usingEmulator = true;
		}

		/* Force actionbar overflow */
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}
		catch (Exception ignore) {}

		/* Establish device memory class */
		Utilities.calculateMemoryCacheSize(getApplicationContext());

		/* Initialize managers */
		initializeManagers();

		/* Inject configuration */
		openContainer(StringManager.getString(R.string.id_container), RefreshMode.STANDARD);

		/* Inject dispatch manager */
		Patch.dispatch = new DispatchManager();
	}

	protected void initializeManagers() {
		/*
		 * Additional setup is done in SplashForm#configure
		 */

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();
		Reporting.updateCrashKeys();
	}

	public void initializeUser() {
		/*
		 * Start with anonymous user as the default and then try to auto signin..
		 */
		final User anonymous = (User) mEntityManager.loadEntityFromResources(R.raw.user_entity, Json.ObjectType.ENTITY);
		setCurrentUser(anonymous);
		signinAuto();
		mEntityManager.activateCurrentUser(false);
		Logger.i(this, "User initialized");
	}

	public void snapshotPreferences() {
		mPrefTheme = Patch.settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default));
		mPrefSearchRadius = Patch.settings.getString(StringManager.getString(R.string.pref_search_radius),
				StringManager.getString(R.string.pref_search_radius_default));
		mPrefEnableDev = Patch.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false);
		mPrefTestingBeacons = Patch.settings.getString(StringManager.getString(R.string.pref_testing_beacons), "natural");
	}

	@SuppressWarnings("ucd")
	public void openContainer(String containerId, RefreshMode refreshMode) {
		/*
		 * TagManager always starts by loading the defaults bundled with app. Next,
		 * it looks for the latest version of the container on disk (which it saved when pulling
		 * a fresh container version from the network). If none or stale, it tries to retrieve
		 * the current version from the network.
		 */

		TagManager tagManager = TagManager.getInstance(this);
		tagManager.setRefreshMode(refreshMode); // TODO: Must be set to standard to get fresh pulls
		tagManager.getLogger().setLogLevel(LogLevel.VERBOSE);

		mContainer = tagManager.openContainer(containerId, new Container.Callback() {

			@Override
			public void containerRefreshBegin(Container container, RefreshType refreshType) {
				/* Called when a refresh is about to begin for the given refresh type. */
				Logger.v(this, "Container refresh starting: " + refreshType.toString());
			}

			@Override
			public void containerRefreshSuccess(Container container, RefreshType refreshType) {
				/* Called when a successful refresh occurred for the given refresh type. */
				Logger.v(this, "Container refresh success: " + refreshType.toString());

				if (Patch.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
						&& Patch.getInstance().getCurrentUser() != null && Type.isTrue(Patch.getInstance().getCurrentUser().developer)) {
					UI.showToastNotification("Container refreshed: " + refreshType.toString(), Toast.LENGTH_SHORT);
				}

				if (!container.isDefault()) {
					String accessKey = container.getString(AWS_ACCESS_KEY);
					String secretKey = container.getString(AWS_SECRET_KEY);
					awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
				}
			}

			@Override
			public void containerRefreshFailure(Container container, RefreshType refreshType, RefreshFailure refreshFailure) {
				// Called when a refresh failed for the given refresh type.
				Logger.v(this, "Container refresh failed: " + refreshType.toString() + ": " + refreshFailure.toString());
			}
		});
		Logger.v(this, "Container set using default");
	}

	public Boolean signinAuto() {
		/*
		 * Gets called on app create and after restart and ending with the back key.
		 */
		final String jsonUser = Patch.settings.getString(StringManager.getString(R.string.setting_user), null);
		final String jsonSession = Patch.settings.getString(StringManager.getString(R.string.setting_user_session), null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto sign in...");
			final User user = (User) Json.jsonToObject(jsonUser, Json.ObjectType.ENTITY);
			if (user != null) {
				user.session = (Session) Json.jsonToObject(jsonSession, Json.ObjectType.SESSION);
				if (user.session != null) {
					Patch.getInstance().setCurrentUser(user);
					return true;
				}
			}
		}
		return false;
	}

	public IEntityController getControllerForSchema(String schema) {
		return controllerMap.get(schema);
	}

	public IEntityController getControllerForClass(Class<?> clazz) {
		return controllerMap.get(clazz.getSimpleName().toLowerCase(Locale.US));
	}

	public IEntityController getControllerForEntity(Entity entity) {
		return controllerMap.get(entity.schema);
	}

	/*--------------------------------------------------------------------------------------------
	 * Statics
	 *--------------------------------------------------------------------------------------------*/

	public static String getVersionName(Context context, Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
			return null;
		}
	}

	public static Integer getVersionCode(Context context, Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionCode;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
			return null;
		}
	}

	public synchronized static String getInstallType() {
		if (uniqueType == null) {
			getinstallId();
		}
		return uniqueType;
	}

	public synchronized static Long getInstallDate() {
		if (uniqueDate == null) {
			getinstallId();
		}
		return uniqueDate;
	}

	public synchronized static String getinstallId() {
		if (uniqueId == null) {
			uniqueId = Patch.settings.getString(StringManager.getString(R.string.setting_unique_id), null);
			uniqueDate = Patch.settings.getLong(StringManager.getString(R.string.setting_unique_id_date), 0);
			uniqueType = Patch.settings.getString(StringManager.getString(R.string.setting_unique_id_type), null);
			if (uniqueId == null || uniqueType == null) {
				if (Build.SERIAL != null && !Build.SERIAL.equals("unknown")) {
					uniqueId = Build.SERIAL;
					uniqueType = Constants.INSTALL_TYPE_SERIAL;
				}
				else {
					String androidId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
					if (androidId != null) {
						uniqueId = androidId;
						uniqueType = Constants.INSTALL_TYPE_ANDROID_ID;
					}
					else {
						uniqueId = UUID.randomUUID().toString();
						uniqueType = Constants.INSTALL_TYPE_RANDOM;
					}
				}
				uniqueId += "." + applicationContext.getPackageName();
				uniqueDate = DateTime.nowDate().getTime();
				Patch.settingsEditor.putString(StringManager.getString(R.string.setting_unique_id_type), uniqueType);
				Patch.settingsEditor.putString(StringManager.getString(R.string.setting_unique_id), uniqueId);
				Patch.settingsEditor.putLong(StringManager.getString(R.string.setting_unique_id_date), uniqueDate);
				Patch.settingsEditor.commit();
			}
		}
		return uniqueId;
	}

	public static class ThemeTone {

		public static String DARK  = "dark";
		public static String LIGHT = "light";
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public void setCurrentUser(User user) {
		mCurrentUser = user;
		Reporting.updateCrashUser(user);
	}

	public User getCurrentUser() {
		return mCurrentUser;
	}

	public String getPrefTheme() {
		return mPrefTheme;
	}

	public String getPrefSearchRadius() {
		return mPrefSearchRadius;
	}

	public Boolean getPrefEnableDev() {
		return mPrefEnableDev;
	}

	public Boolean getPrefEntityFencing() {
		return mPrefEntityFencing;
	}

	public String getPrefTestingBeacons() {
		return mPrefTestingBeacons;
	}

	public Activity getCurrentActivity() {
		return mCurrentActivity;
	}

	public void setCurrentActivity(Activity currentActivity) {
		mCurrentActivity = currentActivity;
	}

	public Entity getCurrentPlace() {
		return mCurrentPlace;
	}

	public void setCurrentPlace(Entity currentPlace) {
		mCurrentPlace = currentPlace;
	}

	public MenuManager getMenuManager() {
		return mMenuManager;
	}

	public Container getContainer() {
		return mContainer;
	}

	public ActivityDecorator getActivityDecorator() {
		return mActivityDecorator;
	}

	public MediaManager getMediaManager() {
		return mMediaManager;
	}

	public EntityManager getEntityManager() {
		return mEntityManager;
	}

	public AnimationManager getAnimationManager() {
		return mAnimationManager;
	}

	public Patch setMenuManager(MenuManager menuManager) {
		mMenuManager = menuManager;
		return this;
	}

	public Patch setActivityDecorator(ActivityDecorator activityDecorator) {
		mActivityDecorator = activityDecorator;
		return this;
	}

	public Patch setEntityManager(EntityManager entityManager) {
		mEntityManager = entityManager;
		return this;
	}

	public Patch setMediaManager(MediaManager mediaManager) {
		mMediaManager = mediaManager;
		return this;
	}

	public Patch setAnimationManager(AnimationManager animationManager) {
		mAnimationManager = animationManager;
		return this;
	}

	public static Boolean isActivityVisible() {
		return activityVisible;
	}

	public static void activityResumed() {
		activityVisible = false;
	}

	public static void activityPaused() {
		activityVisible = false;
	}
}
