package com.patchr.ui.widgets;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class AirDrawerSwipeRefreshLayout extends SwipeRefreshLayout {

	private int   touchSlop;
	private float initialDownY;

	public AirDrawerSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				this.initialDownY = event.getY();
				break;

			case MotionEvent.ACTION_MOVE:
				final float eventY = event.getY();
				float yDiff = Math.abs(eventY - this.initialDownY);

				if (yDiff > touchSlop) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
		}

		return super.onInterceptTouchEvent(event);
	}
}
