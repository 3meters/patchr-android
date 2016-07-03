package com.patchr.ui.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.patchr.objects.FetchMode;
import com.patchr.objects.Query;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.ListController;
import com.patchr.ui.components.ListScrollListener;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;

/**
 * Fragment lifecycle
 * <p>
 * - onAttach (activity may not be fully initialized but fragment has been associated with it)
 * - onCreate
 * - onCreateView
 * - onViewCreated
 * - onActivityCreated (views created, safe to use findById)
 * - onViewStateRestored
 * - onStart (fragment becomes visible)
 * - onResume
 * <p>
 * - onPause
 * - onStop
 * - onSaveInstanceState
 * - onDestroyView
 * - onDestroy
 * - onDetach
 */
public class EntityListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

	public    RecyclerView       recyclerView;
	protected SwipeRefreshLayout swipeRefresh;
	protected View               emptyMessageView;
	protected AirProgressBar     progressBar;

	public    ListController listController;
	protected BusyController busyController;

	/* Inject required */
	public Query query;

	/* Inject optional */
	public View    header;
	public Integer listTitleResId;
	public Integer layoutResId = R.layout.fragment_entity_list;
	public boolean pagingDisabled;
	public boolean entityCacheDisabled;            // true == always call service
	public boolean fetchOnResumeDisabled;
	public boolean restartAtTop;

	@Override public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Force complete initialization if being recreated by the system */
		boolean recreated = (this.listController == null || this.query == null || (savedInstanceState != null && !savedInstanceState.isEmpty()));
		if (recreated) {
			getActivity().finish();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(layoutResId, container, false);
	}

	@Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onActivityCreated(savedInstanceState);
		View view = getView();
		if (view != null) {
			initialize(view);
		}
	}

	@Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);

		/* Restart at top of list */
		if (this.restartAtTop) {
			RecyclerView.LayoutManager layoutManager = this.listController.recyclerView.getLayoutManager();
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
		if (!fetchOnResumeDisabled) {
			listController.onResume();
		}
	}

	@Override public void onStop() {
		super.onStop();
		listController.onStop();
		Dispatcher.getInstance().unregister(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		listController.fetch(FetchMode.MANUAL);
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		listController.adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(@NonNull View view) {

		if (this.listController == null) {

			this.emptyMessageView = view.findViewById(R.id.list_message);
			this.progressBar = (AirProgressBar) view.findViewById(R.id.list_progress);
			this.recyclerView = (RecyclerView) view.findViewById(R.id.entity_list);
			this.swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);

			if (this.swipeRefresh != null) {
				this.swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
				this.swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getContext(), R.attr.refreshColorBackground));
				this.swipeRefresh.setOnRefreshListener(this);
				this.swipeRefresh.setRefreshing(false);
				this.swipeRefresh.setEnabled(true);
				this.busyController = new BusyController(this.progressBar, this.swipeRefresh);
			}

			this.listController = new ListController(getContext());
			this.listController.emptyController = new EmptyController(this.emptyMessageView);
			this.listController.busyController = this.busyController;
			this.listController.recyclerView = this.recyclerView;
			this.listController.query = this.query;
			this.listController.entityCacheDisabled = this.entityCacheDisabled;

			this.listController.initialize();

			this.listController.recyclerView.addOnScrollListener(new ListScrollListener() {
				@Override public void onMoved(int distance) {
					if (busyController.swipeRefreshLayout != null) {
						busyController.swipeRefreshLayout.setEnabled(distance == 0);
					}
				}
			});

			this.listController.adapter.header = this.header;
		}
	}
}