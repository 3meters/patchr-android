package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.patchr.components.FontManager;

public class AirButton extends Button {

	@SuppressWarnings("ucd")
	public AirButton(Context context) {
		this(context, null);
	}

	public AirButton(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.buttonStyle);
	}

	public AirButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
		}
	}
}
