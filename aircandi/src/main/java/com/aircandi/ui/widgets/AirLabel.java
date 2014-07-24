package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.StringManager;

@SuppressWarnings("ucd")
public class AirLabel extends TextView {

	@SuppressWarnings("ucd")
	public AirLabel(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirLabel(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirLabel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirLabel, defStyle, 0);
		Integer textId = ta.getResourceId(R.styleable.AirLabel_textId, 0);
		ta.recycle();

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceBoldDefault(this);
			if (textId != 0) {
				super.setText(StringManager.getString(textId, context, getResources()));
			}
		}
	}
}
