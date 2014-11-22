package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Link.Direction;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Integers;
import com.squareup.otto.Subscribe;

@SuppressWarnings("ucd")
public class PatchList extends BaseActivity {

	protected String  mListLinkType;
	protected Integer mListTitleResId;
	protected Integer mListEmptyMessageResId;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mEntity = EntityManager.getCacheEntity(mEntityId);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mListTitleResId = extras.getInt(Constants.EXTRA_LIST_TITLE_RESID);
			mListEmptyMessageResId = extras.getInt(Constants.EXTRA_LIST_EMPTY_RESID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mCurrentFragment = new EntityListFragment();
		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.out.name())
		     .setLinkType(mListLinkType)
		     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
		     .setSchema(Constants.SCHEMA_ENTITY_PATCH);

		((EntityListFragment) mCurrentFragment).setQuery(query)
		                                       .setMonitor(monitor)
		                                       .setListPagingEnabled(true)
		                                       .setListViewType(ViewType.LIST)
		                                       .setListLayoutResId(R.layout.patch_list_fragment)
		                                       .setListLoadingResId(R.layout.temp_listitem_loading)
		                                       .setListItemResId(R.layout.temp_listitem_patch)
		                                       .setListEmptyMessageResId(mListEmptyMessageResId)
		                                       .setTitleResId(mListTitleResId)
		                                       .setSelfBindingEnabled(true);

		getFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, mCurrentFragment)
				.commit();

		draw(null);
	}

	@Override
	public void draw(View view) {
		setActivityTitle(StringManager.getString(((BaseFragment) mCurrentFragment).getTitleResId()));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {
		mBusy.hideBusy(false);

		final EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
		final Integer count = fragment.getAdapter().getCount();

		((BaseFragment) mCurrentFragment).onProcessingFinished();

		if (mBubbleButton.isEnabled()) {
			if (count == 0) {
				mBubbleButton.setText(fragment.getListEmptyMessageResId());
				mBubbleButton.fadeIn();
			}
			else {
				mBubbleButton.fadeOut();
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}
}