/*
 * Copyright 2013 Niek Haarman
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aircandi.ui.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.UI;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A ViewGroup to visualize ToolTips. Use
 * ToolTipRelativeLayout.showToolTipForView() to show ToolTips.
 */
public class ToolTipView extends LinearLayout implements ViewTreeObserver.OnPreDrawListener {

	private static final int VERB_COUNT = 22;

	private ImageView mTopPointerView;
	private ViewGroup mContentHolder;
	private TextView  mToolTipTV;
	private ImageView mBottomPointerView;
	private View      mShadowView;

	private ToolTip mToolTip;

	private View        mTargetView;
	private AnimatorSet mAnimatorSet;

	private boolean               mDimensionsKnown;
	private int                   mRelativeTargetViewY;
	private int                   mRelativeTargetViewX;
	private int                   mToolTipViewX;
	private int                   mToolTipViewY;
	private int                   mWidth;
	private ToolTip.ArrowPosition mArrowPosition;

	private OnToolTipViewClickedListener mListener;

	public ToolTipView(final Context context) {
		super(context);
		init();
	}

	private void init() {

		setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		setOrientation(VERTICAL);
		LayoutInflater.from(getContext()).inflate(R.layout.widget_tooltip, this, true);

		mTopPointerView = (ImageView) findViewById(R.id.tooltip_pointer_up);
		mContentHolder = (ViewGroup) findViewById(R.id.tooltip_contentholder);
		mToolTipTV = (TextView) findViewById(R.id.tooltip_contenttv);
		mBottomPointerView = (ImageView) findViewById(R.id.tooltip_pointer_down);
		mShadowView = findViewById(R.id.tooltip_shadow);

		Integer colorResId = UI.getResIdForAttribute(this.getContext(), R.attr.backgroundTooltip);
		Integer textColorResId = UI.getResIdForAttribute(this.getContext(), R.attr.textColorTooltip);
		setColor(Colors.getColor(colorResId));
		mToolTipTV.setTextColor(Colors.getColor(textColorResId));

		getViewTreeObserver().addOnPreDrawListener(this);
	}

	@Override
	public boolean onPreDraw() {

		getViewTreeObserver().removeOnPreDrawListener(this);
		mDimensionsKnown = true;

		mWidth = mContentHolder.getWidth();
		mContentHolder.bringToFront();

		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
		layoutParams.width = mWidth;
		setLayoutParams(layoutParams);

		if (mToolTip != null && mTargetView != null) {
			applyToolTipPosition();
		}
		else {
			final int[] screenPosition = new int[2];
			getLocationOnScreen(screenPosition);
			mToolTipViewX = screenPosition[0];
			mToolTipViewY = screenPosition[1];
			animateView();
		}
		return true;
	}

	public void setToolTip(final ToolTip toolTip) {
		setToolTip(toolTip, null);
	}

	public void setToolTip(final ToolTip toolTip, final View targetView) {

		mToolTip = toolTip;
		mTargetView = targetView;

		if (mToolTip.getText() != null) {
			mToolTipTV.setText(mToolTip.getText());
		}
		else if (mToolTip.getTextResId() != 0) {
			mToolTipTV.setText(mToolTip.getTextResId());
		}

		if (mToolTip.getTypeface() != null) {
			mToolTipTV.setTypeface(mToolTip.getTypeface());
		}

		if (mToolTip.getTextColor() != 0) {
			mToolTipTV.setTextColor(mToolTip.getTextColor());
		}

		if (mToolTip.getColor() != 0) {
			setColor(mToolTip.getColor());
		}

		if (mToolTip.getContentView() != null) {
			setContentView(mToolTip.getContentView());
		}

		if (!mToolTip.showShadow()) {
			mShadowView.setVisibility(View.GONE);
		}

		if (!mToolTip.showArrow()) {
			mTopPointerView.setVisibility(GONE);
			mBottomPointerView.setVisibility(GONE);
		}

		if (mToolTip.getMaxWidth() != 0) {
			mToolTipTV.setMaxWidth(mToolTip.getMaxWidth());
			mToolTipTV.setMinWidth(mToolTip.getMaxWidth());
		}

		mArrowPosition = mToolTip.getArrowPosition();

		if (mDimensionsKnown && mTargetView != null) {
			applyToolTipPosition();
		}
	}

