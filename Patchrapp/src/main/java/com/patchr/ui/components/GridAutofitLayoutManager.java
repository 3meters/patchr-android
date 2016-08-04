package com.patchr.ui.components;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.patchr.utilities.UI;

public class GridAutofitLayoutManager extends GridLayoutManager {

	private int columnWidth;
	private        boolean columnWidthChanged   = true;

	public GridAutofitLayoutManager(Context context, int columnWidth) {
		super(context, 1);
		setColumnWidth(checkedColumnWidth(context, columnWidth));
	}

	public GridAutofitLayoutManager(Context context, int columnWidth, int orientation, boolean reverseLayout) {
	    /* Initially set spanCount to 1, will be changed automatically later. */
		super(context, 1, orientation, reverseLayout);
		setColumnWidth(checkedColumnWidth(context, columnWidth));
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (columnWidthChanged && columnWidth > 0) {
			int totalSpace;
			if (getOrientation() == VERTICAL) {
				totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
			}
			else {
				totalSpace = getHeight() - getPaddingTop() - getPaddingBottom();
			}
			int spanCount = Math.max(1, totalSpace / columnWidth);
			setSpanCount(spanCount);
			columnWidthChanged = false;
		}
		super.onLayoutChildren(recycler, state);
	}

	private int checkedColumnWidth(Context context, int columnWidth) {
		if (columnWidth <= 0) {
			/* Set default columnWidth value (48dp here). It is better to move this constant
			to static constant on top, but we need context to convert it to dp, so can't really
	        do so. */
			float DEFAULT_COLUMN_WIDTH = 48;
			columnWidth = UI.getRawPixelsForDisplayPixels(DEFAULT_COLUMN_WIDTH);
		}
		return columnWidth;
	}

	public void setColumnWidth(int newColumnWidth) {
		if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
			columnWidth = newColumnWidth;
			columnWidthChanged = true;
		}
	}
}