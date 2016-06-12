package com.patchr.events;

import com.patchr.components.ActivityRecognitionManager.ActivityState;

@SuppressWarnings("ucd")
public class ActivityStateEvent {

	public final ActivityState activityState;

	public ActivityStateEvent(ActivityState activityState) {
		this.activityState = activityState;
	}
}