	private void applyToolTipPosition() {

		final int[] targetViewScreenPosition = new int[2];
		mTargetView.getLocationOnScreen(targetViewScreenPosition);

		final Rect displayFrame = new Rect();
		mTargetView.getWindowVisibleDisplayFrame(displayFrame);

		final int[] parentViewScreenPosition = new int[2];
		((View) getParent()).getLocationOnScreen(parentViewScreenPosition);

		final int targetViewWidth = mTargetView.getWidth();
		final int targetViewHeight = mTargetView.getHeight();

		mRelativeTargetViewX = targetViewScreenPosition[0] - parentViewScreenPosition[0];
		mRelativeTargetViewY = targetViewScreenPosition[1] - parentViewScreenPosition[1];
		final int relativeTargetViewCenterX = mRelativeTargetViewX + targetViewWidth / 2;

		int toolTipViewAboveY = mRelativeTargetViewY - getHeight();
		int toolTipViewBelowY = Math.max(0, mRelativeTargetViewY + targetViewHeight);

		mToolTipViewX = Math.max(0, relativeTargetViewCenterX - mWidth / 2);
		if (mToolTipViewX + mWidth > displayFrame.right) {
			mToolTipViewX = displayFrame.right - mWidth;
		}

		setX(mToolTipViewX);
		setPointerCenterX(relativeTargetViewCenterX);

		final boolean showBelow = (mArrowPosition == ToolTip.ArrowPosition.BELOW || toolTipViewAboveY < 0);

		if (mToolTip.showArrow()) {
			mTopPointerView.setVisibility(showBelow ? VISIBLE : GONE);
			mBottomPointerView.setVisibility(showBelow ? GONE : VISIBLE);
		}

		if (showBelow) {
			mToolTipViewY = toolTipViewBelowY;
		}
		else {
			mToolTipViewY = toolTipViewAboveY;
		}

		if (mToolTip.getAnimationType() == ToolTip.AnimationType.NONE) {
			setTranslationY(mToolTipViewY);
			setTranslationX(mToolTipViewX);
		}
		else {
			animateView();
		}
	}

	public void animateView() {

		if (mAnimatorSet != null) {
			mAnimatorSet.start();
		}
		else {
			Collection<Animator> animators = new ArrayList<Animator>(5);

			if (mToolTip.getAnimationType() == ToolTip.AnimationType.FROM_MASTER_VIEW) {
				animators.add(ObjectAnimator.ofInt(this, AnimationManager.TRANSLATION_Y_COMPAT, mRelativeTargetViewY + mTargetView.getHeight() / 2 - getHeight() / 2, mToolTipViewY));
				animators.add(ObjectAnimator.ofInt(this, AnimationManager.TRANSLATION_X_COMPAT, mRelativeTargetViewX + mTargetView.getWidth() / 2 - mWidth / 2, mToolTipViewX));
			}
			else if (mToolTip.getAnimationType() == ToolTip.AnimationType.FROM_TOP) {
				animators.add(ObjectAnimator.ofFloat(this, AnimationManager.TRANSLATION_Y_COMPAT, 0, mToolTipViewY));
			}

			animators.add(ObjectAnimator.ofFloat(this, AnimationManager.SCALE_X_COMPAT, 0, 1));
			animators.add(ObjectAnimator.ofFloat(this, AnimationManager.SCALE_Y_COMPAT, 0, 1));
			animators.add(ObjectAnimator.ofFloat(this, AnimationManager.ALPHA_COMPAT, 0, 1));

			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.playTogether(animators);

			animatorSet.start();
		}
	}

	public void setPointerCenterX(final int pointerCenterX) {
		int pointerWidth = Math.max(mTopPointerView.getMeasuredWidth(), mBottomPointerView.getMeasuredWidth());

		mTopPointerView.setX(pointerCenterX - pointerWidth / 2 - (int) getX());
		mBottomPointerView.setX(pointerCenterX - pointerWidth / 2 - (int) getX());
	}

