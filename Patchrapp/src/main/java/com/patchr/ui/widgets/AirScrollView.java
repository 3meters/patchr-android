package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.patchr.Constants;
import com.patchr.utilities.UI;

public class AirScrollView extends ScrollView {

	public AirScrollView(Context context) {
		super(context);
	}

	public AirScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
