package com.patchr.ui.components;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.R;
import com.patchr.model.RealmEntity;

import java.util.List;

class SuggestArrayAdapter extends RecyclerView.Adapter<RealmRecyclerViewHolder> {

	private List<RealmEntity> entities;
	private LayoutInflater    inflater;

	public SuggestArrayAdapter(Context context, List<RealmEntity> entities) {
		this.entities = entities;
		this.inflater = LayoutInflater.from(context);
	}

	@Override
	public RealmRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.listitem_search, parent, false);
		return new RealmRecyclerViewHolder(view);
	}

	@Override public void onBindViewHolder(RealmRecyclerViewHolder holder, int position) {
		RealmEntity entity = this.entities.get(position);
		holder.bind(entity);
	}

	@Override public int getItemCount() {
		return this.entities.size();
	}
}
