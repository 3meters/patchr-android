package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.patchr.R;
import com.patchr.components.FontManager;
import com.patchr.components.StringManager;

public class AirTextView extends TextView {

	@SuppressWarnings("ucd")
	public AirTextView(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirTextView, defStyle, 0);
		Integer textId = ta.getResourceId(R.styleable.AirTextView_textId, 0);
		ta.recycle();

		if (!isInEditMode()) {
			setTypeface();
			if (textId != 0) {
				super.setText(StringManager.getString(textId, context, getResources()));
			}
		}
	}

	protected void setTypeface() {
		FontManager.getInstance().setTypefaceDefault(this);
	}
}
