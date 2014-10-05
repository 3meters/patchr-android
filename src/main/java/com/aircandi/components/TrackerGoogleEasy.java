package com.aircandi.components;

import android.app.Activity;
import android.app.Fragment;

import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.utilities.Reporting;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.Logger.LogLevel;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

/*
 * Tracker strategy
 * 
 * - Every activity is a page view when initialized.
 * - Page views and events info is dispatched to google service automatically
 * by EasyTracker.
 * 
 * - Select events are tracked
 * - Insert, update, delete entity
 * - user clicks refresh
 * - Insert, update user
 * - Comment created
 * - user signin, signout
 * 
 * More candidates
 * - Preferences modified
 */

@SuppressWarnings("ucd")
public class TrackerGoogleEasy extends TrackerBase {

	public static final int GA_DISPATCH_PERIOD       = 60;    // seconds																				// Dispatch period in seconds.
	public static final int GA_DISPATCH_PERIOD_DEBUG = 10;    // seconds																				// Dispatch period in seconds.

	@Override
	public void activityStart(Activity activity) {
		try {
			EasyTracker.getInstance(Patch.applicationContext).activityStart(activity);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@Override
	public void activityStop(Activity activity) {
		try {
			EasyTracker.getInstance(Patch.applicationContext).activityStop(activity);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@Override
	public void sendEvent(String category, String action, String target, long value) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			EasyTracker.getInstance(Patch.applicationContext).send(MapBuilder.createEvent(category, action, target, value).build());
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@Override
	public void sendTiming(String category, Long timing, String name, String label) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			EasyTracker.getInstance(Patch.applicationContext).send(MapBuilder.createTiming(category, timing, name, label).build());
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@Override
	public void sendException(Exception exception) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			EasyTracker.getInstance(Patch.applicationContext).send(
					MapBuilder.createException(new StandardExceptionParser(Patch.applicationContext, null)
							.getDescription(Thread.currentThread().getName(), exception), false)
					          .build()
			);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@Override
	public void fragmentStart(Fragment fragment) {
		try {
			/*
			 * Screen name as set will be included in all subsequent sends.
			 */
			EasyTracker.getInstance(Patch.applicationContext).set(Fields.SCREEN_NAME, ((Object) fragment).getClass().getSimpleName());
			EasyTracker.getInstance(Patch.applicationContext).send(MapBuilder.createAppView().build());
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void applicationStart() {
		Boolean enabled = Patch.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false);
		Patch.analytics.setDryRun(enabled);
		Patch.analytics.getLogger().setLogLevel(Patch.DEBUG ? LogLevel.VERBOSE : LogLevel.WARNING);
		GAServiceManager.getInstance().setLocalDispatchPeriod(Patch.DEBUG ? GA_DISPATCH_PERIOD_DEBUG : GA_DISPATCH_PERIOD);
	}

	@SuppressWarnings("unused")
	private void startNewSession() {
		try {
			EasyTracker.getInstance(Patch.applicationContext).send(MapBuilder
							.createEvent(TrackerCategory.SYSTEM, "session_start", null, null)
							.set(Fields.SESSION_CONTROL, "start")
							.build()
			);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@SuppressWarnings("unused")
	private void stopSession() {
		try {
			EasyTracker.getInstance(Patch.applicationContext).send(MapBuilder
							.createEvent(TrackerCategory.SYSTEM, "session_end", null, null)
							.set(Fields.SESSION_CONTROL, "end")
							.build()
			);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	@Override
	public void enableDeveloper(Boolean enable) {
		Patch.analytics.setDryRun(enable);
	}
}
