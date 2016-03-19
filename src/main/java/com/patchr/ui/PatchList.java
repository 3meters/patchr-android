package com.patchr.ui;

import android.os.Bundle;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.Entity;
import com.patchr.ui.fragments.EntityListFragment;

import org.greenrobot.eventbus.Subscribe;

@SuppressWarnings("ucd")
public class PatchList extends BaseActivity {
	/*
	 * Thin wrapper around a list fragment.
	 */
	protected String  listLinkType;
	protected Integer listTitleResId;
	protected Integer listEmptyMessageResId;
	private   Entity  entity;
	private   String  entityId;

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			this.entity = DataController.getStoreEntity(this.entityId);
			listLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			listTitleResId = extras.getInt(Constants.EXTRA_LIST_TITLE_RESID);
			listEmptyMessageResId = extras.getInt(Constants.EXTRA_LIST_EMPTY_RESID);
		}
	}

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onProcessingComplete(ProcessingCompleteEvent event) {
		/*
		 * Gets called direct at the activity level and receives
		 * events from fragments.
		 */
		processing = false;
		uiController.getBusyController().hide(false);
	}

	public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		if (currentFragment != null && currentFragment instanceof EntityListFragment) {
			((EntityListFragment) currentFragment).listPresenter.refresh();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		currentFragment = new EntityListFragment();

//		((EntityListFragment) currentFragment).listPresenter
//				//.setScopingEntityId(this.entityId)
//				//.setLinkSchema(Constants.SCHEMA_ENTITY_PATCH)
//				//.setLinkType(listLinkType)
//				//.setLinkDirection(Link.Direction.out.name())
//				//.setPageSize(Integers.getInteger(R.integer.page_size_entities))
//				.setShowIndex(false)
//				.setListLoadingResId(R.layout.temp_listitem_loading)
//				.setListItemResId(R.layout.temp_listitem_patch)
//				.setListEmptyMessageResId(listEmptyMessageResId)
//				.setTitleResId(listTitleResId);

		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, currentFragment)
				.commit();

		draw(null);
	}

	public void draw(View view) {}

	@Override protected int getLayoutId() {
		return R.layout.entity_list;
	}
}