package com.patchr.components;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.events.ActivityStateEvent;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class ActivityRecognitionManager implements
                                        GoogleApiClient.ConnectionCallbacks,
                                        GoogleApiClient.OnConnectionFailedListener {

	protected GoogleApiClient mGoogleApiClient;
	protected PendingIntent   mActivityRecognitionPendingIntent;

	private Integer mActivityTypeCurrent  = DetectedActivity.STILL;
	private Integer mActivityTypePrevious = DetectedActivity.STILL;
	private Integer mActivityTypeConfidence;

	private Long          mActivityStateStart;
	private ActivityState mActivityStateCandidate;

	private   ActivityState mActivityStateCurrent   = ActivityState.STILL;
	private   Integer       mActivityStateThreshold = Constants.TIME_TEN_SECONDS;
	protected Boolean       mInProgress             = false;
	protected Integer       mDetectionInterval      = Constants.TIME_THIRTY_SECONDS;
	protected DetectionMode mDetectionMode          = DetectionMode.MOVING;

	private static final int     REQUEST_RESOLVE_ERROR = 1001;
	private              boolean mResolvingError       = false;

	private ActivityRecognitionManager() {
		mActivityStateStart = DateTime.nowDate().getTime();
	}

	private static class ActivityRecognitionManagerHolder {
		public static final ActivityRecognitionManager instance = new ActivityRecognitionManager();
	}

	public static ActivityRecognitionManager getInstance() {
		return ActivityRecognitionManagerHolder.instance;
	}

	public void initialize(Context applicationContext) {
		Logger.i(this, "Initializing the ActivityRecognitionManager");
		mGoogleApiClient = new GoogleApiClient.Builder(applicationContext)
				.addApi(ActivityRecognition.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();
		Intent intent = new Intent(applicationContext, ActivityRecognitionService.class);
		mActivityRecognitionPendingIntent = PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		startUpdates(Constants.TIME_ONE_MINUTE, Constants.TIME_TWO_MINUTES);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onConnected(Bundle extras) {

		/* Start updates */
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient
				, mDetectionInterval
				, mActivityRecognitionPendingIntent);

		mInProgress = false;
		mGoogleApiClient.disconnect();
	}

	@Override
	public void onConnectionSuspended(int i) {
		mInProgress = false;
		mGoogleApiClient = null;
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects. If the error has a
		 * resolution, try sending an Intent to start a Google Play services activity
		 * that can resolve error.
		 */
		mInProgress = false;
		if (!mResolvingError) {
			if (result.hasResolution()) {
				try {
					mResolvingError = true;
					result.startResolutionForResult((Activity) Patchr.applicationContext, REQUEST_RESOLVE_ERROR);
				}
				catch (IntentSender.SendIntentException e) {
				/* There was an error with the resolution intent. Try again. */
					Reporting.logException(e);
					mGoogleApiClient.connect();
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void setActivityType(Integer activityType, Integer confidence) {

		Logger.v(this, getNameFromType(activityType) + ": " + confidence);

		if (mGoogleApiClient == null) return;

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
				UI.showToastNotification("Activity recognition: slowing updates", Toast.LENGTH_SHORT);
			}
		}
		else if ((mActivityStateCurrent == ActivityState.DEPARTING || mActivityStateCurrent == ActivityState.MOVING)
				&& mDetectionMode == DetectionMode.STILL) {
			startUpdates(Constants.TIME_ONE_MINUTE, Constants.TIME_TWO_MINUTES);    // Transition to still takes more time
			mDetectionMode = DetectionMode.MOVING;
			if (Patchr.getInstance().getPrefEnableDev()) {
				MediaManager.playSound(MediaManager.SOUND_ACTIVITY_CHANGE, 1.0f, 3);
				UI.showToastNotification("Activity recognition: faster updates", Toast.LENGTH_SHORT);
			}
		}

		if (Patchr.getInstance().getPrefEnableDev()) {
			UI.showToastNotification("Activity state: " + mActivityStateCurrent.name(), Toast.LENGTH_SHORT);
		}

		/* Broadcast current activity state */

		Dispatcher.getInstance().post(new ActivityStateEvent(mActivityStateCurrent));
	}

	public void startUpdates(Integer detectionInterval, Integer stateThreshold) {
		if (!mInProgress && !mResolvingError) {
			mDetectionInterval = detectionInterval;
			mActivityStateThreshold = stateThreshold;
			mInProgress = true;
			mGoogleApiClient.connect();
		}
	}

	@NonNull
	public Boolean isMoving(Integer activityType) {
		return (activityType != DetectedActivity.STILL);
	}

	@NonNull
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

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

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
