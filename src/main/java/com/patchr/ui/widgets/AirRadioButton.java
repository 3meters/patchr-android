package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

import com.patchr.components.FontManager;

public class AirRadioButton extends RadioButton {

	public AirRadioButton(Context context) {
		this(context, null);
	}

	public AirRadioButton(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.radioButtonStyle);
	}

	public AirRadioButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
		}
	}
}
