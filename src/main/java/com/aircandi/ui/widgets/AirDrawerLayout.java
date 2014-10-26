package com.aircandi.ui.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.aircandi.components.Logger;

public class AirDrawerLayout extends DrawerLayout {

	private GestureDetectorCompat mDetector;

	public AirDrawerLayout(Context context) {
		super(context);
		initialize(context, null);
	}

	public AirDrawerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, attrs);
	}

	public AirDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context, attrs);
	}

	private void initialize(Context context, AttributeSet attrs) {
		mDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener());
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (mDetector.onTouchEvent(event)) {
			/*
			 * We detected a touch event (gesture) we want to override and do not
			 * give the super chance a shot at it because we do not call
			 * super.onTouchEvent.
			 */
			return true;
		}
		return super.onTouchEvent(event);
	}
}
