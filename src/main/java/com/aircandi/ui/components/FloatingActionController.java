package com.aircandi.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.StringManager;
import com.aircandi.utilities.UI;

public class FloatingActionController {

	private View mView;
	private Boolean mEnabled = true;
	private Boolean mLocked  = false;
	private Boolean mHidden  = false;
	private Boolean mSliding = false;
	private ImageView mFabIcon;
	private ObjectAnimator mSlideInAnim  = ObjectAnimator.ofFloat(null, "translationY", 0);
	private ObjectAnimator mSlideOutAnim = ObjectAnimator.ofFloat(null, "translationY", 0);

	public FloatingActionController() {}

	public FloatingActionController(View view) {
		mView = view;
		if (view != null) {
			mFabIcon = (ImageView) view.findViewById(R.id.fab_image);
		}
	}

	public void fadeIn() {
		/*
		 * Skips if already visible and full opacity. Always ensures
		 * default position.
		 */
		if (mView == null || (mView.getVisibility() == View.VISIBLE && mView.getAlpha() == 1f))
			return;

		mView.setAlpha(0f);
		mView.setTranslationY(0f);
		mView.setVisibility(View.VISIBLE);
		ObjectAnimator anim = ObjectAnimator.ofFloat(mView, "alpha", 1f);
		anim.setDuration(AnimationManager.DURATION_MEDIUM);
		anim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationStart(Animator animator) {
				mView.setClickable(true);
				animator.removeAllListeners();
			}
		});
		anim.start();
	}

	public void fadeOut() {
			/*
			 * Skips if already gone and fully transparent. Always ensures
			 * default position.
			 */
		if (mView == null || (mView.getVisibility() == View.GONE && mView.getAlpha() == 0f))
			return;

		ObjectAnimator anim = ObjectAnimator.ofFloat(mView, "alpha", 0f);
		mView.setTranslationY(0f);
		anim.setDuration(AnimationManager.DURATION_MEDIUM);
		anim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animator animator) {
				mView.setClickable(false);
				mView.setVisibility(View.GONE);
				animator.removeAllListeners();
			}
		});
		anim.start();
	}

	public void slideOut(@NonNull Integer duration) {
		/*
		 * Skips if locked, sliding or already hidden.
		 */
		if (mLocked || mSliding || mHidden || mView == null) return;

		mSliding = true;
		mSlideOutAnim.setTarget(mView);
		mSlideOutAnim.setFloatValues(mView.getHeight() + UI.getRawPixelsForDisplayPixels(30f));
		mSlideOutAnim.setDuration(duration);
		mSlideOutAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animator animator) {
				mView.setClickable(false);
				animator.removeAllListeners();
				mHidden = true;
				mSliding = false;
			}
		});
		mSlideOutAnim.start();
	}

	public void slideIn(@NonNull Integer duration, @NonNull Integer delay) {
		/*
		 * Skips if locked, sliding or not hidden.
		 */
		if (mLocked || mSliding || !mHidden) return;

		mSliding = true;
		mSlideInAnim.setTarget(mView);
		mSlideInAnim.setDuration(duration);
		mSlideInAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animator animator) {
				mView.setClickable(true);
				animator.removeAllListeners();
				mHidden = false;
				mSliding = false;
			}
		});

		mSlideInAnim.setStartDelay(delay);
		mSlideInAnim.start();
	}

	public View getView() {
		return mView;
	}

	public Boolean isEnabled() {
		return mEnabled;
	}

	public Boolean isLocked() {
		return mLocked;
	}

	public FloatingActionController setEnabled(Boolean enabled) {
		if (!enabled) {
			fadeOut();
		}
		else {
			fadeIn();
		}
		mEnabled = enabled;
		return this;
	}

	public FloatingActionController setLocked(Boolean locked) {
		mLocked = locked;
		return this;
	}

	public FloatingActionController setView(View view) {
		mView = view;
		if (view != null) {
			mFabIcon = (ImageView) view.findViewById(R.id.fab_image);
		}
		return this;
	}
}
