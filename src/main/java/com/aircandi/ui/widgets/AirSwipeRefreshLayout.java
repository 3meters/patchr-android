package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.aircandi.R;

@SuppressWarnings("ucd")
public class AirSwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {

	private final Context     mContext;
	private       Integer     mMaxWidth;

	public AirSwipeRefreshLayout(Context context) {
		super(context);
		mContext = context;
		initialize(context, null);
	}

	public AirSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initialize(context, attrs);
	}

	public AirSwipeRefreshLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		mContext = context;
		initialize(context, attrs);
	}

	private void initialize(Context context, AttributeSet attrs) {
		TypedArray typeArray = mContext.obtainStyledAttributes(attrs, R.styleable.AirListView);
		mMaxWidth = typeArray.getDimensionPixelSize(R.styleable.AirListView_maxWidth
				, mContext.getResources().getDimensionPixelSize(R.dimen.list_max_width));

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
		if (mMaxWidth != null && mMaxWidth > 0 && mMaxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

//	@Override
//	public boolean canChildScrollUp() {
//		if (mList != null) {
//			Integer headerCount = ((ListView) mList).getHeaderViewsCount();
//			View view = mList.getChildAt(0);
//			int topOffset = (view == null) ? 0 : (headerCount == 0) ? view.getTop() : (view.getTop() - view.getPaddingTop());
//			return (topOffset < 0);
//		}
//		return true;
//	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}
