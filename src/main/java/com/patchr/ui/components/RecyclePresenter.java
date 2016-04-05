package com.patchr.ui.components;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.events.AbsEntitiesQueryEvent;
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
import java.util.Map;

public class RecyclePresenter {

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
	public Integer headerResId;
	public Integer emptyMessageResId;

	/* Configuration */
	public String  listViewType;
	public boolean entityCacheDisabled;            // true == always call service
	public boolean pagingDisabled;
	public Map     options;
	public Boolean showIndex = true;

	/* Cached for grids */
	public Integer photoWidthPixels;
	public Integer visibleColumns = 1;
	public Integer visibleRows    = 3;

	/* Runtime data */
	public  List<Entity>         entities;
	public  RecyclerView.Adapter adapter;
	public  boolean              more;            // Show paging control
	public  boolean              released;
	public  boolean              bound;
	public  String               groupTag;
	private Context              context;
	private boolean              processing;

	/* Data binding */
	public String                scopingEntityId;     // Used to scope the entity collection
	public Entity                scopingEntity;       // Set after first service query, used to manage cache stamp
	public AbsEntitiesQueryEvent query;

	public RecyclePresenter(Context context) {

		this.listViewType = ViewType.LIST;
		this.entities = new ArrayList<>();
		this.adapter = new SuperArrayAdapter(context, this.entities);
		this.groupTag = String.valueOf(DateTime.nowDate().getTime());
		this.context = context;

		Dispatcher.getInstance().register(this);
	}

	public void onDestroy() {
		Dispatcher.getInstance().unregister(this);
		this.released = true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onFetchComplete(final NetworkManager.ResponseCode responseCode) {
		if (this.released) return;

		this.processing = false;
		this.busyPresenter.hide(false);
		if (this.entities.size() == 0 && responseCode == NetworkManager.ResponseCode.SUCCESS) {
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

			if (event.error != null) {
				onFetchComplete(NetworkManager.ResponseCode.FAILED);
				return;
			}

			this.bound = true;

			if (!event.noop) {

				Logger.v(this, "Data result accepted: " + event.actionType.name());

				if (event.entities != null) {

					this.more = event.more;

					if (this.more && !this.pagingDisabled) {
						this.recycleView.addOnScrollListener(new EndlessRecyclerViewScrollListener((LinearLayoutManager) recycleView.getLayoutManager()) {
							@Override public void onLoadMore(int page, int totalItemsCount) {
								if (!processing) {
									recycleView.removeOnScrollListener(this);
									fetch(FetchMode.PAGING);
								}
							}
						});
					}

					if (event.fetchMode == FetchMode.PAGING) {
						Integer positionStart = adapter.getItemCount();
						this.entities.addAll(event.entities);
						adapter.notifyItemRangeChanged(positionStart, event.entities.size() - 1);
					}
					else {
						this.entities.clear();
						if (event.cursor != null && event.cursor.skip == 0) {
							if (this.injectEntitiesHandler != null) {
								this.injectEntitiesHandler.injectEntities(this.entities, event.actionType);
							}
						}
						this.entities.addAll(event.entities);
						Collections.sort(this.entities, new Entity.SortByPositionSortDate());

						if (event.scopingEntity != null) {
							this.scopingEntity = event.scopingEntity;
						}

						bind();
					}
				}
			}

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

		if (this.emptyPresenter != null && this.emptyMessageResId != null) {
			this.emptyPresenter.setLabel(StringManager.getString(this.emptyMessageResId));
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

	public void fetch(final FetchMode fetchMode) {
		if (this.processing) return;
		if (fetchMode == FetchMode.AUTO && this.bound) return;

		Logger.v(this, "Fetching: " + fetchMode.name());

		this.processing = true;
		this.query.cursor.skip = (fetchMode == FetchMode.PAGING && this.more) ? this.entities.size() : 0;
		this.query.tag = System.identityHashCode(this);
		this.query.fetchMode = fetchMode;

		Dispatcher.getInstance().post(this.query);

		if (this.entities.size() == 0) {
			this.busyPresenter.show(BusyPresenter.BusyAction.Refreshing_Empty);
		}
	}

	public void bind() {
		this.adapter.notifyDataSetChanged();
	}

	public void clear() {
		this.busyPresenter.hide(false);
		if (!this.entities.isEmpty()) {
			this.entities.clear();
			this.adapter.notifyDataSetChanged();
		}
		this.emptyPresenter.show(true);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	protected class SuperArrayAdapter extends RecyclerView.Adapter<ViewHolder> {

		private static final int TYPE_HEADER = 0;
		private static final int TYPE_ITEM   = 1;
		private static final int TYPE_FOOTER = 2;

		private List<Entity>   entities;
		private LayoutInflater inflater;

		public SuperArrayAdapter(Context context, List<Entity> entities) {
			this.entities = entities;
			this.inflater = LayoutInflater.from(context);
			setHasStableIds(true);
		}

		@Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			if (viewType == TYPE_HEADER) {
				if (headerView.getParent() != null) {
					((ViewGroup) headerView.getParent()).removeView(headerView);
				}
				return new ViewHolder(headerView);
			}
			else if (viewType == TYPE_FOOTER) {
				View view = inflater.inflate(R.layout.listitem_loading, parent, false);
				return new ViewHolder(view);
			}
			else {
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
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {
			int itemType = getItemViewType(position);
			if (itemType == TYPE_FOOTER) {
				ProgressBar progress = (ProgressBar) holder.entityView;
				if (progress != null) {
					progress.setIndeterminate(true);
				}
			}
			else if (itemType == TYPE_ITEM) {
				Entity entity = getItem(position);
				entity.index = getIndex(position);
				holder.bind(entity, scopingEntity, options);
			}
		}

		@Override public int getItemCount() {
			int itemCount = entities.size();
			if (headerView != null) {
				itemCount++;
			}
			if (!pagingDisabled && more) {
				itemCount++;
			}
			return itemCount;
		}

		@Override public long getItemId(int position) {
			if (headerView != null) {
				if (position == 0) {
					return 1000;
				}
				else if (!pagingDisabled && more && position == entities.size() + 1) {
					return 2000;
				}
			}
			else if (!pagingDisabled && more && position == entities.size()) {
				return 2000;
			}

			Entity entity = getItem(position);
			return entity.idAsLong();
		}

		@Override public int getItemViewType(int position) {
			if (headerView != null) {
				if (position == 0) {
					return TYPE_HEADER;
				}
				else if (!pagingDisabled && more && position == entities.size() + 1) {
					return TYPE_FOOTER;
				}
			}
			else if (!pagingDisabled && more && position == entities.size()) {
				return TYPE_FOOTER;
			}
			return TYPE_ITEM;
		}

		private Entity getItem(int position) {
			int dataPosition = position;
			if (headerView != null) {
				dataPosition--;
			}
			return entities.get(dataPosition);
		}

		private Integer getIndex(int position) {
			if (headerView != null) {
				return position;
			}
			else {
				return position + 1;
			}
		}
	}

	public static class ViewType {
		public static String LIST = "list";
		public static String GRID = "grid";
	}

	public interface OnInjectEntitiesHandler {
		public void injectEntities(List<Entity> entities, ActionType actionType);
	}
}
