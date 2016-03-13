package com.patchr.ui;

import android.os.Bundle;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.Link.Direction;
import com.patchr.ui.EntityListFragment.ViewType;
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.Integers;
import com.squareup.otto.Subscribe;

@SuppressWarnings("ucd")
public class PatchList extends BaseActivity {
	/*
	 * Thin wrapper around a list fragment.
	 */
	protected String  mListLinkType;
	protected Integer mListTitleResId;
	protected Integer mListEmptyMessageResId;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mEntity = DataController.getStoreEntity(mEntityId);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mListTitleResId = extras.getInt(Constants.EXTRA_LIST_TITLE_RESID);
			mListEmptyMessageResId = extras.getInt(Constants.EXTRA_LIST_EMPTY_RESID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mCurrentFragment = new EntityListFragment();

		((EntityListFragment) mCurrentFragment)
				.setScopingEntityId(mEntityId)
				.setLinkSchema(Constants.SCHEMA_ENTITY_PATCH)
				.setLinkType(mListLinkType)
				.setLinkDirection(Direction.out.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_entities))
				.setShowIndex(false)
				.setListPagingEnabled(true)
				.setListViewType(ViewType.LIST)
				.setListLayoutResId(R.layout.patch_list_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListItemResId(R.layout.temp_listitem_patch)
				.setListEmptyMessageResId(mListEmptyMessageResId)
				.setPauseOnFling(false)
				.setTitleResId(mListTitleResId);

		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, mCurrentFragment)
				.commit();

		draw(null);
	}

	@Override
	public void draw(View view) {
		Integer titleResId = ((EntityListFragment) mCurrentFragment).getTitleResId();
		if (titleResId != null) {
			setActivityTitle(StringManager.getString(titleResId));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		/*
		 * Gets called direct at the activity level and receives
		 * events from fragments.
		 */
		mProcessing = false;
		mUiController.getBusyController().hide(false);
	}

	@Override
	public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			((EntityListFragment) mCurrentFragment).onRefresh();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}
}