package com.patchr.ui.components;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ViewSwitcher;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.events.AbsEntitiesQueryEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.EntitiesQueryResultEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.utilities.DateTime;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecyclePresenter implements View.OnClickListener {

	public BusyPresenter           busyPresenter;
	public EmptyPresenter          emptyPresenter;
	public OnInjectEntitiesHandler injectEntitiesHandler;

	/* Widgets */
	public  RecyclerView recycleView;        // injected by host
	private View         pagingControl;
	public  View         headerView;
	public  View         footerView;

	/* Resources */
	public Integer listItemResId;
	public Integer emptyMessageResId;

	/* Configuration */
	public String  listViewType;
	public boolean entityCacheDisabled;            // true == always call service
	public Boolean showIndex = true;

	/* Cached for grids */
	public Integer photoWidthPixels;
	public Integer visibleColumns = 1;
	public Integer visibleRows    = 3;

	/* Runtime data */
	public  List<Entity>         entities;
	public  RecyclerView.Adapter adapter;
	public  boolean              empty;           // Used to control busy feedback
	public  boolean              more;            // Show paging control
	public  boolean              bound;
	public  boolean              released;
	public  String               groupTag;
	private Context              context;

	/* Data binding */
	public String                scopingEntityId;     // Used to scope the entity collection
	public Entity                scopingEntity;       // Set after first service query, used to manage cache stamp
	public AbsEntitiesQueryEvent query;

	public RecyclePresenter(Context context) {

		this.listViewType = ViewType.LIST;
		this.entities = new ArrayList<>();
		this.adapter = new SuperArrayAdapter(context, this.entities);
		this.empty = (this.adapter.getItemCount() == 0);
		this.groupTag = String.valueOf(DateTime.nowDate().getTime());
		this.context = context;

		Dispatcher.getInstance().register(this);
	}

	public void onResume() {
		if (this.busyPresenter != null) {
			this.busyPresenter.onResume();
		}
	}

	public void onPause() {
		if (this.busyPresenter != null) {
			this.busyPresenter.onPause();
		}
	}

	public void onDestroy() {
		Dispatcher.getInstance().unregister(this);
		this.released = true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {

		Integer id = view.getId();
		if (id == R.id.paging_button) {
			ViewSwitcher switcher = (ViewSwitcher) view;
			if (switcher.getDisplayedChild() == 0) {
				switcher.setDisplayedChild(1);
				fetchItems(this.entities.size(), this.query.pageSize, FetchMode.MANUAL);
			}
		}
	}

	public void onFetchComplete(final NetworkManager.ResponseCode responseCode) {
		if (this.released) return;
		assert responseCode != null;

		this.busyPresenter.hide(false);

		if (adapter.getItemCount() == 0 && responseCode == NetworkManager.ResponseCode.SUCCESS) {
			this.emptyPresenter.show(true);
		}
		else {
			this.emptyPresenter.hide(true); // Only fades if currently visible
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onEntitiesResult(final EntitiesQueryResultEvent event) {

		if (event.tag.equals(System.identityHashCode(this))
				&& (event.actionType == ActionType.ACTION_GET_ENTITIES
				|| event.actionType == ActionType.ACTION_GET_NOTIFICATIONS
				|| event.actionType == ActionType.ACTION_GET_TREND)) {

			Logger.v(this, "Data result accepted: " + event.actionType.name());

			if (event.entities != null) {

				this.more = event.more;
				if (event.cursor != null && event.cursor.skip == 0) {
					this.entities.clear();
				}

				/* Chance for sub class to inject additional entities */
				if (this.injectEntitiesHandler != null) {
					//this.injectEntitiesHandler.injectEntities(this.adapter, this.query.actionType);
				}

				this.entities.addAll(event.entities);
				Collections.sort(this.entities, new Entity.SortByPositionSortDate());
				bind();
			}

			if (event.scopingEntity != null) {
				this.scopingEntity = event.scopingEntity;
			}

			if (this.pagingControl != null) {
				ViewSwitcher switcher = (ViewSwitcher) this.pagingControl.findViewById(R.id.paging_button);
				if (switcher != null) {
					switcher.setDisplayedChild(0);
				}
			}
			this.empty = (this.adapter == null || this.adapter.getItemCount() == 0);
			if (event.actionType == ActionType.ACTION_GET_ENTITIES) {
				this.bound = true;
			}

			onFetchComplete(NetworkManager.ResponseCode.SUCCESS);
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN) public void onDataError(DataErrorEvent event) {
		if (event.tag.equals(System.identityHashCode(this))) {
			Logger.v(this, "Data error accepted: " + event.actionType.name());
			onFetchComplete(NetworkManager.ResponseCode.FAILED);
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN) public void onDataNoop(DataNoopEvent event) {
		if (event.tag.equals(System.identityHashCode(this))) {
			Logger.v(this, "Data no-op accepted: " + event.actionType.name());
			onFetchComplete(NetworkManager.ResponseCode.SUCCESS);
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN) public void onNotificationReceived(final NotificationReceivedEvent event) {
		/* Refresh the list because something happened with our parent. */
		if ((event.notification.parentId != null && event.notification.parentId.equals(scopingEntityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(scopingEntityId))) {
			fetch(FetchMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(Context context, View view) {

		assert view != null;

		this.recycleView = (RecyclerView) view.findViewById(R.id.entity_list);
		this.pagingControl = LayoutInflater.from(context).inflate(R.layout.temp_listitem_loading, null);
		this.pagingControl.setOnClickListener(this);

		if (this.emptyPresenter != null && this.emptyMessageResId != null) {
			this.emptyPresenter.setLabel(StringManager.getString(this.emptyMessageResId));
		}

		if (this.listViewType.equals(ViewType.GRID)) {
			recycleView.setLayoutManager(new GridLayoutManager(context, 4));
		}

		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (this.recycleView != null) {
			if (this.listViewType.equals(ViewType.LIST)) {
				this.recycleView.setLayoutManager(new LinearLayoutManager(context));
				this.recycleView.setAdapter(this.adapter);
			}
			else if (this.listViewType.equals(ViewType.GRID)) {
				this.recycleView.setLayoutManager(new GridLayoutManager(context, 4));
				this.recycleView.setAdapter(this.adapter);
			}
		}
	}

	public void refresh() {
		/* Called as needed by host */
		fetch(FetchMode.MANUAL);
	}

	public void fetch(final FetchMode mode) {
		/*
		 * Called on resume and externally when a parent entity wants to rebind a related entity list.
		 * If additional entities have been paged in, we include them as part of the request size.
		 */
		Logger.v(this, "Fetching: " + mode.name());

		if (mode == FetchMode.MANUAL) {
			this.entities.clear();
		}

		Integer pageSize = this.query != null ? this.query.pageSize : 50;
		Integer limit = pageSize;
		if (this.entities.size() > 0) {
			limit = (int) Math.ceil((float) this.entities.size() / pageSize) * pageSize;
		}

		fetchItems(0, limit, mode);

		if (mode != FetchMode.MANUAL) {
			this.emptyPresenter.hide(false);
		}

		if (!this.bound) {
			this.busyPresenter.show(BusyPresenter.BusyAction.Refreshing_Empty);
		}
	}

	private void fetchItems(Integer skip, Integer limit, FetchMode fetchMode) {

		this.query.cursor.limit = limit;
		this.query.cursor.skip = ((int) Math.ceil((double) skip / query.pageSize) * query.pageSize);
		this.query.fetchMode = fetchMode;
		this.query.setTag(System.identityHashCode(this));

		if (this.bound && this.scopingEntity != null && fetchMode != FetchMode.MANUAL) {
			this.query.setCacheStamp(this.scopingEntity.getCacheStamp());
		}

		Logger.v(this, "CacheStamp: " + (this.query.cacheStamp != null ? this.query.cacheStamp.toString() : "null"));

		Dispatcher.getInstance().post(this.query);
	}

	public void bind() {
		this.adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	protected class SuperArrayAdapter extends RecyclerView.Adapter<ViewHolder> {

		private List<Entity>   entities;
		private LayoutInflater inflater;

		public SuperArrayAdapter(Context context, List<Entity> entities) {
			this.entities = entities;
			this.inflater = LayoutInflater.from(context);
		}

		@Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(listItemResId, parent, false);

			/* Some fix-up so we don't need additional boilerplate list item layout */
			if (!showIndex) {
				View index = view.findViewById(R.id.index);
				if (index != null) {
					((ViewGroup) index.getParent()).removeView(index);
				}
			}

			return new ViewHolder(view);
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {
			Entity entity = entities.get(position);
			entity.index = position + 1;
			holder.bind(entity, scopingEntity, query);
		}

		@Override public int getItemCount() {
			return entities.size();
		}
	}

	public static class ViewType {
		public static String LIST = "list";
		public static String GRID = "grid";
	}

	public interface OnInjectEntitiesHandler {
		public void injectEntities(ArrayAdapter adapter, ActionType actionType);
	}
}
