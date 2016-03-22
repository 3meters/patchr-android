package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.patchr.Constants;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirLinearLayout extends LinearLayout {

	public AirLinearLayout(Context context) {
		super(context);
	}

	public AirLinearLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int maxWidth = UI.getRawPixelsForDisplayPixels((float) Constants.MAX_WIDTH_FORM);
		if (maxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
