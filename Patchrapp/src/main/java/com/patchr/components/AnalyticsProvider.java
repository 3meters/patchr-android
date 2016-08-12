package com.patchr.components;

public interface AnalyticsProvider {

	public AnalyticsProvider init();

	public void updateUser(String userId, String userName, String userAuth);

	public void track(String category, String event);

	public void track(String category, String event, String key, Object value);

	public void screen(String category, String name);
}
