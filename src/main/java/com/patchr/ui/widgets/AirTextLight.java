package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.patchr.components.FontManager;

public class AirTextLight extends AirTextView {

	public AirTextLight(Context context) {
		super(context, null);
	}

	public AirTextLight(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.textViewStyle);
	}

	public AirTextLight(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void setTypeface() {
		FontManager.getInstance().setTypefaceLight(this);
	}
}
