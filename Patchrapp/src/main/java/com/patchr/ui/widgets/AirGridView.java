package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

import com.patchr.Constants;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirGridView extends GridView {

	public AirGridView(Context context) {
		super(context);
	}

	public AirGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int maxWidth = UI.getRawPixelsForDisplayPixels((float) Constants.MAX_WIDTH_GRID);
		if (maxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
