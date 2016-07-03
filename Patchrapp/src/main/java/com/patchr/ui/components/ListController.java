package com.patchr.ui.components;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.model.RealmEntity;
import com.patchr.model.RealmQueryStatus;
import com.patchr.objects.FetchMode;
import com.patchr.objects.FetchStrategy;
import com.patchr.objects.Query;
import com.patchr.service.ProxibaseResponse;
import com.patchr.service.RestClient;
import com.patchr.utilities.Utils;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Subscription;

public class ListController {

	/* Injection required */
	public RecyclerView    recyclerView;
	public EmptyController emptyController;
	public BusyController  busyController;
	public Query           query;

	public RealmArrayAdapter         adapter;
	public RealmResults<RealmEntity> entities;
	public RealmQueryStatus          queryStatus;
	public boolean                   processingQuery;
	public boolean                   entityCacheDisabled;            // true == always call service
	public Subscription              subscription;
	public Context                   context;
	public boolean                   pagingDisabled;

	public ListController(Context context) {
		this.context = context;
	}

	public void onResume() {
		/* Check if service has something fresher */
		fetch(FetchMode.AUTO);
	}

	public void onStop() {
		if (this.subscription != null && !this.subscription.isUnsubscribed()) {
			this.subscription.unsubscribe();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {

		Utils.guard(this.recyclerView != null, "recyclerView cannot be null");
		Utils.guard(this.emptyController != null, "emptyController cannot be null");
		Utils.guard(this.busyController != null, "busyController cannot be null");
		Utils.guard(this.query != null, "query cannot be null");

		this.emptyController.setText(StringManager.getString(this.query.getListEmptyMessage()));
	}

	public void bind() {

		if (this.adapter != null) {

			this.queryStatus = Realm.getDefaultInstance().where(RealmQueryStatus.class).equalTo("id", this.query.getId()).findFirst();
			if (this.queryStatus == null) {
				RealmQueryStatus realmQuery = new RealmQueryStatus();
				realmQuery.id = this.query.getId();
				this.queryStatus = RealmQueryStatus.copyToRealmOrUpdate(realmQuery);
			}

			this.entities = Realm.getDefaultInstance()
				.where(RealmEntity.class)
				.equalTo(this.query.filterField, this.query.filterValue)
				.equalTo("schema", this.query.schema)
				.findAllSorted(this.query.sortField, this.query.sortAscending ? Sort.ASCENDING : Sort.DESCENDING);

			this.entities.addChangeListener(results -> {
				if (this.entities.size() == 0) {
					this.emptyController.show(true);
				}
				else {
					this.emptyController.hide(true);
				}
			});

			this.adapter = new RealmArrayAdapter(this.context, this.entities);
			this.adapter.listItemResId = this.query.getListItemResId();

			this.recyclerView.setLayoutManager(new LinearLayoutManager(this.context));
			this.recyclerView.setAdapter(this.adapter);
		}
	}

	public void fetch(final FetchMode mode) {
		fetchQueryItems(mode);
	}

	public void fetchQueryItems(final FetchMode mode) {

		if (processingQuery) return;

		processingQuery = true;
		Logger.v(this, "Fetching list entities: " + mode.name().toString());
		final FetchStrategy strategy = (mode != FetchMode.AUTO || !queryStatus.executed) ? FetchStrategy.IgnoreCache : FetchStrategy.UseCacheAndVerify;
		final Integer skip = (mode == FetchMode.PAGING && queryStatus.more) ? entities.size() : 0;

		AsyncTask.execute(() -> {
			this.subscription = RestClient.getInstance().fetchEntities(strategy, this.query, skip)
				.doOnTerminate(() -> {
					if (this.busyController != null) {
						this.busyController.hide(true);
					}
				})
				.subscribe(response -> {
					processingQuery = false;
					fetchQueryItemsComplete(mode, response, skip);
				});
		});
	}

	public void fetchQueryItemsComplete(FetchMode mode, ProxibaseResponse response, Integer skip) {

		if (queryStatus.more && !this.pagingDisabled) {
			recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((LinearLayoutManager) recyclerView.getLayoutManager()) {
				@Override public void onLoadMore(int page, int totalItemsCount) {
					if (!processingQuery) {
						recyclerView.removeOnScrollListener(this);
						fetchQueryItems(FetchMode.PAGING);
					}
				}
			});
		}

		if (response != null && !response.noop) {

			Realm realm = Realm.getDefaultInstance();
			realm.beginTransaction();
			queryStatus.more = (response.more != null && response.more);
			queryStatus.executed = true;
			realm.commitTransaction();
			realm.close();

			if (mode == FetchMode.PAGING) {
				Integer positionStart = skip;
				recyclerView.getAdapter().notifyItemRangeChanged(positionStart, entities.size() - 1);
			}
			else {
				recyclerView.getAdapter().notifyDataSetChanged();
			}
		}
	}
}
