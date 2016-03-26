package com.patchr.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.events.AbsEntitiesQueryEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.objects.FetchMode;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.RecyclePresenter;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Fragment lifecycle
 * <p/>
 * - onAttach (activity may not be fully initialized but fragment has been associated with it)
 * - onCreate
 * - onCreateView
 * - onViewCreated
 * - onActivityCreated (views created, safe to use findById)
 * - onViewStateRestored
 * - onStart (fragment becomes visible)
 * - onResume
 * <p/>
 * - onPause
 * - onStop
 * - onSaveInstanceState
 * - onDestroyView
 * - onDestroy
 * - onDetach
 */
public class EntityListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

	public RecyclePresenter                         listPresenter;
	public AbsEntitiesQueryEvent                    query;
	public RecyclePresenter.OnInjectEntitiesHandler injectEntitiesHandler;
	public View                                     headerView;

	public Integer layoutResId;
	public Integer listItemResId;
	public Integer titleResId;
	public Integer emptyMessageResId;

	public boolean entityCacheDisabled;            // true == always call service
	public boolean fetchOnResumeDisabled;
	public boolean pagingDisabled;
	public boolean restartAtTop;

	@Override public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.layoutResId = R.layout.fragment_entity_list;

		/* Force complete initialization if being recreated by the system */
		boolean recreated = (savedInstanceState != null && !savedInstanceState.isEmpty());
		if (recreated) {
			Intent intent = getActivity().getIntent();
			getActivity().finish();
			startActivity(intent);
		}

		this.listPresenter = new RecyclePresenter(getContext());
		this.listPresenter.listItemResId = this.listItemResId;
		this.listPresenter.emptyMessageResId = this.emptyMessageResId;
		this.listPresenter.query = this.query;
		this.listPresenter.headerView = this.headerView;
		this.listPresenter.entityCacheDisabled = this.entityCacheDisabled;
		this.listPresenter.pagingDisabled = this.pagingDisabled;
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

	@Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);

		/* Restart at top of list */
		if (this.restartAtTop) {
			RecyclerView.LayoutManager layoutManager = this.listPresenter.recycleView.getLayoutManager();
			if (layoutManager instanceof LinearLayoutManager) {
				((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
			}
		}
	}

	@Override public void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override public void onResume() {
		super.onResume();

		bind();                             // Shows any data we already have
		if (!fetchOnResumeDisabled) {
			fetch(FetchMode.AUTO);              // Checks for data changes and binds again if needed
		}
		if (this.listPresenter != null) {
			this.listPresenter.onResume();  // Update ui
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
		fetch(FetchMode.MANUAL);
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		listPresenter.adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onNotificationReceived(final NotificationReceivedEvent event) {
		/*
		 * If list is showing notifications and contains notification entity then replace it.
		 */
		if (listPresenter.entities.contains(event.notification)) {
			listPresenter.entities.remove(event.notification);
		}
		listPresenter.emptyPresenter.hide(true);
		listPresenter.entities.add(0, event.notification);
		listPresenter.adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(View view) {

		assert view != null;

		this.listPresenter.recycleView = (RecyclerView) view.findViewById(R.id.entity_list);
		this.listPresenter.emptyPresenter = new EmptyPresenter(view.findViewById(R.id.list_message));
		this.listPresenter.busyPresenter = new BusyPresenter();
		this.listPresenter.busyPresenter.setProgressBar(view.findViewById(R.id.list_progress));

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getContext(), R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyPresenter.setSwipeRefresh(swipeRefresh);
		}

		this.listPresenter.initialize(getContext(), view);        // We init after everything is setup
	}

	public void fetch(FetchMode fetchMode) {
		if (listPresenter != null) {
			listPresenter.fetch(fetchMode);
		}
	}

	public void bind() {
		if (listPresenter != null) {
			listPresenter.bind();
		}
	}
}