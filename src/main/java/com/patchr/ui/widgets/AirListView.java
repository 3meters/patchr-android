package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.patchr.Constants;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirListView extends ListView {

	public AirListView(Context context) {
		this(context, null, 0);
	}

	public AirListView(Context context, AttributeSet attrs) {
		this(context, null, 0);
	}

	public AirListView(Context context, AttributeSet attrs, int defStyle) {
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
