// $codepro.audit.disable fileComment
package com.patchr;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.adobe.creativesdk.foundation.AdobeCSDKFoundation;
import com.adobe.creativesdk.foundation.auth.IAdobeAuthClientCredentials;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.facebook.FacebookSdk;
import com.facebook.stetho.Stetho;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;
import com.google.gson.Gson;
import com.kbeanie.imagechooser.api.BChooserPreferences;
import com.onesignal.OneSignal;
import com.patchr.components.ContainerManager;
import com.patchr.components.Foreground;
import com.patchr.components.GoogleAnalyticsProvider;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.NotificationManager;
import com.patchr.components.ReportingManager;
import com.patchr.components.Stopwatch;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.enums.Preference;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Utils;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.branch.referral.Branch;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Patchr extends Application implements IAdobeAuthClientCredentials {

	private static Patchr instance;
	public static  Intent sendIntent;  // Used when we are started to handle a send intent

	public static Context           applicationContext;
	public static SharedPreferences settings;
	public static JobManager        jobManager;
	public static boolean           updateRequired;

	public static Handler   mainThread = new Handler(Looper.getMainLooper());
	public static Stopwatch stopwatch1 = new Stopwatch();
	public static Stopwatch stopwatch2 = new Stopwatch();
	public static Gson      gson       = new Gson();

	public        Boolean prefEnableDev;
	public static String  installId;
	public static Long    installDate;
	public static String  installType;

	public static Patchr getInstance() {
		return instance;
	}

	@Override public void onCreate() {
		/*
		 * Application starts for all basic cases but also when not running and
		 * a broadcast receiver is activated.
		 */
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

		/* Turn on crash reporting and analytics */
		ReportingManager.init(new GoogleAnalyticsProvider().init());

		/* Must have this so activity rerouting works. */
		settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);

		Foreground.init(this);

		/* Connectivity monitoring */
		NetworkManager.getInstance().initialize();

		/* Inject configuration */
		loadContainer(StringManager.getString(R.string.tag_manager_container_id));

		/* Turn on branch */
		Branch.getAutoInstance(this);

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

			/* Turn on Onesignal */
			OneSignal.startInit(this).init();
			OneSignal.idsAvailable(NotificationManager.getInstance());

			Logger.d(this, "Finished app initialization");
		});
	}

	private void initializeInstallInfo() {

		installId = settings.getString(StringManager.getString(R.string.setting_unique_id), null);
		installDate = settings.getLong(StringManager.getString(R.string.setting_unique_id_date), 0);
		installType = settings.getString(StringManager.getString(R.string.setting_unique_id_type), null);

		if (installId == null || installType == null) {

			/* Generate and store a unique number for this device */
			installId = UUID.randomUUID().toString();
			installType = Constants.INSTALL_TYPE_RANDOM;
			installDate = DateTime.nowDate().getTime();

			settings.edit().putString(StringManager.getString(R.string.setting_unique_id_type), installType);
			settings.edit().putString(StringManager.getString(R.string.setting_unique_id), installId);
			settings.edit().putLong(StringManager.getString(R.string.setting_unique_id_date), installDate);
			settings.edit().apply();
		}
	}

	public void loadContainer(String containerId) {
		/*
		 * TagManager always starts by loading the defaults bundled with app. Next,
		 * it looks for the latest version of the container on disk (which it saved when pulling
		 * a fresh container version from the network). If none or stale, it tries to retrieve
		 * the current version from the network.
		 */
		TagManager tagManager = TagManager.getInstance(this);
		tagManager.setVerboseLoggingEnabled(BuildConfig.DEBUG);

		PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(containerId, R.raw.gtm_default_container);
		pending.setResultCallback(containerHolder -> {
			/*
			 * The onResult method will be called as soon as one of the following happens:
			 *  1. a saved container is loaded
			 *  2. if there is no saved container, a network container is loaded
			 *  3. the 2-second timeout occurs
			 */
			if (!containerHolder.getStatus().isSuccess()) {
				Logger.e(this, "Container refresh failed");
				return;
			}
			ContainerManager.getInstance().setContainerHolder(containerHolder);
		}, 2, TimeUnit.SECONDS);
	}

	public void snapshotPreferences() {
		prefEnableDev = settings.getBoolean(Preference.ENABLE_DEV, false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@Override public String getClientID() {
		return StringManager.getString(R.string.creative_sdk_client_id);
	}

	@Override public String getClientSecret() {
		return StringManager.getString(R.string.creative_sdk_client_key);
	}

	public synchronized String getinstallId() {
		if (installId == null) {
			initializeInstallInfo();
		}
		return installId;
	}
}
