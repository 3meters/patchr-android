package com.aircandi.components;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.events.ActivityStateEvent;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

@SuppressWarnings("ucd")
public class ActivityRecognitionManager implements
                                        GooglePlayServicesClient.ConnectionCallbacks,
                                        GooglePlayServicesClient.OnConnectionFailedListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	protected ActivityRecognitionClient mActivityRecognitionClient;
	protected PendingIntent             mActivityRecognitionPendingIntent;

	private Integer mActivityTypeCurrent  = DetectedActivity.STILL;
	private Integer mActivityTypePrevious = DetectedActivity.STILL;
	private Integer mActivityTypeConfidence;

	private Long          mActivityStateStart;
	private ActivityState mActivityStateCandidate;

	private ActivityState mActivityStateCurrent   = ActivityState.STILL;
	private Integer       mActivityStateThreshold = Constants.TIME_TEN_SECONDS;

	protected Boolean       mInProgress        = false;
	protected Integer       mDetectionInterval = Constants.TIME_THIRTY_SECONDS;
	protected DetectionMode mDetectionMode     = DetectionMode.MOVING;

	private ActivityRecognitionManager() {
	}

	private static class ActivityRecognitionManagerHolder {
		public static final ActivityRecognitionManager instance = new ActivityRecognitionManager();
	}

	public static ActivityRecognitionManager getInstance() {
		return ActivityRecognitionManagerHolder.instance;
	}

	public void initialize(Context applicationContext) {
		Logger.i(this, "Initializing the ActivityRecognitionManager");
		mActivityRecognitionClient = new ActivityRecognitionClient(applicationContext, this, this);
		Intent intent = new Intent(applicationContext, ActivityRecognitionService.class);
		mActivityRecognitionPendingIntent = PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		startUpdates(Constants.TIME_ONE_MINUTE, Constants.TIME_TWO_MINUTES);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onConnected(Bundle extras) {
		mActivityRecognitionClient.requestActivityUpdates(mDetectionInterval, mActivityRecognitionPendingIntent);
		mInProgress = false;
		mActivityRecognitionClient.disconnect();
	}

	@Override
	public void onDisconnected() {
		mInProgress = false;
		mActivityRecognitionClient = null;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error has a resolution, try sending an Intent
		 * to start a Google Play services activity that can resolve error.
		 */
		mInProgress = false;
		if (connectionResult.hasResolution()) {
			try {
				/* Start an Activity that tries to resolve the error */
				connectionResult.startResolutionForResult((Activity) Patchr.applicationContext
						, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			}
			catch (IntentSender.SendIntentException e) {
				Reporting.logException(e);
			}
		}
		else {
			/*
			 * If no resolution is available, display a dialog to the
			 * user with the error.
			 */
			AndroidManager.showPlayServicesErrorDialog(connectionResult.getErrorCode(), Patchr.getInstance().getCurrentActivity());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void setActivityType(Integer activityType, Integer confidence) {

		Logger.v(this, getNameFromType(activityType) + ": " + confidence);

		if (mActivityRecognitionClient == null) return;

		if (activityType == DetectedActivity.UNKNOWN
				|| activityType == DetectedActivity.TILTING) return;

		mActivityTypeConfidence = confidence;
		mActivityTypePrevious = mActivityTypeCurrent;
		mActivityTypeCurrent = activityType;
		
		/* Determine current activity state */

		if (!isMoving(mActivityTypePrevious).equals(isMoving(mActivityTypeCurrent))) {
			mActivityStateStart = DateTime.nowDate().getTime();
			mActivityStateCandidate = isMoving(mActivityTypeCurrent) ? ActivityState.MOVING : ActivityState.STILL;
		}
		else {
			if (mActivityStateCandidate != null && (DateTime.nowDate().getTime() - mActivityStateStart) >= mActivityStateThreshold) {
				mActivityStateCurrent = (mActivityStateCandidate == ActivityState.MOVING)
				                        ? ActivityState.DEPARTING
				                        : ActivityState.ARRIVING;
				mActivityStateCandidate = null;
			}
			else {
				mActivityStateCurrent = (mActivityStateCurrent == ActivityState.DEPARTING)
				                        ? ActivityState.MOVING
				                        : (mActivityStateCurrent == ActivityState.ARRIVING)
				                          ? ActivityState.STILL
				                          : mActivityStateCurrent;
			}
		}
		
		/* Throttle up/down as needed */

		if ((mActivityStateCurrent == ActivityState.ARRIVING || mActivityStateCurrent == ActivityState.STILL)
				&& mDetectionMode == DetectionMode.MOVING) {
			startUpdates(Constants.TIME_TWO_MINUTES, Constants.TIME_TEN_SECONDS);  // Transition to moving is fast
			mDetectionMode = DetectionMode.STILL;
			if (Patchr.getInstance().getPrefEnableDev()) {
				MediaManager.playSound(MediaManager.SOUND_ACTIVITY_CHANGE, 1.0f, 1);
				UI.showToastNotification("Activity recognition: throttling down", Toast.LENGTH_SHORT);
			}
		}
		else if ((mActivityStateCurrent == ActivityState.DEPARTING || mActivityStateCurrent == ActivityState.MOVING)
				&& mDetectionMode == DetectionMode.STILL) {
			startUpdates(Constants.TIME_ONE_MINUTE, Constants.TIME_TWO_MINUTES);    // Transition to still takes more time
			mDetectionMode = DetectionMode.MOVING;
			if (Patchr.getInstance().getPrefEnableDev()) {
				MediaManager.playSound(MediaManager.SOUND_ACTIVITY_CHANGE, 1.0f, 3);
				UI.showToastNotification("Activity recognition: throttling up", Toast.LENGTH_SHORT);
			}
		}

		if (Patchr.getInstance().getPrefEnableDev()) {
			UI.showToastNotification("Activity state: " + mActivityStateCurrent.name(), Toast.LENGTH_SHORT);
		}

		/* Broadcast current activity state */

		BusProvider.getInstance().post(new ActivityStateEvent(mActivityStateCurrent));
	}

	public void startUpdates(Integer detectionInterval, Integer stateThreshold) {
		if (!mInProgress) {
			mDetectionInterval = detectionInterval;
			mActivityStateThreshold = stateThreshold;
			mInProgress = true;
			mActivityRecognitionClient.connect();
		}
	}

	public Boolean isMoving(Integer activityType) {
		return (activityType != DetectedActivity.STILL);
	}

	public String getNameFromType(int activityType) {
		switch (activityType) {
			case DetectedActivity.IN_VEHICLE:
				return "Driving";
			case DetectedActivity.ON_BICYCLE:
				return "Bicycle";
			case DetectedActivity.ON_FOOT:
				return "Walking";
			case DetectedActivity.STILL:
				return "Still";
			case DetectedActivity.UNKNOWN:
				return "Unknown";
			case DetectedActivity.TILTING:
				return "Tilting";
		}
		return "Unknown";
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Integer getActivityType() {
		return mActivityTypeCurrent;
	}

	public Long getActivityDuration() {
		return DateTime.nowDate().getTime() - mActivityStateStart;
	}

	public Integer getConfidence() {
		return mActivityTypeConfidence;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public Integer getActivityStateThreshold() {
		return mActivityStateThreshold;
	}

	public void setActivityStateThreshold(Integer activityStateThreshold) {
		mActivityStateThreshold = activityStateThreshold;
	}

	public enum ActivityState {
		STILL,
		DEPARTING,
		MOVING,
		ARRIVING
	}

	public enum DetectionMode {
		MOVING,
		STILL
	}
}
