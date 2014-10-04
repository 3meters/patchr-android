package com.aircandi.events;

import com.aircandi.components.ActivityRecognitionManager.ActivityState;

@SuppressWarnings("ucd")
public class ActivityStateEvent {

	public final ActivityState activityState;

	public ActivityStateEvent(ActivityState activityState) {
		this.activityState = activityState;
	}
}
