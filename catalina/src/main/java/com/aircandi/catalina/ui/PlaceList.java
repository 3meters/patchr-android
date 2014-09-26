package com.aircandi.catalina.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.StringManager;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Link.Direction;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Integers;

@SuppressWarnings("ucd")
public class PlaceList extends BaseActivity {

	private   EntityListFragment mListFragment;
	protected String             mListLinkType;
	protected Integer            mListTitleResId;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mEntity = EntityManager.getCacheEntity(mEntityId);
			mListLinkType = extras.getString(com.aircandi.Constants.EXTRA_LIST_LINK_TYPE);
			mListTitleResId = extras.getInt(com.aircandi.Constants.EXTRA_LIST_TITLE_RESID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListFragment = new EntityListFragment();
		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.out.name())
		     .setLinkType(mListLinkType)
		     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
		     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListPagingEnabled(true)
		             .setListViewType(ViewType.LIST)
		             .setListLayoutResId(R.layout.place_list_fragment)
		             .setListLoadingResId(R.layout.temp_list_item_loading)
		             .setListItemResId(R.layout.temp_listitem_radar)
		             .setListEmptyMessageResId(R.string.label_watching_empty)
		             .setTitleResId(mListTitleResId)
		             .setButtonSpecialClickable(false)
		             .setSelfBindingEnabled(true);

		setActivityTitle(StringManager.getString(((BaseFragment) mListFragment).getTitleResId()));

		getFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, mListFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}
}