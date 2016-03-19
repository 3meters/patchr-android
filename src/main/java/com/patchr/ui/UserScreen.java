package com.patchr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

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
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Notification;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.ListPresenter;
import com.patchr.ui.edit.ReportEdit;
import com.patchr.ui.edit.UserEdit;
import com.patchr.ui.views.UserDetailView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Integers;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class UserScreen extends BaseActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

	private UserDetailView header;
	private boolean        bound;
	private ListPresenter  listPresenter;

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override public void onResume() {
		super.onResume();
		if (!isFinishing()) {
			if (this.entity != null) {
				bind();
			}
			fetch(FetchMode.AUTO);
		}
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

	@Override public void onClick(View view) {

		if (view.getId() == R.id.edit_fab) {
			final String jsonEntity = Json.objectToJson(entity);
			startActivity(new Intent(this, UserEdit.class).putExtra(Constants.EXTRA_ENTITY, jsonEntity));
		}
		else if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				if (photo != null) {
					final String jsonPhoto = Json.objectToJson(photo);
					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
					Patchr.router.route(this, Route.PHOTO, null, extras);
				}
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				final Bundle extras = new Bundle();
				extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

				if (entity instanceof Notification) {
					Notification notification = (Notification) entity;
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Entity.getSchemaForId(notification.targetId));
					extras.putString(Constants.EXTRA_ENTITY_ID, notification.targetId);
					extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, notification.parentId);
				}

				Patchr.router.route(this, Route.BROWSE, entity, extras);
			}
			else {
				int titleResId = 0;
				int emptyResId = 0;
				String linkType = (String) view.getTag();

				if (view.getId() == R.id.button_member) {
					titleResId = R.string.label_drawer_item_watch;
					emptyResId = R.string.label_member_of_empty;
				}
				else if (view.getId() == R.id.button_owner) {
					titleResId = R.string.label_drawer_item_create;
					emptyResId = R.string.label_owner_of_empty;
				}

				Bundle extras = new Bundle();
				extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
				extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);
				extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, emptyResId);
				extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

				Patchr.router.route(this, Route.PATCH_LIST, this.entity, extras);
			}
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;
		getMenuInflater().inflate(R.menu.menu_refresh, menu);
		getMenuInflater().inflate(R.menu.menu_sign_out, menu);
		getMenuInflater().inflate(R.menu.menu_report_user, menu);

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.refresh) {
			onRefresh();
		}
		else if (item.getItemId() == R.id.report_user) {
			startActivity(new Intent(this, ReportEdit.class)
					.putExtra(Constants.EXTRA_ENTITY_ID, this.entityId)
					.putExtra(Constants.EXTRA_ENTITY_SCHEMA, this.entity.schema));
		}
		else if (item.getItemId() == R.id.logout) {
			UserManager.shared().signout();
		}

		return true;
	}

	@Override public boolean onPrepareOptionsMenu(Menu menu) {
		configureStandardMenuItems(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	public void onRefresh() {
		fetch(FetchMode.MANUAL);
		if (this.listPresenter != null) {
			if (!isFinishing()) {
				this.listPresenter.refresh();
			}
			else {
				this.listPresenter.busyController.hide(false);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onEntityResult(final EntityQueryResultEvent event) {

		if (event.actionType == ActionType.ACTION_GET_ENTITY) {
			if (event.entity != null && event.entity.id != null && event.entity.id.equals(entityId)) {
				this.entity = event.entity;
				this.listPresenter.scopingEntity = event.entity;
				this.listPresenter.scopingEntityId = event.entity.id;
				bind();
				if (this.optionMenu != null) {
					configureStandardMenuItems(optionMenu);
				}
				this.listPresenter.fetch(FetchMode.AUTO);
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

		this.header = new UserDetailView(this);
		this.header.buttonOwner.setOnClickListener(this);
		this.header.buttonMember.setOnClickListener(this);

		this.listPresenter = new ListPresenter(this);
		this.listPresenter.listView = (AbsListView) ((ViewGroup) this.rootView.findViewById(R.id.swipe)).getChildAt(1);
		this.listPresenter.listItemResId = R.layout.temp_listitem_message;
		this.listPresenter.busyController = new BusyController();
		this.listPresenter.busyController.setProgressBar(this.rootView.findViewById(R.id.list_progress));
		this.listPresenter.emptyController = new EmptyController(this.rootView.findViewById(R.id.list_message));
		this.listPresenter.emptyController.setLabel(StringManager.getString(R.string.label_posted_empty));
		this.listPresenter.headerView = this.header;

		this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
				, Integers.getInteger(R.integer.page_size_messages)
				, Maps.asMap("enabled", true)
				, Link.Direction.out.name()
				, Constants.TYPE_LINK_CREATE
				, Constants.SCHEMA_ENTITY_MESSAGE
				, this.entityId);

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) this.rootView.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(UI.getResIdForAttribute(this, R.attr.refreshColor)));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyController.setSwipeRefresh(swipeRefresh);
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
		assert this.entity != null;
		header.databind(this.entity);
	}
}