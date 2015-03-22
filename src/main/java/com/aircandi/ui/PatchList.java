package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.DataController;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Link.Direction;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.Integers;

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
				.setMonitorEntityId(mEntityId)
				.setLinkSchema(Constants.SCHEMA_ENTITY_PATCH)
				.setLinkType(mListLinkType)
				.setLinkDirection(Direction.out.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_entities))
				.setListPagingEnabled(true)
				.setListViewType(ViewType.LIST)
				.setListLayoutResId(R.layout.patch_list_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListItemResId(R.layout.temp_listitem_patch)
				.setListEmptyMessageResId(mListEmptyMessageResId)
				.setTitleResId(mListTitleResId);

		getFragmentManager()
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

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}
}