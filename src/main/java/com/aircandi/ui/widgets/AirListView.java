package com.aircandi.ui.widgets;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.aircandi.Constants;
import com.aircandi.R;

@SuppressWarnings("ucd")
public class AirListView extends ListView implements OnScrollListener {

	private final Context        mContext;
	private       int            mMaxYOverscrollDistance;
	private       OnDragListener mDragListener;
	private       float          mInitialTouchX;
	private       float          mInitialTouchY;
	private       float          mLastX;
	private       float          mLastY;
	private       float          mDragX;
	private       float          mDragY;
	private DragDirection mDragDirectionLast = DragDirection.NONE;
	private GestureDetectorCompat mDetector;
	private Integer               mMaxWidth;

	private static final float   DEFAULT_PARALLAX_FACTOR = 1.9F;
	private static final boolean DEFAULT_IS_CIRCULAR     = false;
	private              float   mParallaxFactor         = DEFAULT_PARALLAX_FACTOR;
	private ParallaxedViewBase mParallaxedHeaderView;
	private ParallaxedViewBase mParallaxedView;
	private boolean            mIsCircular;
	private OnScrollListener mListener = null;
	private Drawable  mActionBarBackgroundDrawable;
	private View      mHeaderView;
	private ActionBar mActionBar;

	public AirListView(Context context) {
		super(context);
		mContext = context;
		initialize(context, null);
	}

