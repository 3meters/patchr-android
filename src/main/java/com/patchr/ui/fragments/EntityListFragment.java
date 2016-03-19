package com.patchr.ui.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.events.AbsEntitiesQueryEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.objects.FetchMode;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.ListPresenter;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class EntityListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

	public ListPresenter listPresenter;
	public Integer layoutResId = R.layout.entity_list_fragment;
	public ListPresenter.OnInjectEntitiesHandler injectEntitiesHandler;
	public Integer                               listItemResId;
	public Integer                               emptyMessageResId;
	public AbsEntitiesQueryEvent                 query;
	public View                                  headerView;
	public Boolean entityCacheEnabled = true;            // false == always call service

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.listPresenter = new ListPresenter(getContext());
		this.listPresenter.listItemResId = this.listItemResId;
		this.listPresenter.emptyMessageResId = this.emptyMessageResId;
		this.listPresenter.query = this.query;
		this.listPresenter.headerView = this.headerView;
		this.listPresenter.entityCacheEnabled = this.entityCacheEnabled;
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(layoutResId, container, false);
	}

	@Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onActivityCreated(savedInstanceState);
		initialize(getView());
	}

	@Override public void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
		fetch(FetchMode.AUTO);
	}

	@Override public void onResume() {
		super.onResume();
		if (listPresenter != null) {
			listPresenter.onResume();
		}
	}

	@Override public void onPause() {
		super.onPause();
		if (listPresenter != null) {
			listPresenter.onPause();
		}
	}

	@Override public void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		if (listPresenter != null) {
			listPresenter.refresh();
		}
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		listPresenter.getAdapter().notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onNotificationReceived(final NotificationReceivedEvent event) {

		if (listPresenter.entities.contains(event.notification)) {
			listPresenter.entities.remove(event.notification);
		}
		listPresenter.emptyController.fadeOut();
		listPresenter.adapter.insert(event.notification, 0);
		listPresenter.adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(View view) {

		assert view != null;

		this.listPresenter.listView = (AbsListView) ((ViewGroup) view.findViewById(R.id.swipe)).getChildAt(1);
		this.listPresenter.emptyController = new EmptyController(view.findViewById(R.id.list_message));
		this.listPresenter.busyController = new BusyController();
		this.listPresenter.busyController.setProgressBar(view.findViewById(R.id.list_progress));

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(UI.getResIdForAttribute(getContext(), R.attr.refreshColor)));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getContext(), R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyController.setSwipeRefresh(swipeRefresh);
		}

		this.listPresenter.initialize(getContext(), view);        // We init after everything is setup
	}

	public void fetch(FetchMode fetchMode) {
		listPresenter.fetch(fetchMode);
	}

	@Override public void setMenuVisibility(final boolean visible) {
		/*
		 * Called when fragment is going to be visible to the user and that's when
		 * we want to start the data binding work. CreateView will have already been called.
		 */
		super.setMenuVisibility(visible);
		isVisible = visible;
		if (isVisible) {
			listPresenter.fetch(FetchMode.AUTO);
		}
	}
}