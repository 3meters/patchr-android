package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ScrollView;

import com.patchr.Constants;
import com.patchr.R;

public class AirScrollView extends ScrollView {

	private final Context mContext;
	private       int     mMaxYOverscrollDistance;
	private       Integer mMaxWidth;

	public AirScrollView(Context context) {
		super(context);
		mContext = context;
		initialize(context, null);
	}

	public AirScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initialize(context, attrs);
	}

	public AirScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initialize(context, attrs);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (mMaxWidth != null && mMaxWidth > 0 && mMaxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	private void initialize(Context context, AttributeSet attrs) {
		/*
		 * get the density of the screen and do some maths with it on the max overscroll distance
		 * variable so that you get similar behaviors no matter what the screen size
		 */
		final DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		final float density = metrics.density;
		mMaxYOverscrollDistance = (int) (density * Constants.MAX_Y_OVERSCROLL_DISTANCE);

		TypedArray typeArray = mContext.obtainStyledAttributes(attrs, R.styleable.AirScrollView);
		mMaxWidth = typeArray.getDimensionPixelSize(R.styleable.AirScrollView_maxWidth
				, mContext.getResources().getDimensionPixelSize(R.dimen.form_max_width));

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
