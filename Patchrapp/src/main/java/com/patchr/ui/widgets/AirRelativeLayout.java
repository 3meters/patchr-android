package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.patchr.Constants;
import com.patchr.utilities.UI;

public class AirRelativeLayout extends RelativeLayout {

	public AirRelativeLayout(Context context) {
		super(context);
	}

	public AirRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
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