	public AirListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initialize(context, attrs);
	}

	public AirListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initialize(context, attrs);
	}

	private void initialize(Context context, AttributeSet attrs) {
		/*
		 * Get the density of the screen and do some maths with it on the max overscroll distance
		 * variable so that you get similar behaviors no matter what the screen size.
		 */
		final DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		final float density = metrics.density;
		mMaxYOverscrollDistance = (int) (density * Constants.MAX_Y_OVERSCROLL_DISTANCE);
		mDetector = new GestureDetectorCompat(mContext, new GestureListener());

		TypedArray typeArray = mContext.obtainStyledAttributes(attrs, R.styleable.AirListView);
		mParallaxFactor = typeArray.getFloat(R.styleable.AirListView_parallax_factor, DEFAULT_PARALLAX_FACTOR);
		mIsCircular = typeArray.getBoolean(R.styleable.AirListView_circular_parallax, DEFAULT_IS_CIRCULAR);
		mMaxWidth = typeArray.getDimensionPixelSize(R.styleable.AirListView_maxWidth
				, mContext.getResources().getDimensionPixelSize(R.dimen.list_max_width));

		typeArray.recycle();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		parallaxScroll();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (this.mListener != null) {
			this.mListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (this.mListener != null) {
			this.mListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
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
		 * Only called if there is an overscroll.
		 * 
		 * This is where the magic happens, we have replaced the incoming maxOverScrollY
		 * with our own custom variable mMaxYOverscrollDistance;
		 */
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, mMaxYOverscrollDistance, isTouchEvent);
	}

	@Override
	public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {

		final float x = event.getX(), y = event.getY();

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:  // Only reliable place to catch the down

				mLastX = x;
				mLastY = y;

				mInitialTouchX = mLastX;
				mInitialTouchY = mLastY;

				break;

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				break;

			case MotionEvent.ACTION_MOVE:
				break;
		}

		return super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {

		if (!mDetector.onTouchEvent(event)) { // We stop if a fling is detected

			switch (event.getAction()) {

				case MotionEvent.ACTION_MOVE: {

					final float x = event.getX(), y = event.getY();

					if (Math.abs(y - mLastY) > 0.0000001) {
						/*
						 * We note change in drag direction so we can add that to our fling logic.
						 */
						DragDirection current = (y < mLastY) ? DragDirection.UP : DragDirection.DOWN;

						if (current != mDragDirectionLast) {
							mDragDirectionLast = current;
						}

						mDragX = x - mInitialTouchX;
						mDragY = y - mInitialTouchY;

						mLastX = x;
						mLastY = y;

						if (mDragListener != null) {
							mDragListener.onDragEvent(DragEvent.DRAG, mDragX, mDragY);
						}
					}
					break;
				}

				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_POINTER_UP:
				case MotionEvent.ACTION_UP: {

					if (mDragDirectionLast != DragDirection.NONE && mDragListener != null) {
						mDragListener.onDragEvent(DragEvent.STOP, mDragX, mDragY);
					}
					break;
				}

			}
		}

		return super.onTouchEvent(event);
	}

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

	protected void addAlphaEffect() {
		if (mHeaderView == null) return;

		View topChild = getChildAt(0);
		Integer scrollPosition;
		if (topChild == null) {
			scrollPosition = 0;
		}
		else {
			scrollPosition = -topChild.getTop();
		}

		int currentHeaderHeight = mHeaderView.getHeight();

		int headerHeight = currentHeaderHeight - mActionBar.getHeight();
		float ratio = (float) Math.min(Math.max(scrollPosition, 0), headerHeight) / headerHeight;
		int newAlpha = (int) (ratio * 255);
		mActionBarBackgroundDrawable.setAlpha(newAlpha);

	}

	protected void parallaxScroll() {
		if (mIsCircular) {
			circularParallax();
		}
		else {
			headerParallax();
		}
	}

	private void circularParallax() {
		if (getChildCount() > 0) {
			int top = -getChildAt(0).getTop();
			float factor = mParallaxFactor;
			fillParallaxedViews();
			mParallaxedHeaderView.setOffset((float) top / factor);
		}
	}

	private void headerParallax() {
		int top = 0;
		if (mParallaxedHeaderView != null) {
			if (getChildCount() > 0) {
				top = -getChildAt(0).getTop();
				float factor = mParallaxFactor;
				mParallaxedHeaderView.setOffset((float) top / factor);
			}
		}

		if (mParallaxedView != null) {
			if (mParallaxedHeaderView != null) {
				top = top / 2;
				float factor = mParallaxFactor;
				mParallaxedView.setOffset((float) top / factor);
			}
			else {
				if (getChildCount() > 0) {
					top = -getChildAt(0).getTop();
					float factor = mParallaxFactor;
					mParallaxedView.setOffset((float) top / factor);
				}
			}
		}
	}

	private void fillParallaxedViews() {
		if (mParallaxedHeaderView == null || !mParallaxedHeaderView.is(getChildAt(0))) {
			if (mParallaxedHeaderView != null) {
				mParallaxedHeaderView.setOffset(0);
				mParallaxedHeaderView.setView(getChildAt(0));
			}
			else {
				mParallaxedHeaderView = new ParallaxedView(getChildAt(0));
			}
		}
	}

	public void addParallaxedHeaderView(View view) {
		super.addHeaderView(view);
		this.mParallaxedHeaderView = new ParallaxedView(view);
	}

	public void addParallaxedHeaderView(View view, Object data, boolean isSelectable) {
		super.addHeaderView(view, data, isSelectable);
		this.mParallaxedHeaderView = new ParallaxedView(view);
	}

	public void addParallaxedView(View view) {
		this.mParallaxedView = new ParallaxedView(view);
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public void setActionBarBackgroundDrawable(Drawable actionBarBackgroundDrawable) {
		mActionBarBackgroundDrawable = actionBarBackgroundDrawable;
	}

	public void setHeaderView(View headerView) {
		mHeaderView = headerView;
	}

	public void setActionBar(ActionBar actionBar) {
		mActionBar = actionBar;
	}

	public void setDragListener(OnDragListener dragListener) {
		mDragListener = dragListener;
	}

	public void setDragDirectionLast(DragDirection dragDirectionLast) {
		mDragDirectionLast = dragDirectionLast;
	}

	public Drawable getActionBarBackgroundDrawable() {
		return mActionBarBackgroundDrawable;
	}

	public OnDragListener getDragListener() {
		return mDragListener;
	}

	public DragDirection getDragDirectionLast() {
		return mDragDirectionLast;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class ParallaxedView extends ParallaxedViewBase {

		public ParallaxedView(View view) {
			super(view);
		}

		@Override
		protected void translatePreICS(View view, float offset) {
			TranslateAnimation ta = new TranslateAnimation(0, 0, offset, offset);
			ta.setDuration(0);
			ta.setFillAfter(true);
			view.setAnimation(ta);
			ta.start();
		}
	}

	class GestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

			if (mDragListener != null) {
				mDragListener.onDragEvent(DragEvent.FLING, mDragX, mDragY);
			}
			return true;
		}
	}

	@SuppressWarnings("ucd")
	public interface OnRefreshListener {
		public void onRefreshStarted(View view);
	}

	public interface OnDragListener {
		public boolean onDragEvent(DragEvent event, Float dragX, Float dragY);
	}

	public enum DragEvent {
		DRAG,
		STOP,
		FLING
	}

	public enum DragDirection {
		NONE,
		UP,
		DOWN
	}
}
