package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.aircandi.R;

public class SmoothProgressBar extends fr.castorflex.android.smoothprogressbar.SmoothProgressBar {

	private int mColor;

	public SmoothProgressBar(Context context) {
		this(context, null);
	}

	public SmoothProgressBar(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.spbStyle);
	}

	public SmoothProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public int getColor() {
		return mColor;
	}
}