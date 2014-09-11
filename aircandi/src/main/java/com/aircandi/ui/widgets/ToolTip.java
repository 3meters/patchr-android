/*
 * Copyright 2013 Niek Haarman
 *
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

import android.graphics.Typeface;
import android.view.View;

public class ToolTip {

	private CharSequence  mText;
	private int           mTextResId;
	private int           mColor;
	private int           mTextColor;
	private int           mMaxWidth;
	private View          mContentView;
	private AnimationType mAnimationType;
	private boolean       mShowShadow;
	private boolean       mShowArrow;
	private ArrowPosition mArrowPosition;
	private Typeface      mTypeface;

	public ToolTip() {
		mText = null;
		mTypeface = null;
		mTextResId = 0;
		mColor = 0;
		mMaxWidth = 0;
		mContentView = null;
		mShowArrow = true;
		mShowShadow = false;
		mArrowPosition = ArrowPosition.AUTO;
		mAnimationType = AnimationType.FROM_MASTER_VIEW;
	}

	public ToolTip withText(final CharSequence text) {
		mText = text;
		mTextResId = 0;
		return this;
	}

	public ToolTip withText(final int resId) {
		mTextResId = resId;
		mText = null;
		return this;
	}

	public ToolTip withText(final int resId, final Typeface tf) {
		mTextResId = resId;
		mText = null;
		withTypeface(tf);
		return this;
	}

	public ToolTip withColor(final int color) {
		mColor = color;
		return this;
	}

	public ToolTip withTextColor(final int color) {
		mTextColor = color;
		return this;
	}

	public ToolTip withContentView(final View view) {
		mContentView = view;
		return this;
	}

	public ToolTip withAnimationType(final AnimationType animationType) {
		mAnimationType = animationType;
		return this;
	}

	public ToolTip withShadow(boolean showShadow) {
		mShowShadow = showShadow;
		return this;
	}

	public ToolTip withArrow(boolean showArrow) {
		mShowArrow = showArrow;
		return this;
	}

	public ToolTip withTypeface(final Typeface typeface) {
		mTypeface = typeface;
		return this;
	}

	public ToolTip setMaxWidth(int maxWidth) {
		mMaxWidth = maxWidth;
		return this;
	}

	public ToolTip setArrowPosition(ArrowPosition arrowPosition) {
		mArrowPosition = arrowPosition;
		return this;
	}

	public ArrowPosition getArrowPosition() {
		return mArrowPosition;
	}

	public int getMaxWidth() {
		return mMaxWidth;
	}

	public CharSequence getText() {
		return mText;
	}

	public int getTextResId() {
		return mTextResId;
	}

	public int getColor() {
		return mColor;
	}

	public int getTextColor() {
		return mTextColor;
	}

	public View getContentView() {
		return mContentView;
	}

	public AnimationType getAnimationType() {
		return mAnimationType;
	}

	public boolean showShadow() {
		return mShowShadow;
	}

	public boolean showArrow() {
		return mShowArrow;
	}

	public Typeface getTypeface() {
		return mTypeface;
	}

	public enum AnimationType {
		FROM_MASTER_VIEW,
		FROM_TOP,
		FROM_SELF,
		NONE
	}

	public enum ArrowPosition {
		AUTO,
		ABOVE,
		BELOW
	}
}
