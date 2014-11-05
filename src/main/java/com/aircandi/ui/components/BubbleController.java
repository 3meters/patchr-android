package com.aircandi.ui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.utilities.UI;

public class BubbleController {

	private View mView;
	private Boolean mEnabled = true;

	public BubbleController(View view) {
		mView = view;
	}

	public void show(final Boolean visible) {
		if (mView != null) {
			mView.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public ObjectAnimator fadeIn() {
		if (mView == null || (mView.getVisibility() == View.VISIBLE && mView.getAlpha() == 1f))
			return null;
		Logger.d(this, "Bubble: fading in");
		ObjectAnimator anim = ObjectAnimator.ofFloat(mView, "alpha", 1f);
		mView.setAlpha(0f);
		mView.setVisibility(View.VISIBLE);
		anim.setDuration(AnimationManager.DURATION_MEDIUM);
		anim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationStart(Animator animator) {
				animator.removeAllListeners();
			}
		});
		anim.start();
		return anim;
	}

	public ObjectAnimator fadeOut() {
		if (mView == null || (mView.getVisibility() == View.GONE && mView.getAlpha() == 0f))
			return null;
		Logger.d(this, "Bubble: fading out");
		ObjectAnimator anim = ObjectAnimator.ofFloat(mView, "alpha", 0f);
		anim.setDuration(AnimationManager.DURATION_MEDIUM);
		anim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animator animator) {
				mView.setVisibility(View.GONE);
				animator.removeAllListeners();
			}
		});
		anim.start();
		return anim;
	}

	public void position(final View header, final Integer headerHeightProjected) {

		if (mView != null && header != null) {

			ViewTreeObserver vto = header.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {

					if (Patchr.getInstance().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mView.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						int headerHeight = (headerHeightProjected != null)
						                   ? headerHeightProjected
						                   : header.getHeight();
						params.topMargin = headerHeight + UI.getRawPixelsForDisplayPixels(100f);
						mView.setLayoutParams(params);
					}
					else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mView.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						mView.setLayoutParams(params);
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

	public void setOnClickListener(View.OnClickListener listener) {
		mView.setOnClickListener(listener);
	}
}
