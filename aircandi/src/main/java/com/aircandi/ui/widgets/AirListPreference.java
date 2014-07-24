package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.aircandi.R;
import com.aircandi.components.StringManager;

public class AirListPreference extends ListPreference {

	@SuppressWarnings("ucd")
	public AirListPreference(Context context) {
		this(context, null);
	}

	public AirListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirListPreference, 0, 0);
		Integer keyId = ta.getResourceId(R.styleable.AirListPreference_keyId, 0);
		ta.recycle();

		if (keyId != 0) {
			super.setKey(StringManager.getString(keyId, context, context.getResources()));
		}
	}
}
