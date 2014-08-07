package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.GridView;

import com.aircandi.Constants;
import com.aircandi.R;

@SuppressWarnings("ucd")
public class AirGridView extends GridView {

	private final Context mContext;
	private       int     mMaxYOverscrollDistance;
	private       Integer mMaxWidth;

	public AirGridView(Context context) {
		super(context);
		mContext = context;
		initBounceScrollView(context, null);
	}

	public AirGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initBounceScrollView(context, attrs);
	}

	public AirGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initBounceScrollView(context, attrs);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (mMaxWidth != null && mMaxWidth > 0 && mMaxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void initBounceScrollView(Context context, AttributeSet attrs) {
		/*
		 * get the density of the screen and do some maths with it on the max overscroll distance
		 * variable so that you get similar behaviors no matter what the screen size
		 */
		final DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		final float density = metrics.density;
		mMaxYOverscrollDistance = (int) (density * Constants.MAX_Y_OVERSCROLL_DISTANCE);

		TypedArray typeArray = getContext().obtainStyledAttributes(attrs, R.styleable.AirGridView);
		mMaxWidth = typeArray.getDimensionPixelSize(R.styleable.AirGridView_maxWidth
				, getContext().getResources().getDimensionPixelSize(R.dimen.grid_max_width));
		typeArray.recycle();

	}

	@Override
	protected boolean overScrollBy(int deltaX
			, int deltaY
			, int scrollX
			, int scrollY
			, int scrollRangeX
			, int scrollRangeY
			, int maxOverScrollX
			, int maxOverScrollY
			, boolean isTouchEvent) {
		/*
		 * This is where the magic happens, we have replaced the incoming maxOverScrollY
		 * with our own custom variable mMaxYOverscrollDistance;
		 */
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, mMaxYOverscrollDistance, isTouchEvent);
	}

}
