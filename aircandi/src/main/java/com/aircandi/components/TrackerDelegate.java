package com.aircandi.components;

import android.app.Activity;
import android.app.Fragment;

@SuppressWarnings("ucd")
public interface TrackerDelegate {

	public void sendEvent(String category, String action, String target, long value);

	public void sendTiming(String category, Long timing, String name, String label);

	public void sendException(Exception exception);

	public void sendError(String category, String name);

	public void fragmentStart(Fragment fragment);

	public void activityStart(Activity activity);

	public void activityStop(Activity activity);

	public void applicationStart();

	public void enableDeveloper(Boolean enable);

}