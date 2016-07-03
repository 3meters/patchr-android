package com.patchr.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.components.AnimationManager;
import com.patchr.ui.BaseScreen;

public class EmptyController {

	public View label;
	private ObjectAnimator fadeInAnim  = ObjectAnimator.ofFloat(null, "alpha", 1f);
	private ObjectAnimator fadeOutAnim = ObjectAnimator.ofFloat(null, "alpha", 0f);

	public EmptyController(View view) {
		label = view;
		hide(false);
	}

	public void show(boolean animated) {
		if (label != null) {
			if (animated) {
				fadeIn(Constants.TIME_ONE_SECOND);
			}
			else {
				label.setVisibility(View.VISIBLE);
			}
		}
	}

	public void hide(boolean animated) {
		if (label != null) {
			if (animated) {
				fadeOut();
			}
			else {
				label.setVisibility(View.GONE);
			}
		}
	}

	public EmptyController setText(String text) {
		if (this.label != null) {
			if (!(this.label instanceof TextView)) {
				throw new RuntimeException("Cannot call setMessage if not a TextView");
			}
			((TextView) this.label).setText(text);
		}
		return this;
	}

	public void positionBelow(final View header, final Integer headerHeightProjected) {
		BaseScreen.position(this.label, header, headerHeightProjected);
	}

	private void fadeIn(Integer delay) {
		if (label == null || (label.getVisibility() == View.VISIBLE && label.getAlpha() == 1f))
			return;

		fadeInAnim.setTarget(label);
		label.setAlpha(0f);
		label.setVisibility(View.VISIBLE);
		fadeInAnim.setDuration(AnimationManager.DURATION_MEDIUM);
		fadeInAnim.addListener(new SimpleAnimationListener() {
			@Override public void onAnimationStart(@NonNull Animator animator) {
				animator.removeAllListeners();
			}
		});
		if (delay > 0) {
			fadeInAnim.setStartDelay(delay);
		}
		fadeInAnim.start();
	}

	private void fadeOut() {
		if (label == null || (label.getVisibility() == View.GONE && label.getAlpha() == 0f))
			return;

		fadeOutAnim.setTarget(label);
		fadeOutAnim.setDuration(AnimationManager.DURATION_MEDIUM);
		fadeOutAnim.addListener(new SimpleAnimationListener() {
			@Override public void onAnimationEnd(@NonNull Animator animator) {
				label.setVisibility(View.GONE);
				animator.removeAllListeners();
			}
		});
		fadeOutAnim.start();
	}
}
