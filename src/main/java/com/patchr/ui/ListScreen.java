package com.patchr.ui;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.StringManager;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.Photo;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.RecyclePresenter;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class ListScreen extends BaseScreen implements SwipeRefreshLayout.OnRefreshListener {
	/*
	 * Thin wrapper around a list fragment.
	 */
	protected String           listLinkType;
	protected String           listLinkSchema;
	protected Integer          listTitleResId;
	protected Integer          listEmptyMessageResId;
	protected Integer          listItemResId;
	protected String           listLinkDirection;
	protected Entity           entity;
	protected String           entityId;
	protected RecyclePresenter listPresenter;

	@Override public void onResume() {
		super.onResume();

		if (!isFinishing()) {
			bind();                             // Shows any data we already have
			fetch(FetchMode.AUTO);              // Checks for data changes and binds again if needed
			if (this.listPresenter != null) {
				this.listPresenter.onResume();  // Update ui
			}
		}
	}

	@Override public void onPause() {
		super.onPause();
		if (this.listPresenter != null) {
			this.listPresenter.onPause();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				navigateToPhoto(photo);
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				navigateToEntity(entity);
			}
		}
	}

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			this.entity = DataController.getStoreEntity(this.entityId);

			this.listItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.listitem_user);
			this.listLinkDirection = extras.getString(Constants.EXTRA_LIST_LINK_DIRECTION, Link.Direction.in.name());
			this.listLinkSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_USER);
			this.listLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			this.listEmptyMessageResId = extras.getInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.empty_general);
			this.listTitleResId = extras.getInt(Constants.EXTRA_LIST_TITLE_RESID);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		assert this.rootView != null;

		if (this.listPresenter == null) {
			this.listPresenter = new RecyclePresenter(this);
		}

		this.listPresenter.recycleView = (RecyclerView) this.rootView.findViewById(R.id.entity_list);
		this.listPresenter.showIndex = false;
		this.listPresenter.listItemResId = this.listItemResId;
		this.listPresenter.busyPresenter = new BusyPresenter();
		this.listPresenter.busyPresenter.setProgressBar(this.rootView.findViewById(R.id.list_progress));
		this.listPresenter.emptyPresenter = new EmptyPresenter(this.rootView.findViewById(R.id.list_message));
		this.listPresenter.emptyPresenter.setLabel(StringManager.getString(this.listEmptyMessageResId));
		this.listPresenter.scopingEntity = this.entity;

		if (this.listPresenter.query == null) {
			this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
					, Maps.asMap("enabled", true)
					, this.listLinkDirection
					, this.listLinkType
					, this.listLinkSchema
					, this.entityId);
		}

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) this.rootView.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyPresenter.setSwipeRefresh(swipeRefresh);
		}

		this.listPresenter.initialize(this, this.rootView);        // We init after everything is setup
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_list;
	}

	public void fetch(final FetchMode fetchMode) {
		listPresenter.fetch(fetchMode);
	}

	public void bind() {
		listPresenter.bind();
	}
}