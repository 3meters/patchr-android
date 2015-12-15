package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.patchr.components.FontManager;

public class AirTextMedium extends AirTextView {

	@SuppressWarnings("ucd")
	public AirTextMedium(Context context) {
		super(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTextMedium(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirTextMedium(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void setTypeface() {
		FontManager.getInstance().setTypefaceMedium(this);
	}
}
