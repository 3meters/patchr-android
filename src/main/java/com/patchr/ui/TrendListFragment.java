package com.patchr.ui;

import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.StringManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.TrendRequestEvent;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.ViewHolder;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Locale;

public class TrendListFragment extends EntityListFragment {

	private   Integer mCountLabelResId;
	protected String  mToSchema;
	protected String  mFromSchema;

	@Override
	public void bind(final BindingMode mode) {
		if (mEntities.size() == 0 || mode == BindingMode.MANUAL) {
			mEntities.clear();
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

	@Subscribe
	public void onViewClick(ActionEvent event) {
		super.onViewClick(event);
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
		if (((ViewHolderExtended) holder).countValue != null) {
			Count messageCount = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Link.Direction.in);
			((ViewHolderExtended) holder).countValue.setText(String.valueOf(messageCount.count.intValue()));
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