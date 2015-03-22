package com.aircandi.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.aircandi.components.AnimationManager;

public class MessageController {

	private View mMessage;
	private ObjectAnimator mFadeInAnim  = ObjectAnimator.ofFloat(null, "alpha", 1f);
	private ObjectAnimator mFadeOutAnim = ObjectAnimator.ofFloat(null, "alpha", 0f);

	public MessageController(View view) {
		mMessage = view;
	}

	public void showMessage(final Boolean visible) {
		if (mMessage != null) {
			mMessage.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public void fadeIn(Integer delay) {
		if (mMessage == null || (mMessage.getVisibility() == View.VISIBLE && mMessage.getAlpha() == 1f))
			return;

		mFadeInAnim.setTarget(mMessage);
		mMessage.setAlpha(0f);
		mMessage.setVisibility(View.VISIBLE);
		mFadeInAnim.setDuration(AnimationManager.DURATION_MEDIUM);
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

	public void fadeOut() {
		if (mMessage == null || (mMessage.getVisibility() == View.GONE && mMessage.getAlpha() == 0f))
			return;

		mFadeOutAnim.setTarget(mMessage);
		mFadeOutAnim.setDuration(AnimationManager.DURATION_MEDIUM);
		mFadeOutAnim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(@NonNull Animator animator) {
				mMessage.setVisibility(View.GONE);
				animator.removeAllListeners();
			}
		});
		mFadeOutAnim.start();
	}

	public void position(final View view, final View header, final Integer headerHeightProjected) {
		ListController.position(mMessage, header, headerHeightProjected);
	}

	public void setMessage(String label) {
		if (mMessage != null) {
			if (!(mMessage instanceof TextView)) {
				throw new RuntimeException("Cannot call setMessage if not a TextView");
			}
			((TextView) mMessage).setText(label);
		}
	}
}
