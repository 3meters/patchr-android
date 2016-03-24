package com.patchr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.events.EntityQueryEvent;
import com.patchr.events.EntityQueryResultEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.RecyclePresenter;
import com.patchr.ui.edit.UserEdit;
import com.patchr.ui.views.UserDetailView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class UserScreen extends BaseScreen implements SwipeRefreshLayout.OnRefreshListener {

	private UserDetailView       header;
	private boolean              bound;
	private RecyclePresenter     listPresenter;
	private FloatingActionButton fab;

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override public void onResume() {
		super.onResume();

		bind();
		fetch(FetchMode.AUTO);
		if (this.listPresenter != null) {
			this.listPresenter.onResume();
		}
	}

	@Override public void onPause() {
		super.onPause();
		if (this.listPresenter != null) {
			this.listPresenter.onPause();
		}
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {

		if (view.getId() == R.id.fab) {
			final String jsonEntity = Json.objectToJson(entity);
			startActivity(new Intent(this, UserEdit.class).putExtra(Constants.EXTRA_ENTITY, jsonEntity));
		}
		else if (view.getId() == R.id.member_of_button || view.getId() == R.id.owner_of_button) {
			int titleResId = 0;
			int emptyResId = 0;
			String linkType = (String) view.getTag();

			if (view.getId() == R.id.member_of_button) {
				titleResId = R.string.label_drawer_item_watch;
				emptyResId = R.string.label_profile_member_of_empty;
			}
			else if (view.getId() == R.id.owner_of_button) {
				titleResId = R.string.label_drawer_item_create;
				emptyResId = R.string.label_profile_owner_of_empty;
			}

			Bundle extras = new Bundle();
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_patch);
			extras.putString(Constants.EXTRA_LIST_LINK_DIRECTION, Link.Direction.out.name());
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, emptyResId);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			Patchr.router.route(this, Command.ENTITY_LIST, this.entity, extras);
		}
		else if (view.getTag() != null) {
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

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;

		/* Shown for owner */
		getMenuInflater().inflate(R.menu.menu_logout, menu);    // base

		/* Shown for everyone */
		getMenuInflater().inflate(R.menu.menu_refresh, menu);
		getMenuInflater().inflate(R.menu.menu_report, menu);    // base

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.refresh) {
			onRefresh();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onRefresh() {
		fetch(FetchMode.MANUAL);
	}

	public void onFetchComplete() {
		super.onFetchComplete();
		supportInvalidateOptionsMenu();     // In case user authenticated
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onEntityResult(final EntityQueryResultEvent event) {

		if (event.actionType == ActionType.ACTION_GET_ENTITY) {
			if (event.entity != null && event.entity.id != null && event.entity.id.equals(entityId)) {
				if (event.error != null) {
					onFetchComplete();
					return;
				}

				this.bound = true;

				if (!event.noop) {
					Boolean firstBind = (entity == null);
					this.entity = event.entity;
					this.listPresenter.scopingEntity = event.entity;
					this.listPresenter.scopingEntityId = event.entity.id;
					this.listPresenter.fetch(event.fetchMode); // Next in the chain
				}
				onFetchComplete();
				bind();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		assert this.rootView != null;

		this.fab = (FloatingActionButton) findViewById(R.id.fab);
		this.header = new UserDetailView(this);

		this.listPresenter = new RecyclePresenter(this);
		this.listPresenter.recycleView = (RecyclerView) this.rootView.findViewById(R.id.entity_list);
		this.listPresenter.listItemResId = R.layout.temp_listitem_message;
		this.listPresenter.busyPresenter = new BusyPresenter();
		this.listPresenter.busyPresenter.setProgressBar(this.rootView.findViewById(R.id.list_progress));
		this.listPresenter.emptyPresenter = new EmptyPresenter(this.rootView.findViewById(R.id.list_message));
		this.listPresenter.emptyPresenter.setLabel(StringManager.getString(R.string.empty_posted_messages));
		this.listPresenter.headerView = this.header;

		this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
				, Maps.asMap("enabled", true)
				, Link.Direction.out.name()
				, Constants.TYPE_LINK_CREATE
				, Constants.SCHEMA_ENTITY_MESSAGE
				, this.entityId);

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
		return R.layout.user_screen;
	}

	public void fetch(final FetchMode mode) {
		/*
		 * Called on main thread.
		 */
		Logger.v(this, "Fetching: " + mode.name().toString());

		Boolean currentUser = UserManager.shared().authenticated() && UserManager.currentUser.id.equals(this.entityId);
		Integer linkProfile = currentUser ? LinkSpecType.LINKS_FOR_USER_CURRENT : LinkSpecType.LINKS_FOR_USER;

		EntityQueryEvent request = new EntityQueryEvent();
		request.setLinkProfile(linkProfile)
				.setActionType(ActionType.ACTION_GET_ENTITY)
				.setFetchMode(mode)
				.setEntityId(this.entityId)
				.setTag(System.identityHashCode(this));

		if (this.bound && this.entity != null && mode != FetchMode.MANUAL) {
			request.setCacheStamp(this.entity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	public void bind() {
		if (this.entity != null) {
			header.bind(this.entity);
		}
	}
}