package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

@SuppressWarnings("ucd")
public class AirAutofitRecyclerView extends RecyclerView {

	private GridLayoutManager layoutManager;
	private int columnWidth = -1;

	public AirAutofitRecyclerView(Context context) {
		super(context);
		initialize(context, null);
	}

	public AirAutofitRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, attrs);
	}

	public AirAutofitRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context, attrs);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (columnWidth > 0) {
			int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
			layoutManager.setSpanCount(spanCount);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize(Context context, AttributeSet attrs) {
		if (attrs != null) {
			int[] attrsArray = {
				android.R.attr.columnWidth
			};
			TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
			columnWidth = array.getDimensionPixelSize(0, -1);
			array.recycle();
		}

		layoutManager = new GridLayoutManager(getContext(), 1);
		setLayoutManager(layoutManager);
	}
}
