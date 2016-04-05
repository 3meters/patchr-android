package com.patchr.ui.components;

import android.support.v7.widget.RecyclerView;

public abstract class ListScrollListener extends RecyclerView.OnScrollListener {

	private int scrollOffset = 0;

	@Override
	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
		super.onScrolled(recyclerView, dx, dy);
		scrollOffset += dy;
		onMoved(scrollOffset);
	}

	public abstract void onMoved(int distance);
}