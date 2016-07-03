package com.patchr.ui.collections;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.model.RealmEntity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.FetchStrategy;
import com.patchr.objects.Photo;
import com.patchr.objects.Query;
import com.patchr.service.RestClient;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.ListController;
import com.patchr.ui.views.BaseView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.realm.Realm;
import rx.Subscription;

@SuppressWarnings("ucd")
public class BaseListScreen extends BaseScreen implements AppBarLayout.OnOffsetChangedListener, SwipeRefreshLayout.OnRefreshListener {

	public    RecyclerView       recyclerView;
	protected SwipeRefreshLayout swipeRefresh;
	protected View               emptyMessageView;
	public    Query              query;

	protected ListController listController;

	public BaseView     header;
	public Subscription subscription;
	public boolean      boundHeader;

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override public void onResume() {
		super.onResume();
		if (this.appBarLayout != null) {
			this.appBarLayout.addOnOffsetChangedListener(this);
		}
		listController.onResume();

		/* Shows what we have cached before starting any fetch logic */
		bind();
	}

	@Override protected void onPause() {
		super.onPause();
		if (this.appBarLayout != null) {
			this.appBarLayout.removeOnOffsetChangedListener(this);
		}
	}

	@Override protected void onStop() {
		super.onStop();
		listController.onStop();
		Dispatcher.getInstance().unregister(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNotificationReceived(final NotificationReceivedEvent event) {
		/* Refresh the list because something happened with the list parent. */
		if ((event.notification.parentId != null && event.notification.parentId.equals(this.entity.id))
			|| (event.notification.targetId != null && event.notification.targetId.equals(this.entity.id))) {
			fetch(FetchMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
	}

	public void onClick(View view) {
		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				navigateToPhoto(photo);
			}
			else if (view.getTag() instanceof RealmEntity) {
				final RealmEntity entity = (RealmEntity) view.getTag();
				navigateToEntity(entity);
			}
		}
	}

	@Override public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
		if (this.swipeRefresh != null) {
			this.swipeRefresh.setEnabled(i == 0);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		Utils.guard(this.rootView != null, "Root view cannot be null");
		Utils.guard(this.query != null, "Query cannot be null");

		this.emptyMessageView = findViewById(R.id.list_message);
		this.recyclerView = (RecyclerView) findViewById(R.id.entity_list);
		this.swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe);

		if (this.swipeRefresh != null) {
			this.swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			this.swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			this.swipeRefresh.setOnRefreshListener(this);
			this.swipeRefresh.setRefreshing(false);
			this.swipeRefresh.setEnabled(true);

			/* Override */
			this.busyController = new BusyController(this.progressBar, this.swipeRefresh);
		}

		this.listController = new ListController(this);
		this.listController.emptyController = new EmptyController(emptyMessageView);
		this.listController.busyController = this.busyController;
		this.listController.recyclerView = this.recyclerView;
		this.listController.query = this.query;

		this.listController.initialize();

		if (this.boundHeader) {
			this.entity = Realm.getDefaultInstance().where(RealmEntity.class).equalTo("id", this.entityId).findFirst();
			this.listController.adapter.header = this.header;
			this.listController.emptyController.positionBelow(this.header, null);
			this.listController.busyController.positionBelow(this.header, null);
		}
	}

	public void fetch(final FetchMode mode) {

		if (!this.boundHeader) {
			listController.fetch(mode);
		}
		else {
			if (processing) return;

			processing = true;
			Logger.v(this, "Fetching form entity: " + mode.name().toString());
			FetchStrategy strategy = (mode == FetchMode.MANUAL) ? FetchStrategy.IgnoreCache : FetchStrategy.UseCacheAndVerify;

			AsyncTask.execute(() -> {
				this.subscription = RestClient.getInstance().fetchEntity(this.entityId, strategy)
					.doOnTerminate(() -> {
						if (this.busyController != null) {
							this.busyController.hide(true);
						}
					})
					.subscribe(response -> {
						processing = false;
						if (!response.noop || mode == FetchMode.MANUAL) {
							listController.fetchQueryItems(mode);
						}
						else {
							listController.fetchQueryItemsComplete(mode, null, 0);
						}
						if (this.entity == null) {
							this.entity = Realm.getDefaultInstance().where(RealmEntity.class).equalTo("id", this.entityId).findFirst();
							bind();
							supportInvalidateOptionsMenu();     // In case user authenticated
						}
					});
			});
		}
	}

	public void bind() {
		this.listController.bind();
		if (this.boundHeader && this.entity != null) {
			this.header.bind(this.entity);
			this.entity.removeChangeListeners();
			this.entity.addChangeListener(user -> {
				header.invalidate();
			});
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_list;
	}
}