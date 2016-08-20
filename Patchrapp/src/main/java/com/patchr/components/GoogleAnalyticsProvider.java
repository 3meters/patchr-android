package com.patchr.components;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.patchr.Patchr;
import com.patchr.R;

public class GoogleAnalyticsProvider implements AnalyticsProvider {

	private Tracker tracker;

	public AnalyticsProvider init() {
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(Patchr.applicationContext);
		tracker = analytics.newTracker(R.xml.analytics);
		return this;
	}

	@Override public void updateUser(String userId, String userName, String userAuth) {
		if (userId != null) {
			tracker.set("&uid", userId);
		}
		else {
			tracker.set("&uid", null);
		}
	}

	public void track(String category, String event) {
		tracker.send(new HitBuilders.EventBuilder()
			.setCategory(category)
			.setAction(event)
			.build());
	}

	@Override public void track(String category, String event, String key, Object value) {
		tracker.send(new HitBuilders.EventBuilder()
			.setCategory(category)
			.setAction(event)
			.setLabel(key)
			.setValue((long) value)
			.build());
	}

	@Override public void screen(String category, String name) {
		tracker.setScreenName(name);
		tracker.send(new HitBuilders.ScreenViewBuilder().build());
	}
}
