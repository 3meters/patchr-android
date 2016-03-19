package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirSwipeRefreshLayout extends SwipeRefreshLayout {

	private final Context context;
	private       Integer maxWidth;

	public AirSwipeRefreshLayout(Context context) {
		super(context);
		this.context = context;
		initialize(context, null);
	}

	public AirSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		initialize(context, attrs);
	}

	public AirSwipeRefreshLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		this.context = context;
		initialize(context, attrs);
	}

	private void initialize(Context context, AttributeSet attrs) {
		TypedArray typeArray = this.context.obtainStyledAttributes(attrs, R.styleable.AirListView);
		maxWidth = typeArray.getDimensionPixelSize(R.styleable.AirListView_maxWidth
				, this.context.getResources().getDimensionPixelSize(R.dimen.list_max_width));

		typeArray.recycle();
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
