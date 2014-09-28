package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.StringManager;

public class AirCheckBox extends CheckBox {

	public AirCheckBox(Context context) {
		this(context, null);
	}

	public AirCheckBox(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.checkboxStyle);
	}

	public AirCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirButton, defStyle, 0);
		Integer textId = ta.getResourceId(R.styleable.AirCheckBox_textId, 0);
		ta.recycle();

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
			if (textId != 0) {
				super.setText(StringManager.getString(textId, context, getResources()));
			}
		}
	}
}
