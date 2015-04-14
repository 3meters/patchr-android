package com.aircandi.ui;

import android.view.View;
import android.widget.TextView;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.StringManager;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataNoopEvent;
import com.aircandi.events.DataResultEvent;
import com.aircandi.events.TrendRequestEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.ViewHolder;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Locale;

public class TrendListFragment extends EntityListFragment {

	private   Integer mCountLabelResId;
	protected String  mToSchema;
	protected String  mFromSchema;

	@Override
	public void bind(final BindingMode mode) {
		if (mEntities.size() == 0 || mode == BindingMode.MANUAL) {
			super.bind(mode);
		}
	}

	public void fetch(Integer skip, Integer limit, BindingMode mode) {

		TrendRequestEvent request = new TrendRequestEvent()
				.setLinkType(mLinkType)
				.setFromSchema(mFromSchema)
				.setToSchema(mToSchema);

		request.setActionType(mActionType)
		       .setTag(System.identityHashCode(this));

		if (mBound && mScopingEntity != null && mode != BindingMode.MANUAL) {
			request.setCacheStamp(mScopingEntity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
		super.onDataResult(event);
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe
	public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void bindListItem(Entity entity, View view, String groupTag) {

		IEntityController controller = Patchr.getInstance().getControllerForEntity(entity);

		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolderExtended();

			((ViewHolderExtended) holder).countValue = (TextView) view.findViewById(R.id.count_value);
			((ViewHolderExtended) holder).countLabel = (TextView) view.findViewById(R.id.count_label);

			controller.bindHolder(view, holder);
			view.setTag(holder);
		}
		holder.data = entity;

		controller.bind(entity, view, groupTag);
		/*
		 * Trending data
		 */
		if (((ViewHolderExtended) holder).countLabel != null) {
			((ViewHolderExtended) holder).countLabel.setText(StringManager.getString(mCountLabelResId).toUpperCase(Locale.US));
		}
		if (((ViewHolderExtended) holder).countValue != null && entity.count != null) {
			((ViewHolderExtended) holder).countValue.setText(String.valueOf(entity.count.intValue()));
		}

		/* Replace index with rank */
		if (holder.index != null && entity.rank != null) {
			holder.index.setText(String.valueOf(entity.rank.intValue()));
			UI.setVisibility(holder.index, View.VISIBLE);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Integer getCountLabelResId() {
		return mCountLabelResId;
	}

	public TrendListFragment setCountLabelResId(Integer countLabelResId) {
		mCountLabelResId = countLabelResId;
		return this;
	}

	public TrendListFragment setToSchema(String toSchema) {
		mToSchema = toSchema;
		return this;
	}

	public TrendListFragment setFromSchema(String fromSchema) {
		mFromSchema = fromSchema;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class ViewHolderExtended extends ViewHolder {
		public TextView countValue;
		public TextView countLabel;
	}
}