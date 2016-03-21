package com.patchr.ui.components;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.events.AbsEntitiesQueryEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.EntitiesQueryResultEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.interfaces.IBusy;
import com.patchr.objects.ActionType;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.NotificationView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.views.UserView;
import com.patchr.utilities.DateTime;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class ListPresenter implements View.OnClickListener {

	public BusyPresenter           busyPresenter;
	public EmptyPresenter          emptyPresenter;
	public OnInjectEntitiesHandler injectEntitiesHandler;

	/* Widgets */
	public  AbsListView listView;        // injected by host
	private View        pagingControl;
	public  View        headerView;
	public  View        footerView;

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
	public  List<Entity> entities;
	public  ArrayAdapter adapter;
	public  boolean      empty;           // Used to control busy feedback
	public  boolean      more;            // Show paging control
	public  boolean      bound;
	public  boolean      released;
	public  String       groupTag;
	private Context      context;

	/* Data binding */
	public String                scopingEntityId;     // Used to scope the entity collection
	public Entity                scopingEntity;       // Set after first service query, used to manage cache stamp
	public AbsEntitiesQueryEvent query;

	public ListPresenter(Context context) {

		this.listViewType = ViewType.LIST;
		this.entities = new ArrayList<>();
		this.adapter = new SuperArrayAdapter(context, this.entities);
		this.empty = (this.adapter.getCount() == 0);
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

		if (adapter.getCount() == 0 && responseCode == NetworkManager.ResponseCode.SUCCESS) {
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
					this.adapter.setNotifyOnChange(false);
					this.adapter.clear();
					this.adapter.setNotifyOnChange(true);
				}

				/* Chance for sub class to inject additional entities */
				if (this.injectEntitiesHandler != null) {
					this.injectEntitiesHandler.injectEntities(this.adapter, this.query.actionType);
				}

				for (Entity entity : event.entities) {
					this.adapter.add(entity);
				}

				this.adapter.sort(new Entity.SortByPositionSortDate());
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
			this.empty = (this.adapter == null || this.adapter.getCount() == 0);
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

		this.listView = (AbsListView) ((ViewGroup) view.findViewById(R.id.swipe)).getChildAt(1);

		this.pagingControl = LayoutInflater.from(context).inflate(R.layout.temp_listitem_loading, null);
		this.pagingControl.setOnClickListener(this);

		if (this.emptyPresenter != null && this.emptyMessageResId != null) {
			this.emptyPresenter.setLabel(StringManager.getString(this.emptyMessageResId));
		}

		if (this.headerView != null && this.listView != null && this.listViewType.equals(ViewType.LIST)) {
			((ListView) this.listView).addHeaderView(this.headerView);
		}

		if (this.footerView != null && this.listView != null && this.listViewType.equals(ViewType.LIST)) {
			((ListView) this.listView).addFooterView(this.footerView);
		}

		if (this.listViewType.equals(ViewType.GRID)) {

			GridView gridView = (GridView) this.listView;
			Resources resources = context.getResources();

			/* Set spacing */
			Integer requestedHorizontalSpacing = resources.getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
			Integer requestedVerticalSpacing = resources.getDimensionPixelSize(R.dimen.grid_spacing_vertical);
			gridView.setHorizontalSpacing(requestedHorizontalSpacing);
			gridView.setVerticalSpacing(requestedVerticalSpacing);

			/* Stash some sizing info */
			final DisplayMetrics metrics = resources.getDisplayMetrics();
			final Integer availableWidth = metrics.widthPixels - gridView.getPaddingLeft() - gridView.getPaddingRight();
			final Integer availableHeight = metrics.heightPixels - gridView.getPaddingTop() - gridView.getPaddingBottom();

			Integer requestedColumnWidth = resources.getDimensionPixelSize(R.dimen.grid_column_width_requested_large);

			this.visibleColumns = (availableWidth + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
			if (this.visibleColumns <= 0) {
				this.visibleColumns = 1;
			}

			this.visibleRows = (availableHeight + requestedVerticalSpacing) / (requestedColumnWidth + requestedVerticalSpacing);
			if (this.visibleRows <= 0) {
				this.visibleRows = 1;
			}

			int spaceLeftOver = availableWidth - (this.visibleColumns * requestedColumnWidth) - ((this.visibleColumns - 1) * requestedHorizontalSpacing);

			this.photoWidthPixels = requestedColumnWidth + spaceLeftOver / this.visibleColumns;
			gridView.setColumnWidth(this.photoWidthPixels);
			final AbsListView.LayoutParams params = new AbsListView.LayoutParams(this.photoWidthPixels, this.photoWidthPixels - 10);
			this.pagingControl.setLayoutParams(params);
		}

		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (this.listView != null) {
			if (this.listViewType.equals(ViewType.LIST)) {
				((ListView) this.listView).setAdapter(this.adapter);
			}
			else if (this.listViewType.equals(ViewType.GRID)) {
				((GridView) this.listView).setAdapter(this.adapter);
			}
		}

		this.listView.invalidate();
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
			this.adapter.setNotifyOnChange(false);
			this.adapter.clear();
			this.adapter.setNotifyOnChange(true);
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
			this.busyPresenter.show(IBusy.BusyAction.Refreshing_Empty);
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
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public ArrayAdapter getAdapter() {
		return adapter;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	protected class SuperArrayAdapter extends ArrayAdapter<Entity> {

		private Context context;

		public SuperArrayAdapter(Context context, List<Entity> entities) {
			super(context, 0, entities);
			this.context = context;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {

			if (more && position == entities.size() && entities.size() > 0) return pagingControl;

			View view = convertView;

			if (position >= entities.size() && position < (visibleColumns * visibleRows)) {
				/*
				 * Make the widget used to request more list items.
				 */
				if (view == null || view.findViewById(R.id.item_placeholder) == null) {
					view = LayoutInflater.from(this.context).inflate(R.layout.temp_listitem_empty, null);
					if (listView instanceof GridView) {
						final AbsListView.LayoutParams params = new AbsListView.LayoutParams(photoWidthPixels, photoWidthPixels - (10 / 2));
						view.findViewById(R.id.item_placeholder).setLayoutParams(params);
					}
				}
				view.setTag(null);
			}
			else {

				Entity entity = entities.get(position);

				/* Perform cache lookup to make sure we are using the latest */
				if (entityCacheDisabled && DataController.getStoreEntity(entity.id) != null) {
					entity = DataController.getStoreEntity(entity.id);
				}

				entity.index = position + 1;

                /*
                 * Holder is created and bound to view elements by the controller in bindListItem.
                 */
				if (view == null
						|| view.findViewById(R.id.paging_button) != null
						|| view.findViewById(R.id.item_placeholder) != null) {

					view = LayoutInflater.from(this.context).inflate(listItemResId, null);

					/* Some fix-up so we don't need additional boilerplate list item layout */
					if (!showIndex) {
						View index = view.findViewById(R.id.index);
						if (index != null) {
							((ViewGroup) index.getParent()).removeView(index);
						}
					}
				}

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
					PatchView patchView = (PatchView) view.findViewById(R.id.item_view);
					patchView.setTag(entity);
					patchView.databind(entity);
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
					MessageView messageView = (MessageView) view.findViewById(R.id.item_view);
					messageView.setTag(entity);
					messageView.databind(entity);
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
					NotificationView notificationView = (NotificationView) view.findViewById(R.id.item_view);
					notificationView.setTag(entity);
					notificationView.databind(entity);
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
					UserView userView = (UserView) view.findViewById(R.id.item_view);
					userView.setTag(entity);
					if (query.cursor.linkTypes.get(0).equals(Constants.TYPE_LINK_MEMBER)) {
						if (scopingEntity != null) {
							Boolean itemIsOwner = (entity.id.equals(scopingEntity.ownerId));
							userView.databind(entity, !itemIsOwner, itemIsOwner);
							if (scopingEntity instanceof Entity) {
								userView.patch = scopingEntity;
							}
							return view;
						}
					}
					userView.databind(entity);
				}
			}
			return view;
		}

		@Override public int getCount() {
			if (more)
				return entities.size() + 1;
			else if (listViewType.equals(ViewType.GRID)) {
				if (entities.size() == 0) return 0;
				return Math.max(entities.size(), visibleColumns * visibleRows);
			}
			else {
				return entities.size();
			}
		}

		@Override public Entity getItem(int position) {
			return entities.get(position);
		}

		@Override public boolean areAllItemsEnabled() {
			return false;
		}

		@Override public boolean isEnabled(int position) {
			return false;
		}

		public List<Entity> getItems() {
			return entities;
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
