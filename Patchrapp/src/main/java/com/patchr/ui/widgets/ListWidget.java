package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.events.RefreshEvent;
import com.patchr.model.Query;
import com.patchr.model.RealmEntity;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.enums.FetchMode;
import com.patchr.objects.enums.FetchStrategy;
import com.patchr.objects.enums.QueryName;
import com.patchr.service.ProxibaseResponse;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.EndlessRecyclerViewScrollListener;
import com.patchr.ui.components.ListScrollListener;
import com.patchr.ui.components.RealmArrayAdapter;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Subscription;

@SuppressWarnings("ucd")
public class ListWidget extends FrameLayout implements SwipeRefreshLayout.OnRefreshListener {

	private static final Object lock = new Object();

	public SwipeRefreshLayout swipeRefresh;

	/* Inject optional */
	public View    header;
	public boolean pagingDisabled;
	public boolean cacheDisabled;            // true == always call service
	public boolean fetchOnResumeDisabled;
	public boolean restartAtTop;

	private Integer        layoutResId;
	public  RecyclerView   recyclerView;
	public  View           listGroup;

	public  BusyController  busyController;
	public  EmptyController emptyController;
	private QuerySpec       querySpec;
	private String          contextEntityId;
	private RealmEntity     contextEntity;

	public RealmArrayAdapter         adapter;
	public RealmResults<RealmEntity> entities;
	public Query                     query;
	public boolean                   processing;
	public Subscription              subscription;
	public Context                   context;
	public boolean                   executed;
	public Realm                     realm;  // Always on main thread

	public ListWidget(Context context) {
		this(context, null, 0);
	}

	public ListWidget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ListWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		layoutResId = R.layout.view_entity_list;

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ListWidget, defStyle, 0);

		pagingDisabled = ta.getBoolean(R.styleable.ListWidget_pagingDisabled, false);
		cacheDisabled = ta.getBoolean(R.styleable.ListWidget_cacheDisabled, false);
		fetchOnResumeDisabled = ta.getBoolean(R.styleable.ListWidget_fetchOnResumeDisabled, false);
		restartAtTop = ta.getBoolean(R.styleable.ListWidget_restartAtTop, false);

		ta.recycle();

		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		Dispatcher.getInstance().post(new RefreshEvent(FetchMode.MANUAL));
		fetch(FetchMode.MANUAL);
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		adapter.notifyDataSetChanged();
	}

	public void onStart() {
		if (!Dispatcher.getInstance().isRegistered(this)) {
			Dispatcher.getInstance().register(this);
		}
	}

	public void onResume() {
		if (!fetchOnResumeDisabled || !executed) {
			/* Check if service has something fresher */
			fetch(FetchMode.AUTO);
		}
	}

	public void onStop() {
		Dispatcher.getInstance().unregister(this);
		if (subscription != null && !subscription.isUnsubscribed()) {
			subscription.unsubscribe();
		}
	}

	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {

		/* Restart at top of list */
		if (restartAtTop) {
			scrollToTop();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {
		if (querySpec != null && querySpec.name.equals(QueryName.NotificationsForUser)) {
			adapter.notifyDataSetChanged();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		ViewGroup layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(layoutResId, this, true);

		View emptyMessageView = layout.findViewById(R.id.list_message);
		AirProgressBar progressBar = (AirProgressBar) layout.findViewById(R.id.list_progress);
		recyclerView = (RecyclerView) layout.findViewById(R.id.entity_list);
		swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.swipe);
		listGroup = layout.findViewById(R.id.list_group);

		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.addOnScrollListener(new ListScrollListener() {
			@Override public void onMoved(int distance) {
				if (busyController.swipeRefreshLayout != null) {
					busyController.swipeRefreshLayout.setEnabled(distance == 0);
				}
			}
		});

		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getContext(), R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
		}

		busyController = new BusyController(progressBar, swipeRefresh);
		emptyController = new EmptyController(emptyMessageView);

		if (header != null) {
			emptyController.positionBelow(header, null);
			busyController.positionBelow(header, null);
		}
	}

	public void bind(String queryName, String contextEntityId) {

		synchronized (lock) {

			this.querySpec = QuerySpec.Factory(queryName);
			this.contextEntityId = contextEntityId;
			if (contextEntityId != null) {
				contextEntity = realm.where(RealmEntity.class).equalTo("id", contextEntityId).findFirst();
			}

			query = RestClient.getQuery(queryName, contextEntityId);
			entities = query.entities
				.sort(querySpec.sortField, querySpec.sortAscending ? Sort.ASCENDING : Sort.DESCENDING);

			executed = entities.size() > 0;

			entities.addChangeListener(results -> {
				if (entities.size() == 0) {
					emptyController.show(true);
				}
				else {
					emptyController.hide(true);
				}
			});

			/* Both paths will trigger the ui to display any data available in the cache. */
			if (adapter == null) {
				adapter = new RealmArrayAdapter(getContext(), entities);
				adapter.header = header;
				adapter.listItemResId = querySpec.listItemResId;
				adapter.contextEntity = contextEntity;
				recyclerView.setAdapter(adapter);
			}
			else {
				adapter.header = header;
				adapter.data = entities;
			}
			emptyController.setText(StringManager.getString(querySpec.getListEmptyMessage()));
		}
	}

	public void draw() {
		adapter.notifyDataSetChanged();
	}

	public void fetch(final FetchMode mode) {
		fetchQueryItems(mode);
	}

	private void fetchQueryItems(final FetchMode mode) {

		if (!processing) {
			processing = true;
			Logger.v(this, "Fetching list entities: " + mode.name());
			final FetchStrategy strategy = (mode != FetchMode.AUTO || !executed) ? FetchStrategy.IgnoreCache : FetchStrategy.UseCacheAndVerify;
			final Integer skip = (mode == FetchMode.PAGING && query.more) ? entities.size() : 0;

			AsyncTask.execute(() -> subscription = RestClient.getInstance().fetchListItems(strategy, querySpec, contextEntityId, skip)
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);
						executed = true;
						fetchQueryItemsComplete(mode, response, skip);
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(getContext(), error);
					}));
		}
	}

	private void fetchQueryItemsComplete(FetchMode mode, ProxibaseResponse response, Integer skip) {

		/* Add load more trigger if more is available */
		if (query.more && !pagingDisabled) {
			recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((LinearLayoutManager) recyclerView.getLayoutManager()) {
				@Override public void onLoadMore(int page, int totalItemsCount) {
					if (!processing) {
						recyclerView.removeOnScrollListener(this);
						fetch(FetchMode.PAGING);
					}
				}
			});
		}

		/* Trigger ui update */
		if (response != null && !response.noop) {
			if (mode == FetchMode.PAGING) {
				adapter.notifyItemRangeChanged(skip, entities.size() - 1);
			}
			else {
				adapter.notifyDataSetChanged();
			}
		}
	}

	public RealmResults<RealmEntity> getEntities() {
		return entities;
	}

	public void scrollToTop() {
		RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
		if (layoutManager instanceof LinearLayoutManager) {
			((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
		}
	}

	public void setHeader(View header) {
		this.header = header;
		emptyController.positionBelow(header, null);
		busyController.positionBelow(header, null);
	}

	public void setRealm(Realm realm) {
		this.realm = realm;
	}
}