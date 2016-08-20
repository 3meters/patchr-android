package com.patchr.ui.components;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

	private int     visibleThreshold       = 5;     // Minimum items below scroll position to trigger loading
	private int     currentPage            = 0;     // The current offset index of data you have loaded
	private int     previousTotalItemCount = 0;     // The total number of items in the dataset after the last load
	private boolean loading                = true;  // True if we are still waiting for the last set of data to load.

	RecyclerView.LayoutManager layoutManager;

	public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
	}

	public EndlessRecyclerViewScrollListener(GridLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
		visibleThreshold = visibleThreshold * layoutManager.getSpanCount();
	}

	public EndlessRecyclerViewScrollListener(StaggeredGridLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
		visibleThreshold = visibleThreshold * layoutManager.getSpanCount();
	}

	public int getLastVisibleItem(int[] lastVisibleItemPositions) {
		int maxSize = 0;
		for (int i = 0; i < lastVisibleItemPositions.length; i++) {
			if (i == 0) {
				maxSize = lastVisibleItemPositions[i];
			}
			else if (lastVisibleItemPositions[i] > maxSize) {
				maxSize = lastVisibleItemPositions[i];
			}
		}
		return maxSize;
	}

	@Override public void onScrolled(RecyclerView view, int dx, int dy) {
		/*
		 * This happens many times a second during a scroll, so be wary of the code you place here.
		 * We are given a few useful parameters to help us work out if we need to load some more data,
		 * but first we check if we are waiting for the previous load to finish.
		 */
		int lastVisibleItemPosition = 0;
		int totalItemCount = layoutManager.getItemCount();

		if (layoutManager instanceof StaggeredGridLayoutManager) {
			int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
			// get maximum element within the list
			lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
		}
		else if (layoutManager instanceof LinearLayoutManager) {
			lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
		}
		else if (layoutManager instanceof GridLayoutManager) {
			lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
		}

		// If the total item count is zero and the previous isn't, assume the
		// list is invalidated and should be reset back to initial state
		if (totalItemCount < previousTotalItemCount) {
			this.currentPage = 0;
			this.previousTotalItemCount = totalItemCount;
			if (totalItemCount == 0) {
				this.loading = true;
			}
		}
		// If it’s still loading, we check to see if the dataset count has
		// changed, if so we conclude it has finished loading and update the current page
		// number and total item count.
		if (loading && (totalItemCount > previousTotalItemCount)) {
			loading = false;
			previousTotalItemCount = totalItemCount;
		}

		// If it isn’t currently loading, we check to see if we have breached
		// the visibleThreshold and need to reload more data.
		// If we do need to reload some more data, we execute onLoadMore to fetch the data.
		// threshold should reflect how many total columns there are too
		if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
			currentPage++;
			onLoadMore(currentPage, totalItemCount);
			loading = true;
		}
	}

	// Defines the process for actually loading more data based on page
	public abstract void onLoadMore(int page, int totalItemsCount);
}