package com.patchr.ui.components;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.patchr.R;
import com.patchr.model.RealmEntity;

import java.util.Map;

import io.realm.OrderedRealmCollection;

public class RealmArrayAdapter extends RecyclerView.Adapter<RealmRecyclerViewHolder> {
	private static final int TYPE_HEADER = 0;
	private static final int TYPE_ITEM   = 1;
	private static final int TYPE_FOOTER = 2;

	public  OrderedRealmCollection<RealmEntity> data;   // Live results
	private LayoutInflater                      inflater;
	private Context                             context;

	public View header;
	public View footerView;
	public Map  options;
	public int  listItemResId;
	public boolean showIndex      = true;
	public boolean pagingDisabled = true;
	public boolean     more;
	public RealmEntity contextEntity;

	public RealmArrayAdapter(Context context, OrderedRealmCollection<RealmEntity> data) {

		if (context == null) {
			throw new IllegalArgumentException("Context can not be null");
		}

		this.data = data;
		this.inflater = LayoutInflater.from(context);
		super.setHasStableIds(true);
	}

	@Override public RealmRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		if (viewType == TYPE_HEADER) {
			if (this.header.getParent() != null) {
				((ViewGroup) this.header.getParent()).removeView(this.header);
			}
			return new RealmRecyclerViewHolder(this.header);
		}
		else if (viewType == TYPE_FOOTER) {
			View view = inflater.inflate(R.layout.listitem_loading, parent, false);
			return new RealmRecyclerViewHolder(view);
		}
		else {
			View view = inflater.inflate(this.listItemResId, parent, false);

				/* Some fix-up so we don't need additional boilerplate list item layout */
			if (!this.showIndex) {
				View index = view.findViewById(R.id.index);
				if (index != null) {
					((ViewGroup) index.getParent()).removeView(index);
				}
			}

			return new RealmRecyclerViewHolder(view);
		}
	}

	@Override public void onBindViewHolder(RealmRecyclerViewHolder holder, int position) {
		int itemType = getItemViewType(position);
		if (itemType == TYPE_FOOTER) {
			ProgressBar progress = (ProgressBar) holder.entityView;
			if (progress != null) {
				progress.setIndeterminate(true);
			}
		}
		else if (itemType == TYPE_ITEM) {
			RealmEntity entity = getItem(position);
			if (entity != null) {
				entity.index = getIndex(position);
				holder.bind(entity, contextEntity, options);
			}
		}
	}

	@Override public int getItemCount() {
		int itemCount = isDataValid() ? data.size() : 0;
		if (header != null) {
			itemCount++;
		}
		if (!pagingDisabled && more) {
			itemCount++;
		}
		return itemCount;
	}

	@Override public long getItemId(int position) {
		if (header != null) {
			if (position == 0) {
				return 1000;
			}
			else if (!pagingDisabled && more && position == data.size() + 1) {
				return 2000;
			}
		}
		else if (!pagingDisabled && more && position == data.size()) {
			return 2000;
		}

		RealmEntity entity = getItem(position);
		return (entity != null) ? entity.idAsLong() : position;
	}

	@Override public int getItemViewType(int position) {
		if (header != null) {
			if (position == 0) {
				return TYPE_HEADER;
			}
			else if (!pagingDisabled && more && position == data.size() + 1) {
				return TYPE_FOOTER;
			}
		}
		else if (!pagingDisabled && more && position == data.size()) {
			return TYPE_FOOTER;
		}
		return TYPE_ITEM;
	}

	private RealmEntity getItem(int position) {
		int dataPosition = position;
		if (header != null) {
			dataPosition--;
		}
		return isDataValid() ? data.get(dataPosition) : null;
	}

	private Integer getIndex(int position) {
		if (header != null) {
			return position;
		}
		else {
			return position + 1;
		}
	}

	private boolean isDataValid() {
		return this.data != null && this.data.isValid();
	}
}
