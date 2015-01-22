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
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.utilities.UI;

import org.jetbrains.annotations.Nullable;

public class EmptyController {

	private View mMessage;
	private Boolean mEnabled = true;

	public EmptyController(View view) {
		mMessage = view;
	}

	public void show(final Boolean visible) {
		if (mMessage != null) {
			mMessage.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public void fadeIn() {
		if (mMessage == null || (mMessage.getVisibility() == View.VISIBLE && mMessage.getAlpha() == 1f))
			return;
		Logger.d(this, "Bubble: fading in");
		ObjectAnimator anim = ObjectAnimator.ofFloat(mMessage, "alpha", 1f);
		mMessage.setAlpha(0f);
		mMessage.setVisibility(View.VISIBLE);
		anim.setDuration(AnimationManager.DURATION_MEDIUM);
		anim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationStart(@NonNull Animator animator) {
				animator.removeAllListeners();
			}
		});
		anim.start();
	}

	public void fadeOut() {
		if (mMessage == null || (mMessage.getVisibility() == View.GONE && mMessage.getAlpha() == 0f))
			return;
		Logger.d(this, "Bubble: fading out");
		ObjectAnimator anim = ObjectAnimator.ofFloat(mMessage, "alpha", 0f);
		anim.setDuration(AnimationManager.DURATION_MEDIUM);
		anim.addListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(@NonNull Animator animator) {
				mMessage.setVisibility(View.GONE);
				animator.removeAllListeners();
			}
		});
		anim.start();
	}

	public void position(@Nullable final View header, @Nullable final Integer headerHeightProjected) {

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
		if (mMessage != null) {
			if (!(mMessage instanceof TextView)) {
				throw new RuntimeException("Cannot call setText if not a TextView");
			}
			((TextView) mMessage).setText(label);
		}
	}

	public void setOnClickListener(View.OnClickListener listener) {
		mMessage.setOnClickListener(listener);
	}
}
