package com.patchr.ui.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.patchr.components.Logger;

@SuppressWarnings("ucd")
public class AirDrawerListView extends AirListView {

	public AirDrawerListView(Context context) {
		super(context);
		initialize(context, null);
	}

	public AirDrawerListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, attrs);
	}

	public AirDrawerListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context, attrs);
	}

	private void initialize(Context context, AttributeSet attrs) {
		/*
		 * Get the density of the screen and do some maths with it on the max overscroll distance
		 * variable so that you get similar behaviors no matter what the screen size.
		 */
		mDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {

			@Override
			public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
				Logger.v(this, "onScroll: vertical = " + (Math.abs(distanceY) > Math.abs(distanceX)));
				return (Math.abs(distanceY) > Math.abs(distanceX));
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {

		try {
			if (mDetector.onTouchEvent(event)) {
				((AirSwipeRefreshLayout) getParent()).requestDrawerDisallowInterceptTouchEvent(true);
			}
		}
		catch (ClassCastException e) {
			throw new ClassCastException("Parent must be SwipeRefreshLayout");
		}

		return super.onTouchEvent(event);
	}

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
