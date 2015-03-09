package com.aircandi.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.components.AnimationManager;
import com.aircandi.utilities.UI;

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

	public void position(final View header, final Integer headerHeightProjected) {

		if (mMessage != null && header != null) {

			ViewTreeObserver vto = header.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {

					if (Patchr.getInstance().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mMessage.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						int headerHeight = (headerHeightProjected != null)
						                   ? headerHeightProjected
						                   : header.getHeight();
						params.topMargin = headerHeight + UI.getRawPixelsForDisplayPixels(100f);
						mMessage.setLayoutParams(params);
					}
					else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mMessage.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						mMessage.setLayoutParams(params);
					}

					if (Constants.SUPPORTS_JELLY_BEAN) {
						header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
					else {
						header.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
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
