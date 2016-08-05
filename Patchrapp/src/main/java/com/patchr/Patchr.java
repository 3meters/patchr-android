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
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.adobe.creativesdk.foundation.AdobeCSDKFoundation;
import com.adobe.creativesdk.foundation.auth.IAdobeAuthClientCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.bugsnag.android.Bugsnag;
import com.facebook.FacebookSdk;
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

public class Patchr extends Application implements IAdobeAuthClientCredentials {

	private static Patchr instance;

	public static Intent sendIntent;  // Used when we are started to handle a send intent

	public static Context           applicationContext;
	public static Gson              gson;
	public static SharedPreferences settings;
	public static JobManager        jobManager;

	public static Handler   mainThread                = new Handler(Looper.getMainLooper());
	public static Stopwatch stopwatch1                = new Stopwatch();
	public static Stopwatch stopwatch2                = new Stopwatch();
	public static Boolean   debuggable                = false;
	public static Boolean   applicationUpdateRequired = false;

	public static BasicAWSCredentials awsCredentials = null;

	/* Container values */
	public static  String AWS_ACCESS_KEY             = "aws-access-key";
	public static  String AWS_SECRET_KEY             = "aws-secret-key";
	public static  String BING_SUBSCRIPTION_KEY      = "bing-subscription-key";
	public static  String USER_SECRET                = "user-secret";
	private static String CREATIVE_SDK_CLIENT_SECRET = "creative-sdk-client-secret";

	public  Boolean prefEnableDev;
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
			applicationContext = getApplicationContext();
			Logger.i(this, "Application created");
			instance.initializeInstance();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initializeInstance() {

		Logger.d(this, "Starting app initialization");

		/* Turn on crash reporting */
		Bugsnag.init(this);

		/* Must have this so activity rerouting works. */
		settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		gson = new Gson();

		Foreground.init(this);

		/* Connectivity monitoring */
		NetworkManager.getInstance().initialize();

		/* Inject configuration */
		openContainer(StringManager.getString(R.string.tag_manager_container_id));

		/* Turn on branch */
		Branch.getAutoInstance(this);

		/* Turn on parse */
		Parse.initialize(this
			, StringManager.getString(R.string.parse_app_id)        // application id
			, StringManager.getString(R.string.parse_client_key));  // client key
		Parse.setLogLevel(Constants.LOG_LEVEL);
		ParseInstallation.getCurrentInstallation().saveInBackground();

		AsyncTask.execute(() -> {

			/* Configure realm */
			RealmConfiguration config = new RealmConfiguration.Builder(Patchr.applicationContext)
				.name("patchr.realm")
				.deleteRealmIfMigrationNeeded()
				.build();

			Realm.setDefaultConfiguration(config);

			/* Make sure setting defaults are initialized */
			PreferenceManager.setDefaultValues(this, R.xml.preferences_dev, true);
			PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

			/* Make sure unique id is initialized */
			initializeInstallInfo();

			/* Turn on segement to gather user data */
			Analytics analytics = new Analytics.Builder(this, "81Q9wmANTOA6PLVlipPvSRHw97SJBENF").build();
			Analytics.setSingletonInstance(analytics);

			/* Creative SDK needs the app context */
			AdobeCSDKFoundation.initializeCSDKFoundation(getApplicationContext());

			/* Turn on facebook */
			FacebookSdk.sdkInitialize(this);

			/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
			snapshotPreferences();

			/* Establish device memory class */
			Logger.i(this, "Device memory class: " + String.valueOf(Utils.maxMemoryMB()));

			/* Set the folder used by image chooser for all files */
			BChooserPreferences preferences = new BChooserPreferences(this);
			preferences.setFolderName("Pictures/Patchr");

			/* Stetho */
			if (BuildConfig.DEBUG) {
				Logger.d(this, "Debug build, stetho initialized");
				Stetho.initialize(
					Stetho.newInitializerBuilder(this)
						.enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
						.enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
						.build());
			}

			/* Job queue */
			Configuration.Builder builder = new Configuration.Builder(this)
				.minConsumerCount(1)
				.maxConsumerCount(3)
				.loadFactor(3)
				.consumerKeepAlive(120);

			jobManager = new JobManager(builder.build());

			/* Warmup media manager */
			MediaManager.warmup();

			/* Must come after managers are initialized */
			UserManager.shared().loginAuto();

			Logger.d(this, "Finished app initialization");
		});
	}

	public void snapshotPreferences() {
		prefEnableDev = settings.getBoolean(Preference.ENABLE_DEV, false);
	}

	@Override public String getClientID() {
		return StringManager.getString(R.string.creative_sdk_client_id);
	}

	@Override public String getClientSecret() {
		return StringManager.getString(R.string.creative_sdk_client_key);
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

				containerHolder.setContainerAvailableListener((containerHolder1, s) -> activateContainer(containerHolder1));
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

			/* Generate and store a unique number for this device */
			uniqueId = UUID.randomUUID().toString();
			uniqueType = Constants.INSTALL_TYPE_RANDOM;
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
