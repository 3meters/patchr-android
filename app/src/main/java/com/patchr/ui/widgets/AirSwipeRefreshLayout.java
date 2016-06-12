package com.patchr.ui.widgets;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.patchr.Constants;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirSwipeRefreshLayout extends SwipeRefreshLayout {

	public AirSwipeRefreshLayout(Context context) {
		super(context);
	}

	public AirSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirSwipeRefreshLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		getParent().requestDisallowInterceptTouchEvent(true);
		return super.onTouchEvent(event);
	}

	public void requestDrawerDisallowInterceptTouchEvent(boolean disallow) {
		getParent().requestDisallowInterceptTouchEvent(disallow);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int maxWidth = UI.getRawPixelsForDisplayPixels((float) Constants.MAX_WIDTH_FORM);
		if (maxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
