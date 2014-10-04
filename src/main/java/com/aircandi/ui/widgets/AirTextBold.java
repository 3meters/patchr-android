package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.aircandi.components.FontManager;

@SuppressWarnings("ucd")
public class AirTextBold extends AirTextView {

	@SuppressWarnings("ucd")
	public AirTextBold(Context context) {
		super(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTextBold(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirTextBold(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void setTypeface() {
		FontManager.getInstance().setTypefaceBold(this);
	}
}
