package com.patchr.ui.widgets;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.patchr.Constants;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirRecyclerView extends RecyclerView {

	public AirRecyclerView(Context context) {
		super(context);
	}

	public AirRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int maxWidth = UI.getRawPixelsForDisplayPixels((float) Constants.MAX_WIDTH_LIST);
		if (maxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
