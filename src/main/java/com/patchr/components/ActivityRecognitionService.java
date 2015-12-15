package com.patchr.components;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionService extends IntentService {

	@SuppressWarnings("ucd")
	public ActivityRecognitionService(String name) {
		super(name);
	}

	public ActivityRecognitionService() {
		super("ActivityRecognitionService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (ActivityRecognitionResult.hasResult(intent)) {

			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
			DetectedActivity mostProbableActivity = result.getMostProbableActivity();

			/* Pass to activity recognition manager which in turn may trigger a bus event */
			ActivityRecognitionManager.getInstance().setActivityType(
					mostProbableActivity.getType() /* Get an integer describing the type of activity */,
					mostProbableActivity.getConfidence() /* Get the probability that this activity is the user's actual activity */);
		}
	}
}