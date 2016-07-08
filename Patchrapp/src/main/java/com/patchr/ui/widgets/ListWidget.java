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
import com.patchr.model.Query;
import com.patchr.model.RealmEntity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.FetchStrategy;
import com.patchr.objects.QueryName;
import com.patchr.objects.QuerySpec;
import com.patchr.service.ProxibaseResponse;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.EndlessRecyclerViewScrollListener;
import com.patchr.ui.components.ListScrollListener;
import com.patchr.ui.components.RealmArrayAdapter;
import com.patchr.utilities.Colors;
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
	private ViewGroup      layout;
	public  RecyclerView   recyclerView;
	private View           emptyMessageView;
	private AirProgressBar progressBar;
	public  View           listGroup;

	public  BusyController  busyController;
	public  EmptyController emptyController;
	private QuerySpec       querySpec;
	private String          contextEntityId;
	private RealmEntity     contextEntity;


	public RealmArrayAdapter         adapter;
	public RealmResults<RealmEntity> entities;
	public Query                     query;
	public boolean                   processingQuery;
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
		this.layoutResId = R.layout.view_entity_list;

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ListWidget, defStyle, 0);

		this.pagingDisabled = ta.getBoolean(R.styleable.ListWidget_pagingDisabled, false);
		this.cacheDisabled = ta.getBoolean(R.styleable.ListWidget_cacheDisabled, false);
		this.fetchOnResumeDisabled = ta.getBoolean(R.styleable.ListWidget_fetchOnResumeDisabled, false);
		this.restartAtTop = ta.getBoolean(R.styleable.ListWidget_restartAtTop, false);

		ta.recycle();

		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		adapter.notifyDataSetChanged();
	}

	public void onStart() {
		Dispatcher.getInstance().register(this);
	}

	public void onResume() {
		if (!fetchOnResumeDisabled || !executed) {
			/* Check if service has something fresher */
			fetch(FetchMode.AUTO);
		}
	}

	public void onStop() {
		Dispatcher.getInstance().unregister(this);
		if (this.subscription != null && !this.subscription.isUnsubscribed()) {
			this.subscription.unsubscribe();
		}
	}

	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {

		/* Restart at top of list */
		if (this.restartAtTop) {
			scrollToTop();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {
		if (this.querySpec != null && this.querySpec.name.equals(QueryName.NotificationsForUser)) {
			adapter.notifyDataSetChanged();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.emptyMessageView = layout.findViewById(R.id.list_message);
		this.progressBar = (AirProgressBar) layout.findViewById(R.id.list_progress);
		this.recyclerView = (RecyclerView) layout.findViewById(R.id.entity_list);
		this.swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.swipe);
		this.listGroup = layout.findViewById(R.id.list_group);

		if (this.swipeRefresh != null) {
			this.swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			this.swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getContext(), R.attr.refreshColorBackground));
			this.swipeRefresh.setOnRefreshListener(this);
			this.swipeRefresh.setRefreshing(false);
			this.swipeRefresh.setEnabled(true);
		}

		this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		this.busyController = new BusyController(this.progressBar, this.swipeRefresh);
		this.emptyController = new EmptyController(this.emptyMessageView);

		if (this.header != null) {
			this.emptyController.positionBelow(this.header, null);
			this.busyController.positionBelow(this.header, null);
		}

		this.recyclerView.addOnScrollListener(new ListScrollListener() {
			@Override public void onMoved(int distance) {
				if (busyController.swipeRefreshLayout != null) {
					busyController.swipeRefreshLayout.setEnabled(distance == 0);
				}
			}
		});
	}

	public void fetch(final FetchMode mode) {
		fetchQueryItems(mode);
	}

	private void fetchQueryItems(final FetchMode mode) {

		if (processingQuery) return;

		processingQuery = true;
		Logger.v(this, "Fetching list entities: " + mode.name().toString());
		final FetchStrategy strategy = (mode != FetchMode.AUTO || !executed) ? FetchStrategy.IgnoreCache : FetchStrategy.UseCacheAndVerify;
		final Integer skip = (mode == FetchMode.PAGING && query.more) ? entities.size() : 0;

		AsyncTask.execute(() -> {
			this.subscription = RestClient.getInstance().fetchListItems(strategy, this.querySpec, this.contextEntityId, skip)
				.doOnTerminate(() -> {
					if (this.busyController != null) {
						this.busyController.hide(true);
					}
				})
				.subscribe(response -> {
					processingQuery = false;
					executed = true;
					fetchQueryItemsComplete(mode, response, skip);
				});
		});
	}

	private void fetchQueryItemsComplete(FetchMode mode, ProxibaseResponse response, Integer skip) {

		/* Add load more trigger if more is available */
		if (query.more && !this.pagingDisabled) {
			recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((LinearLayoutManager) recyclerView.getLayoutManager()) {
				@Override public void onLoadMore(int page, int totalItemsCount) {
					if (!processingQuery) {
						recyclerView.removeOnScrollListener(this);
						fetch(FetchMode.PAGING);
					}
				}
			});
		}

		/* Trigger ui update */
		if (response != null && !response.noop) {
			if (mode == FetchMode.PAGING) {
				Integer positionStart = skip;
				adapter.notifyItemRangeChanged(positionStart, entities.size() - 1);
			}
			else {
				adapter.notifyDataSetChanged();
			}
		}
	}

	public void bind(QuerySpec querySpec, String contextEntityId) {

		synchronized (lock) {

			this.querySpec = querySpec;
			this.contextEntityId = contextEntityId;
			if (contextEntityId != null) {
				this.contextEntity = realm.where(RealmEntity.class).equalTo("id", this.contextEntityId).findFirst();
			}

			this.query = realm.where(Query.class).equalTo("id", querySpec.getId(contextEntityId)).findFirst();
			if (this.query == null) {
				Query realmQuery = new Query();
				realmQuery.id = querySpec.getId(contextEntityId);
				realm.beginTransaction();
				this.query = realm.copyToRealm(realmQuery);
				realm.commitTransaction();
			}

			this.entities = this.query.entities
				.sort(querySpec.sortField, querySpec.sortAscending ? Sort.ASCENDING : Sort.DESCENDING);

			this.entities.addChangeListener(results -> {
				if (this.entities.size() == 0) {
					this.emptyController.show(true);
				}
				else {
					this.emptyController.hide(true);
				}
			});

			/* Both paths will trigger the ui to display any data available in the cache. */
			if (this.adapter == null) {
				this.adapter = new RealmArrayAdapter(getContext(), this.entities);
				this.adapter.header = this.header;
				this.adapter.listItemResId = querySpec.listItemResId;
				this.adapter.contextEntity = this.contextEntity;
				this.recyclerView.setAdapter(this.adapter);
			}
			else {
				this.adapter.header = this.header;
				this.adapter.data = this.entities;
			}
			this.emptyController.setText(StringManager.getString(querySpec.getListEmptyMessage()));
		}
	}

	public void draw() {
		this.adapter.notifyDataSetChanged();
	}

	public RealmResults<RealmEntity> getEntities() {
		return this.entities;
	}

	public void scrollToTop() {
		RecyclerView.LayoutManager layoutManager = this.recyclerView.getLayoutManager();
		if (layoutManager instanceof LinearLayoutManager) {
			((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
		}
	}

	public void setHeader(View header) {
		this.header = header;
		this.emptyController.positionBelow(this.header, null);
		this.busyController.positionBelow(this.header, null);
	}

	public void setRealm(Realm realm) {
		this.realm = realm;
	}
}