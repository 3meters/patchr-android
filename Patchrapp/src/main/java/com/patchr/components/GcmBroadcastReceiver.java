package com.patchr.components;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */
@SuppressWarnings("ucd")
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	/*
	 * NOTE: Broadcast receivers like the gcm won't get called if the app
	 * was force closed by the user in Settings->Apps. Will get called if the
	 * app was closed by any other method.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		Logger.d(this, "onReceive called");

		/* Explicitly specify that GcmIntentService will handle the intent. */
		ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());

		/* Start the service, keeping the device awake while it is launching. */
		startWakefulService(context, (intent.setComponent(comp)));
		setResultCode(Activity.RESULT_OK);
	}
}
