package com.aircandi.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.StringManager;

public class FloatingActionController {
	private View mView;
	private Boolean mEnabled = true;
	private Boolean mLocked  = false;
	private Boolean mHidden  = false;
	private Boolean mSliding = false;
	private ImageView mFabIcon;
	private ObjectAnimator mSlideInAnim  = ObjectAnimator.ofFloat(null, "translationY", 0);
	private ObjectAnimator mSlideOutAnim = ObjectAnimator.ofFloat(null, "translationY", 0);

	public FloatingActionController(View view) {
		mView = view;
		if (view != null) {
			mFabIcon = (ImageView) view.findViewById(R.id.fab_image);
		}
	}

	public void click() {
		if (!mEnabled) {
			throw new RuntimeException("Cannot call click while not enabled");
		}
		if (mView != null) {
			mView.performClick();
		}
	}

	public void show(final Boolean visible) {
		if (mView != null) {
			mView.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public ObjectAnimator fadeIn() {
			/*
			 * Skips if already visible and full opacity. Always ensures
			 * default position.
			 */
		if (mView == null || (mView.getVisibility() == View.VISIBLE && mView.getAlpha() == 1f))
			return null;

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
		return anim;
	}

	public ObjectAnimator fadeOut() {
			/*
			 * Skips if already gone and fully transparent. Always ensures
			 * default position.
			 */
		if (mView == null || (mView.getVisibility() == View.GONE && mView.getAlpha() == 0f))
			return null;

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
		return anim;
	}

	public ObjectAnimator slideOut(Integer duration) {
			/*
			 * Skips if locked, sliding or already hidden.
			 */
		if (mLocked || mSliding || mHidden || mView == null) return null;

		mSliding = true;
		mSlideOutAnim.setTarget(mView);
		mSlideOutAnim.setFloatValues(mView.getHeight());
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
		return mSlideOutAnim;
	}

	public ObjectAnimator slideIn(Integer duration) {
			/*
			 * Skips if locked, sliding or not hidden.
			 */
		if (mLocked || mSliding || !mHidden) return null;

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
		mSlideInAnim.start();
		return mSlideInAnim;
	}

	public void setEnabled(Boolean enabled) {
		if (!enabled) {
			fadeOut();
		}
		else {
			fadeIn();
		}
		mEnabled = enabled;
	}

	public Boolean isEnabled() {
		return mEnabled;
	}

	public void setLocked(Boolean locked) {
		mLocked = locked;
	}

	public Boolean isLocked() {
		return mLocked;
	}

	public void setTag(Object tag) {
		if (!mEnabled) {
			throw new RuntimeException("Cannot call setTag while not enabled");
		}
		if (mView != null) {
			mView.setTag(tag);
		}
	}

	public void setText(int labelResId) {
		String label = StringManager.getString(labelResId);
		setText(label);
	}

	public void setText(String label) {
		if (!mEnabled) {
			throw new RuntimeException("Cannot call setText while not enabled");
		}
		if (mView != null) {
			if (!(mView instanceof TextView)) {
				throw new RuntimeException("Cannot call setText if not a TextView");
			}
			((TextView) mView).setText(label);
		}
	}

	public void setIcon(Integer drawableResId) {
		if (mFabIcon != null) {
			mFabIcon.setImageDrawable(Patchr.applicationContext.getResources().getDrawable(drawableResId));
		}
	}

	public void setIcon(Drawable drawable) {
		if (mFabIcon != null) {
			mFabIcon.setImageDrawable(drawable);
		}
	}
}
