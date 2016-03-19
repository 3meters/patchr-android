package com.patchr.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.patchr.components.AnimationManager;

public class EmptyController {

	public View label;
	private ObjectAnimator fadeInAnim  = ObjectAnimator.ofFloat(null, "alpha", 1f);
	private ObjectAnimator fadeOutAnim = ObjectAnimator.ofFloat(null, "alpha", 0f);

	public EmptyController(View view) {
		label = view;
	}

	public void showEmptyMessage(final Boolean visible) {
		if (label != null) {
			label.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public void fadeIn(Integer delay) {
		if (label == null || (label.getVisibility() == View.VISIBLE && label.getAlpha() == 1f))
			return;

		fadeInAnim.setTarget(label);
		label.setAlpha(0f);
		label.setVisibility(View.VISIBLE);
		fadeInAnim.setDuration(AnimationManager.DURATION_MEDIUM);
		fadeInAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationStart(@NonNull Animator animator) {
				animator.removeAllListeners();
			}
		});
		if (delay > 0) {
			fadeInAnim.setStartDelay(delay);
		}
		fadeInAnim.start();
	}

	public void fadeOut() {
		if (label == null || (label.getVisibility() == View.GONE && label.getAlpha() == 0f))
			return;

		fadeOutAnim.setTarget(label);
		fadeOutAnim.setDuration(AnimationManager.DURATION_MEDIUM);
		fadeOutAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(@NonNull Animator animator) {
				label.setVisibility(View.GONE);
				animator.removeAllListeners();
			}
		});
		fadeOutAnim.start();
	}

	public void setLabel(String label) {
		if (this.label != null) {
			if (!(this.label instanceof TextView)) {
				throw new RuntimeException("Cannot call setMessage if not a TextView");
			}
			((TextView) this.label).setText(label);
		}
	}
}
