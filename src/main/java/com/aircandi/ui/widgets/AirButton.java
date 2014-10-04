package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Button;

import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.StringManager;

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

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirButton, defStyle, 0);
		Integer textId = ta.getResourceId(R.styleable.AirButton_textId, 0);
		ta.recycle();

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceRegular(this);
			if (textId != 0) {
				super.setText(StringManager.getString(textId, context, getResources()));
			}
		}
	}
}
