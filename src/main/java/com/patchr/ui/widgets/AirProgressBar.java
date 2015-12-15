package com.patchr.ui.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import com.patchr.components.AnimationManager;
import com.patchr.ui.components.SimpleAnimationListener;

/**
 * AirProgressBar implements a ProgressBar that waits a minimum time to be
 * dismissed before showing. Once visible, the progress bar will be visible for
 * a minimum amount of time to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount)
 */
public class AirProgressBar extends ProgressBar {

	private static final int MIN_SHOW_TIME = 500; // ms
	private static final int MIN_DELAY     = 1000; // ms

	private long    mStartTime  = -1;
	private boolean mPostedHide = false;
	private boolean mPostedShow = false;
	private boolean mDismissed  = false;

	private ObjectAnimator mFadeInAnim  = ObjectAnimator.ofFloat(null, "alpha", 1f);
	private ObjectAnimator mFadeOutAnim = ObjectAnimator.ofFloat(null, "alpha", 0f);

	private final Runnable mDelayedHide = new Runnable() {

		@Override
		public void run() {
			mPostedHide = false;
			mStartTime = -1;
			fadeOut();
		}
	};

	private final Runnable mDelayedShow = new Runnable() {

		@Override
		public void run() {
			mPostedShow = false;
			if (!mDismissed) {
				mStartTime = System.currentTimeMillis();
				setVisibility(View.VISIBLE);
			}
		}
	};

	public AirProgressBar(Context context) {
		this(context, null);
	}

	public AirProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		removeCallbacks();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		removeCallbacks();
	}

	private void removeCallbacks() {
		removeCallbacks(mDelayedHide);
		removeCallbacks(mDelayedShow);
	}

	/**
	 * Hide the progress view if it is visible. The progress view will not be
	 * hidden until it has been shown for at least a minimum show time. If the
	 * progress view was not yet visible, cancels showing the progress view.
	 */
	public void hide() {
		mDismissed = true;
		removeCallbacks(mDelayedShow);
		long diff = System.currentTimeMillis() - mStartTime;
		if (diff >= MIN_SHOW_TIME || mStartTime == -1) {
			// The progress spinner has been shown long enough
			// OR was not shown yet. If it wasn't shown yet,
			// it will just never be shown.
			fadeOut();
		}
		else {
			// The progress spinner is shown, but not long enough,
			// so put a delayed message in to hide it when its been
			// shown long enough.
			if (!mPostedHide) {
				postDelayed(mDelayedHide, MIN_SHOW_TIME - diff);
				mPostedHide = true;
			}
		}
	}

	/**
	 * Show the progress view after waiting for a minimum delay. If
	 * during that time, hide() is called, the view is never made visible.
	 */
	public void show() {
		show(MIN_DELAY);
	}

	/**
	 * Show the progress view after waiting for a minimum delay. If
	 * during that time, hide() is called, the view is never made visible.
	 */
	public void show(int minDelay) {
		// Reset the start time.
		mStartTime = -1;
		mDismissed = false;
		removeCallbacks(mDelayedHide);
		if (!mPostedShow) {
			postDelayed(mDelayedShow, minDelay);
			mPostedShow = true;
		}
	}

	public void fadeIn(Integer delay){
		mFadeInAnim.setTarget(this);
		setAlpha(0f);
		setVisibility(View.VISIBLE);
		mFadeInAnim.setDuration(AnimationManager.DURATION_LONG);
		mFadeInAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationStart(@NonNull Animator animator) {
				animator.removeAllListeners();
			}
		});
		if (delay > 0) {
			mFadeInAnim.setStartDelay(delay);
		}
		mFadeInAnim.start();
	}

	public void fadeOut(){
		mFadeOutAnim.setTarget(this);
		mFadeOutAnim.setDuration(AnimationManager.DURATION_LONG);
		mFadeOutAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(@NonNull Animator animator) {
				setVisibility(View.GONE);
				setAlpha(1f);
				animator.removeAllListeners();
			}
		});
		mFadeOutAnim.start();
	}
}
