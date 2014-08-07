package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import com.aircandi.R;
import com.aircandi.components.StringManager;

public class AirCheckBoxPreference extends CheckBoxPreference {

	public AirCheckBoxPreference(Context context) {
		this(context, null);
	}

	public AirCheckBoxPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirCheckBoxPreference, 0, 0);
		Integer keyId = ta.getResourceId(R.styleable.AirCheckBoxPreference_keyId, 0);
		ta.recycle();

		if (keyId != 0) {
			super.setKey(StringManager.getString(keyId, context, context.getResources()));
		}
	}
}
