package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.patchr.components.FontManager;

public class AirCheckBox extends CheckBox {

	public AirCheckBox(Context context) {
		this(context, null);
	}

	public AirCheckBox(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.checkboxStyle);
	}

	public AirCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
		}
	}
}
