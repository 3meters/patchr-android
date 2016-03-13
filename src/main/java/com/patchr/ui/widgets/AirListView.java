package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.patchr.Constants;

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
		if (Constants.MAX_WIDTH_LIST < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(Constants.MAX_WIDTH_LIST, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
