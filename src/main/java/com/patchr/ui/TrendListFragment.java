package com.patchr.ui;

import com.patchr.components.Dispatcher;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.TrendRequestEvent;
import com.squareup.otto.Subscribe;

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

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

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
}