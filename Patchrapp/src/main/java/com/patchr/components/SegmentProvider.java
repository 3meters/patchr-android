package com.patchr.components;

import com.patchr.Patchr;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

public class SegmentProvider implements AnalyticsProvider {

	public AnalyticsProvider init() {
		Analytics analytics = new Analytics.Builder(Patchr.applicationContext, "81Q9wmANTOA6PLVlipPvSRHw97SJBENF").build();
		Analytics.setSingletonInstance(analytics);
		return this;
	}

	@Override public void updateUser(String userId, String userName, String userAuth) {
		if (userId != null) {
			Analytics.with(Patchr.applicationContext).alias(userId);
			Analytics.with(Patchr.applicationContext).identify(userId, new Traits().putName(userName).putEmail(userAuth), null);
		}
		else {
			Analytics.with(Patchr.applicationContext).flush();  // Send queued events before clearing user id
			Analytics.with(Patchr.applicationContext).reset();  // Clear user id currently used by segmentio
		}
	}

	public void track(String category, String event) {
		Analytics.with(Patchr.applicationContext).track(event, new Properties()
			.putValue("category", category));
	}

	@Override public void track(String category, String event, String key, Object value) {
		Properties props = new Properties();
		props.putValue(key, value);
		props.putValue("category", category);
		Analytics.with(Patchr.applicationContext).track(event, props);
	}

	@Override public void screen(String category, String name) {
		Analytics.with(Patchr.applicationContext).screen(category, name);
	}
}
