package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.aircandi.components.FontManager;

public class AirTextLight extends AirTextView {

	@SuppressWarnings("ucd")
	public AirTextLight(Context context) {
		super(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTextLight(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirTextLight(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void setTypeface() {
		FontManager.getInstance().setTypefaceLight(this);
	}
}
