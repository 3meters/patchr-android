// $codepro.audit.disable fileComment
package com.patchr;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.adobe.creativesdk.aviary.AdobeImageIntent;
import com.adobe.creativesdk.aviary.IAviaryClientCredentials;
import com.adobe.creativesdk.foundation.AdobeCSDKFoundation;
import com.amazonaws.auth.BasicAWSCredentials;
import com.bugsnag.android.Bugsnag;
import com.facebook.FacebookSdk;
import com.facebook.accountkit.AccountKit;
import com.facebook.stetho.Stetho;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;
import com.google.gson.Gson;
import com.kbeanie.imagechooser.api.BChooserPreferences;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.patchr.components.ContainerManager;
import com.patchr.components.Foreground;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.Router;
import com.patchr.components.Stopwatch;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.enums.Preference;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.segment.analytics.Analytics;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.branch.referral.Branch;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Patchr extends Application implements IAviaryClientCredentials {

	private static Patchr instance;

	public static Intent sendIntent;  // Used when we are started to handle a send intent

	public static Context           applicationContext;
	public static Gson              gson;
	public static SharedPreferences settings;

	public static Handler   mainThreadHandler         = new Handler(Looper.getMainLooper());
	public static Router    router                    = new Router();
	public static Stopwatch stopwatch1                = new Stopwatch();
	public static Stopwatch stopwatch2                = new Stopwatch();
	public static Boolean   debuggable                = false;
	public static Boolean   applicationUpdateRequired = false;

	public static BasicAWSCredentials awsCredentials = null;

	/* Container values */
	public static  String AWS_ACCESS_KEY             = "aws-access-key";
	public static  String AWS_SECRET_KEY             = "aws-secret-key";
	public static  String BING_ACCESS_KEY            = "bing-access-key";
	public static  String USER_SECRET                = "user-secret";
	private static String CREATIVE_SDK_CLIENT_SECRET = "creative-sdk-client-secret";

	public  Boolean prefEnableDev;
	public  String  prefTestingBeacons;
	private String  advertisingId;
	private String  uniqueId;
	private Long    uniqueDate;
	private String  uniqueType;

	public static Patchr getInstance() {
		return instance;
	}

	@Override public void onCreate() {
		/*
		 * Application starts for all basic cases but also when not running and
		 * a broadcast receiver is activated.
		 */
		debuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));

		super.onCreate();
		if (instance == null) {
			instance = this;
			instance.initializeInstance();
			Logger.d(this, "Application created");
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initializeInstance() {

		/* Must have this so activity rerouting works. */
		applicationContext = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);

		Logger.i(this, "First run configuration");

		Foreground.init(this);

		/* Inject configuration */
		openContainer(StringManager.getString(R.string.id_container));

		/* Turn on branch */
		Branch.getAutoInstance(this);

		/* Init Creative Sdk */
		AdobeCSDKFoundation.initializeCSDKFoundation(getApplicationContext());

		AsyncTask.execute(() -> {

			/* Make sure setting defaults are initialized */
			PreferenceManager.setDefaultValues(this, R.xml.preferences_dev, true);
			PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

			/* Start preloading assets */
			Intent cdsIntent = AdobeImageIntent.createCdsInitIntent(getBaseContext(), "CDS");
			startService(cdsIntent);

			/* Make sure unique id is initialized */
			initializeInstallInfo();

			/* Turn on segement to gather user data */
			Analytics analytics = new Analytics.Builder(this, "81Q9wmANTOA6PLVlipPvSRHw97SJBENF").build();
			Analytics.setSingletonInstance(analytics);

			/* Turn on crash reporting */
			Bugsnag.init(this);

			/* Turn on facebook */
			FacebookSdk.sdkInitialize(this);

			/* Turn on account kit */
			if (BuildConfig.ACCOUNT_KIT_ENABLED) {
				AccountKit.initialize(applicationContext);
			}

			/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
			snapshotPreferences();

			/* Establish device memory class */
			Logger.i(this, "Device memory class: " + String.valueOf(Utils.maxMemoryMB()));

			/* Set the folder used by image chooser for all files */
			BChooserPreferences preferences = new BChooserPreferences(this);
			preferences.setFolderName("Pictures/Patchr");

			/* Turn on parse */
			Parse.initialize(this
				, StringManager.getString(R.string.parse_app_id)        // application id
				, StringManager.getString(R.string.parse_client_key));  // client key
			Parse.setLogLevel(Constants.LOG_LEVEL);
			ParseInstallation.getCurrentInstallation().saveInBackground();

			/* Stetho */
			Stetho.initialize(
				Stetho.newInitializerBuilder(this)
					.enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
					.enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
					.build());

			/* Configure realm */
			RealmConfiguration config = new RealmConfiguration.Builder(Patchr.applicationContext)
				.name("patchr.realm")
				.deleteRealmIfMigrationNeeded()
				.build();

			Realm.setDefaultConfiguration(config);

			/* Stash gson */
			gson = new Gson();

			/* Warmup media manager */
			MediaManager.warmup();

			/* Connectivity monitoring */
			NetworkManager.getInstance().initialize();

			/* Must come after managers are initialized */
			UserManager.shared().loginAuto();
		});
	}

	public void snapshotPreferences() {
		prefEnableDev = settings.getBoolean(Preference.ENABLE_DEV, false);
		prefTestingBeacons = settings.getString(Preference.TESTING_BEACONS, "natural");
	}

	@Override public String getClientID() {
		return StringManager.getString(R.string.creative_sdk_client_id);
	}

	@Override public String getClientSecret() {
		return "7af6a808-0568-477e-88c2-63fa6e8ef617";
	}

	@Override public String getBillingKey() {
		return ""; // Leave this blank
	}

	public void openContainer(String containerId) {
		/*
		 * TagManager always starts by loading the defaults bundled with app. Next,
		 * it looks for the latest version of the container on disk (which it saved when pulling
		 * a fresh container version from the network). If none or stale, it tries to retrieve
		 * the current version from the network.
		 */
		TagManager tagManager = TagManager.getInstance(this);
		tagManager.setVerboseLoggingEnabled(false);

		PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(containerId, R.raw.gtm_default_container);
		/*
		 * The onResult method will be called as soon as one of the following happens:
		 *  1. a saved container is loaded
		 *  2. if there is no saved container, a network container is loaded
		 *  3. the 2-second timeout occurs
		 */
		pending.setResultCallback(new ResultCallback<ContainerHolder>() {

			@Override public void onResult(@NonNull ContainerHolder containerHolder) {

				if (!containerHolder.getStatus().isSuccess()) {
					// Called when a refresh failed for the given refresh type.
					Logger.v(this, "Container refresh failed");
					return;
				}

				/* Called when a successful refresh occurred for the given refresh type. */
				Logger.v(this, "Container refresh success");

				if (Utils.devModeEnabled()) {
					UI.toast("Container refreshed");
				}

				activateContainer(containerHolder);

				containerHolder.setContainerAvailableListener(new ContainerHolder.ContainerAvailableListener() {
					@Override
					public void onContainerAvailable(ContainerHolder containerHolder, String s) {
						activateContainer(containerHolder);
					}
				});
			}
		}, 2, TimeUnit.SECONDS);

		Logger.v(this, "Container set using default");
	}

	private void activateContainer(ContainerHolder containerHolder) {

		ContainerManager.setContainerHolder(containerHolder);
		Container container = containerHolder.getContainer();

		if (!container.isDefault()) {

			String accessKey = container.getString(AWS_ACCESS_KEY);
			String secretKey = container.getString(AWS_SECRET_KEY);
			awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		}
	}

	public synchronized String getInstallType() {
		return uniqueType;
	}

	public synchronized Long getInstallDate() {
		return uniqueDate;
	}

	public synchronized String getinstallId() {
		if (uniqueId == null) {
			initializeInstallInfo();
		}
		return uniqueId;
	}

	private void initializeInstallInfo() {

		uniqueId = settings.getString(StringManager.getString(R.string.setting_unique_id), null);
		uniqueDate = settings.getLong(StringManager.getString(R.string.setting_unique_id_date), 0);
		uniqueType = settings.getString(StringManager.getString(R.string.setting_unique_id_type), null);

		if (uniqueId == null || uniqueType == null) {

			/* Try to use android id first */
			String androidId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
			if (androidId != null) {
				uniqueId = androidId;
				uniqueType = Constants.INSTALL_TYPE_ANDROID_ID;
			}
			/* Hardware serial number */
			else if (Build.SERIAL != null && !Build.SERIAL.equals("unknown")) {
				uniqueId = Build.SERIAL;
				uniqueType = Constants.INSTALL_TYPE_SERIAL;
			}
			/* Generate a unique number for this device */
			else {
				uniqueId = UUID.randomUUID().toString();
				uniqueType = Constants.INSTALL_TYPE_RANDOM;
			}

			uniqueDate = DateTime.nowDate().getTime();
			settings.edit().putString(StringManager.getString(R.string.setting_unique_id_type), uniqueType);
			settings.edit().putString(StringManager.getString(R.string.setting_unique_id), uniqueId);
			settings.edit().putLong(StringManager.getString(R.string.setting_unique_id_date), uniqueDate);
			settings.edit().apply();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Statics
	 *--------------------------------------------------------------------------------------------*/

	public static String getVersionName(Context context, Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = applicationContext.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
		}
		throw new IllegalArgumentException("Failed to get version name");
	}

	public static Integer getVersionCode(Context context, Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = applicationContext.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionCode;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
		}
		throw new IllegalArgumentException("Failed to get version code");
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
}
