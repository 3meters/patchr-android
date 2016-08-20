package com.patchr.ui.components;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public class EmptyArrayAdapter extends RecyclerView.Adapter<RealmRecyclerViewHolder> {

	@Override public RealmRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return null;
	}

	@Override public void onBindViewHolder(RealmRecyclerViewHolder holder, int position) {}

	@Override public int getItemCount() {
		return 0;
	}
}
