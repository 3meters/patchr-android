/**
 * Copyright 2015 Steve Liles
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.patchr.components;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Usage:
 *
 * 1. Get the Foreground Singleton, passing a Context or Application object unless you
 * are sure that the Singleton has definitely already been initialised elsewhere.
 *
 * 2.a) Perform a direct, synchronous check: Foreground.isForeground() / .isBackground()
 *
 * or
 *
 * 2.b) Register to be notified (useful in Service or other non-UI components):
 *
 *   Foreground.Listener myListener = new Foreground.Listener(){
 *       public void onBecameForeground(){
 *           // ... whatever you want to do
 *       }
 *       public void onBecameBackground(){
 *           // ... whatever you want to do
 *       }
 *   }
 *
 *   public void onCreate(){
 *      super.onCreate();
 *      Foreground.get(getApplication()).addListener(listener);
 *   }
 *
 *   public void onDestroy(){
 *      super.onCreate();
 *      Foreground.get(getApplication()).removeListener(listener);
 *   }
 */
public class Foreground implements Application.ActivityLifecycleCallbacks {

	public static final String TAG         = Foreground.class.getName();
	public static final long   CHECK_DELAY = 2000;

	public interface Listener {
		void onBecameForeground();

		void onBecameBackground();
	}

	public interface Binding {
		void unbind();
	}

	private interface Callback {
		void invoke(Listener listener);
	}

	private static class Listeners {
		private List<WeakReference<Listener>> listeners = new CopyOnWriteArrayList<>();

		public Binding add(Listener listener) {
			final WeakReference<Listener> wr = new WeakReference<>(listener);
			listeners.add(wr);
			return () -> listeners.remove(wr);
		}

		public void each(Callback callback) {
			for (Iterator<WeakReference<Listener>> it = listeners.iterator(); it.hasNext(); ) {
				try {
					WeakReference<Listener> wr = it.next();
					Listener l = wr.get();
					if (l != null)
						callback.invoke(l);
					else
						it.remove();
				}
				catch (Exception exc) {
					Log.e(TAG, "Listener threw exception!", exc);
				}
			}
		}
	}

	private static Callback becameForeground = Listener::onBecameForeground;

	private static Callback becameBackground = Listener::onBecameBackground;

	private static Foreground instance;

	private boolean  foreground;
	private Activity current;
	private Listeners listeners = new Listeners();
	private Handler   handler   = new Handler();
	private Runnable check;

	public static Foreground init(Application application) {
		if (instance == null) {
			instance = new Foreground();
			application.registerActivityLifecycleCallbacks(instance);
		}
		return instance;
	}

	public static Foreground get(Application application) {
		if (instance == null) {
			init(application);
		}
		return instance;
	}

	public static Foreground get() {
		if (instance == null) {
			throw new IllegalStateException("Foreground is not initialised - first invocation must use parameterised init/get");
		}
		return instance;
	}

	public boolean isForeground() {
		return foreground;
	}

	public boolean isBackground() {
		return !foreground;
	}

	public Binding addListener(Listener listener) {
		return listeners.add(listener);
	}

	@Override
	public void onActivityResumed(Activity activity) {}

	@Override
	public void onActivityPaused(Activity activity) {
		// if we're changing configurations we aren't going background so
		// no need to schedule the check
		if (!activity.isChangingConfigurations()) {
			// don't prevent activity being gc'd
			final WeakReference<Activity> ref = new WeakReference<>(activity);
			handler.postDelayed(check = () -> onActivityCeased(ref.get()), CHECK_DELAY);
		}
	}

	@Override
	public void onActivityStarted(Activity activity) {
		current = activity;
		// remove any scheduled checks since we're starting another activity
		// we're definitely not going background
		if (check != null) {
			handler.removeCallbacks(check);
		}

		// check if we're becoming foreground and notify listeners
		if (!foreground && (activity != null && !activity.isChangingConfigurations())) {
			foreground = true;
			Logger.d(this, "Move to foreground");
			listeners.each(becameForeground);
		}
	}

	@Override
	public void onActivityStopped(Activity activity) {
		if (check != null) {
			handler.removeCallbacks(check);
		}
		onActivityCeased(activity);
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

	@Override
	public void onActivityDestroyed(Activity activity) {}

	private void onActivityCeased(Activity activity) {
		if (foreground) {
			if ((activity == current) && (activity != null && !activity.isChangingConfigurations())) {
				foreground = false;
				Logger.d(this, "Move to background");
				listeners.each(becameBackground);
			}
		}
	}
}