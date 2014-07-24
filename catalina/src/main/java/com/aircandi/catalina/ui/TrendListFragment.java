package com.aircandi.catalina.ui;

import java.util.Locale;

import android.view.View;
import android.widget.TextView;

import com.aircandi.catalina.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.ui.EntityListFragment;

public class TrendListFragment extends EntityListFragment {

	private Integer	mCountLabelResId;

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public ViewHolder bindHolder(View view, ViewHolder holder) {

		if (holder == null) {
			holder = new ViewHolderExtended();
		}

		((ViewHolderExtended) holder).countValue = (TextView) view.findViewById(R.id.count_value);
		((ViewHolderExtended) holder).countLabel = (TextView) view.findViewById(R.id.count_label);
		((ViewHolderExtended) holder).rank = (TextView) view.findViewById(R.id.rank);

		return super.bindHolder(view, holder);
	}

	@Override
	protected void drawListItem(Entity entity, View view, ViewHolder holder) {
		super.drawListItem(entity, view, holder);

		/*
		 * Trending data
		 */
		if (((ViewHolderExtended) holder).countLabel != null) {
			((ViewHolderExtended) holder).countLabel.setText(StringManager.getString(mCountLabelResId).toUpperCase(Locale.US));
		}
		if (((ViewHolderExtended) holder).countValue != null && entity.count != null) {
			((ViewHolderExtended) holder).countValue.setText(String.valueOf(entity.count.intValue()));
		}
		if (((ViewHolderExtended) holder).rank != null && entity.rank != null) {
			((ViewHolderExtended) holder).rank.setText(String.valueOf(entity.rank.intValue()));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public Integer getCountLabelResId() {
		return mCountLabelResId;
	}

	public EntityListFragment setCountLabelResId(Integer countLabelResId) {
		mCountLabelResId = countLabelResId;
		return this;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public class ViewHolderExtended extends ViewHolder {
		public TextView	countValue;
		public TextView	countLabel;
		public TextView	rank;
	}
}