	public void setOnToolTipViewClickedListener(final OnToolTipViewClickedListener listener) {
		mListener = listener;
	}

	public void setColor(final int color) {
		mTopPointerView.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
		mBottomPointerView.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
	}

	private void setContentView(final View view) {
		mContentHolder.removeAllViews();
		mContentHolder.addView(view);
	}

	public void remove() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
			setX(params.leftMargin);
			setY(params.topMargin);
			params.leftMargin = 0;
			params.topMargin = 0;
			setLayoutParams(params);
		}

		if (mToolTip.getAnimationType() == ToolTip.AnimationType.NONE) {
			if (getParent() != null) {
				((ViewManager) getParent()).removeView(this);
			}
		}
		else {
			Collection<Animator> animators = new ArrayList<Animator>(5);
			if (mToolTip.getAnimationType() == ToolTip.AnimationType.FROM_MASTER_VIEW) {
				animators.add(ObjectAnimator.ofInt(this, AnimationManager.TRANSLATION_Y_COMPAT, (int) getY(), mRelativeTargetViewY + mTargetView.getHeight() / 2 - getHeight() / 2));
				animators.add(ObjectAnimator.ofInt(this, AnimationManager.TRANSLATION_X_COMPAT, (int) getX(), mRelativeTargetViewX + mTargetView.getWidth() / 2 - mWidth / 2));
			}
			else {
				animators.add(ObjectAnimator.ofFloat(this, AnimationManager.TRANSLATION_Y_COMPAT, getY(), 0));
			}

			animators.add(ObjectAnimator.ofFloat(this, AnimationManager.SCALE_X_COMPAT, 1, 0));
			animators.add(ObjectAnimator.ofFloat(this, AnimationManager.SCALE_Y_COMPAT, 1, 0));

			animators.add(ObjectAnimator.ofFloat(this, AnimationManager.ALPHA_COMPAT, 1, 0));

			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.playTogether(animators);
			animatorSet.addListener(new DisappearanceAnimatorListener());
			animatorSet.start();
		}
	}

	public void addRule(int verb) {
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
		params.addRule(verb);
		setLayoutParams(params);
	}

	public void addRule(int verb, int anchor) {
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
		params.addRule(verb, anchor);
		setLayoutParams(params);
	}

	public interface OnToolTipViewClickedListener {
		void onToolTipViewClicked(ToolTipView toolTipView);
	}

	public AnimatorSet getAnimatorSet() {
		return mAnimatorSet;
	}

	public void setAnimatorSet(AnimatorSet animatorSet) {
		mAnimatorSet = animatorSet;
	}

	private class AppearanceAnimatorListener extends AnimatorListenerAdapter {

		private final float mToolTipViewX;
		private final float mToolTipViewY;

		AppearanceAnimatorListener(final float fToolTipViewX, final float fToolTipViewY) {
			mToolTipViewX = fToolTipViewX;
			mToolTipViewY = fToolTipViewY;
		}

		@Override
		public void onAnimationStart(final Animator animation) {
		}

		@Override
		@SuppressLint("NewApi")
		public void onAnimationEnd(final Animator animation) {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
			params.leftMargin = (int) mToolTipViewX;
			params.topMargin = (int) mToolTipViewY;
			setX(0);
			setY(0);
			setLayoutParams(params);
		}

		@Override
		public void onAnimationCancel(final Animator animation) {
		}

		@Override
		public void onAnimationRepeat(final Animator animation) {
		}
	}

	private class DisappearanceAnimatorListener extends AnimatorListenerAdapter {

		@Override
		public void onAnimationStart(final Animator animation) {
		}

		@Override
		public void onAnimationEnd(final Animator animation) {
			if (getParent() != null) {
				((ViewManager) getParent()).removeView(ToolTipView.this);
			}
		}

		@Override
		public void onAnimationCancel(final Animator animation) {
		}

		@Override
		public void onAnimationRepeat(final Animator animation) {
		}
	}
}
