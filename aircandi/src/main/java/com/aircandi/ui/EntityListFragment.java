package com.aircandi.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.controllers.ViewHolder;
import com.aircandi.events.EntitiesLoadedEvent;
import com.aircandi.monitors.IMonitor;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.queries.IQuery;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirListView;
import com.aircandi.ui.widgets.AirListView.DragDirection;
import com.aircandi.ui.widgets.AirListView.DragEvent;
import com.aircandi.ui.widgets.AirListView.OnDragListener;
import com.aircandi.ui.widgets.AirSwipeRefreshLayout;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityListFragment extends BaseFragment implements OnClickListener {

	/* Widgets */
	protected AbsListView mListView;
	protected View        mLoadingView;
	protected View        mHeaderView;
	protected View        mHeaderCandiView;                                        // NO_UCD (unused code)
	protected View        mFooterView;
	protected View        mFooterHolder;

	/* Resources */
	protected Integer mHeaderViewResId;
	protected Integer mFooterViewResId;
	protected Integer mBackgroundResId;
	protected Integer mListItemResId;
	protected Integer mListLayoutResId = R.layout.entity_list_fragment;
	protected Integer mListLoadingResId;
	protected Integer mListButtonMessageResId;
	protected Integer mListEmptyMessageResId;

	/* Configuration */
	protected String mListViewType;
	protected Boolean mListPagingEnabled  = true;
	protected Boolean mParallaxHeader     = false;
	protected Boolean mEntityCacheEnabled = true;
	protected Boolean mFooterHolderHidden = false;
	protected Boolean mFooterHolderLocked = false;
	protected Boolean mReverseSort        = false;

	/* Runtime data */
	protected Integer mPhotoWidthPixels;
	protected Integer mVisibleColumns = 1;
	protected Integer mVisibleRows    = 3;
	protected Integer mTopOffset;                                            // NO_UCD (unused code)
	protected Integer mLastViewedPosition;                                    // NO_UCD (unused code)
	protected Boolean mFirstBind = true;

	/* Data binding */
	protected List<Entity>           mEntities          = new ArrayList<Entity>();
	protected Map<String, Highlight> mHighlightEntities = new HashMap<String, Highlight>();
	protected IQuery        mQuery;
	protected SimpleMonitor mMonitor;
	protected ListAdapter   mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * If the bundle includes state then we know the fragment is being completely
		 * recreated. Rather than try to restore all the state that was passed in
		 * using settings when originally created, just bail.
		 */
		if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
			Intent intent = getActivity().getIntent();
			getActivity().finish();
			startActivity(intent);
		}
		else {
			mAdapter = getAdapter();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		/*
		 * The listview scroll position seems to be preserved between destroy view
		 * and create view. Probably falls into the bucket of view properties that are
		 * auto restored by android.
		 */
		restoreState(savedInstanceState);

		if (view == null) return view;

		if (mListItemResId == null) {
			throw new IllegalArgumentException("List item resource is required by EntityListFragment");
		}

		mLoaded = false;
		mFirstBind = true;
		mListView = (AbsListView) view.findViewById(R.id.list);

		if (mListLoadingResId != null) {
			mLoadingView = LayoutInflater.from(getActivity()).inflate(mListLoadingResId, null);
		}

		if (mButtonSpecial != null) {
			if (mButtonSpecialEnabled && mListButtonMessageResId != null) {
				mButtonSpecial.setText(StringManager.getString(mListButtonMessageResId));
			}
		}

		if (mHeaderViewResId != null && mListView != null && mListViewType.equals(ViewType.LIST)) {
			mHeaderView = inflater.inflate(mHeaderViewResId, mListView, false);
			if (mParallaxHeader) {
				((AirListView) mListView).addParallaxedHeaderView(mHeaderView);
			}
			else {
				((ListView) mListView).addHeaderView(mHeaderView);
			}
		}

		if (mFooterViewResId != null && mListView != null && mListViewType.equals(ViewType.LIST)) {
			mFooterView = inflater.inflate(mFooterViewResId, mListView, false);
			((ListView) mListView).addFooterView(mFooterView);
		}

		if (mListViewType.equals(ViewType.GRID)) {
			gridSizing();
			Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
			final AbsListView.LayoutParams params = new AbsListView.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
			mLoadingView.setLayoutParams(params);
		}

		mFooterHolder = getActivity().findViewById(R.id.footer_holder);
		if (mFooterHolder != null) {

			((AirListView) mListView).setDragListener(new OnDragListener() {

				@Override
				public boolean onDragEvent(DragEvent event, Float dragX, Float dragY) {

					if (event == DragEvent.DRAG) {
						handleFooter(true, null, 200);
					}
					return false;
				}
			});
		}

		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (mListView != null) {
			if (mListViewType.equals(ViewType.LIST)) {
				((ListView) mListView).setAdapter(mAdapter);
			}
			else if (mListViewType.equals(ViewType.GRID)) {
				((GridView) mListView).setAdapter(mAdapter);
			}
		}

		/* Hookup swipe refresh */
		final AirSwipeRefreshLayout swipeRefreshLayout = (AirSwipeRefreshLayout) view.findViewById(R.id.swipe);

		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.setColorSchemeResources(R.color.brand_progress_bar_color
					, R.color.brand_progress_bar_color
					, R.color.brand_progress_bar_color
					, R.color.brand_progress_bar_color);

			swipeRefreshLayout.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (mFooterHolder != null) {
							handleFooter(false, true, 500);
						}
					}
					return false;
				}
			});

			swipeRefreshLayout.setOnRefreshListener(new AirSwipeRefreshLayout.OnRefreshListener() {

				@Override
				public void onRefresh() {
					swipeRefreshLayout.setRefreshing(false);
					EntityListFragment.this.onRefresh();
					if (mFooterHolder != null) {
						handleFooter(false, true, 500);
					}
				}
			});

			//mBusy.setSwipeRefreshLayout(swipeRefreshLayout);
		}

		return view;
	}

	@Override
	protected void preBind() {
	}

	@Override
	public void bind(final BindingMode mode) {

		/*
		 * Guard binding if this is a private place and user isn't approved
		 */
		Entity entity = ((BaseActivity) getActivity()).getEntity();
		if (entity instanceof Place) {
			if (!entity.visibleToCurrentUser()) {
				return;
			}
		}

		Logger.d(this, "Binding called: mode = " + mode.name().toLowerCase(Locale.US));
		/*
		 * Gets called by onResume and setMenuVisibility = true
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				preBind();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncBindEntityList");
				ModelResult result = new ModelResult();
				if (mode == BindingMode.MANUAL
						|| (mEntities != null && mEntities.size() == 0)
						|| (mMonitor.isChanged() && mMonitor.activity)) {
					mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);

					Integer limit = null;

					if (mode == BindingMode.MANUAL && mEntities != null && mEntities.size() > 0) {
						Integer pageSize = mQuery.getPageSize();
						limit = (int) Math.ceil((float) mEntities.size() / pageSize) * pageSize;
					}
					result = mQuery.execute(0, limit);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {

						mEntities.clear();
						mAdapter.setNotifyOnChange(false);
						mAdapter.clear();

						for (Entity entity : (List<Entity>) result.data) {
							/*
							 * Special case: skip broken applinks
							 */
							if (entity instanceof Applink) {
								Applink applink = (Applink) entity;
								if (applink.validatedDate != null && applink.validatedDate.longValue() == -1) {
									continue;
								}
							}
							mAdapter.add(entity);
						}
						mAdapter.sort(mReverseSort ? new Entity.SortByPositionSortDateAscending() : new Entity.SortByPositionSortDate());
						draw();
					}
					postBind();
					mLoaded = true;
					mFirstBind = false;
					onActivityComplete();
				}
				else {
					Errors.handleError(getActivity(), result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected void postBind() {
		/*
		 * Clear notifications and activity indicator if visible, acting as an
		 * activity stream and binding for the first time.
		 */
		if (mFirstBind && mActivityStream && getUserVisibleHint()) {
			MessagingManager.getInstance().setNewActivity(false);
			if (getActivity() != null) {
				((AircandiForm) getActivity()).updateActivityAlert();
			}
			MessagingManager.getInstance().cancelNotifications();
		}
	}

	@Override
	public void draw() {
		mAdapter.notifyDataSetChanged();
		BusProvider.getInstance().post(new EntitiesLoadedEvent()); // Used to trigger item highlighting
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View v) {
		if (v.getTag() == null) {
			((BaseActivity) getActivity()).onAdd(new Bundle());
		}
		else {

			final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
			if (entity instanceof Applink) {
				Aircandi.dispatch.shortcut(getActivity(), entity.getShortcut(), null, null, null);
			}
			else {
				if (mQuery instanceof EntitiesQuery) {
					String linkType = ((EntitiesQuery) mQuery).getLinkType();
					if (linkType != null) {
						Bundle extras = new Bundle();
						extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
						extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, ((EntitiesQuery) mQuery).getEntityId());
						Aircandi.dispatch.route(getActivity(), Route.BROWSE, entity, null, extras);
						return;
					}
				}
				Aircandi.dispatch.route(getActivity(), Route.BROWSE, entity, null, null);
			}
		}
	}

	@Override
	public void onRefresh() {
		saveListPosition();
		super.onRefresh();
	}

	public void onMoreButtonClick(View view) {
		lazyLoad();
	}

	@Override
	public void onScollToTop() {
		scrollToTop(mListView);
	}

	public void onActivityComplete() {
		if (mButtonSpecial != null && mButtonSpecialEnabled) {
			if (mButtonSpecialClickable) {
				if (mEntities.size() == 0) {
					lockFooter(true);
				}
				else {
					lockFooter(false);
					handleFooter(false, true, 500);
				}
			}
			showButtonSpecial(mEntities.size() == 0, mListEmptyMessageResId, mHeaderView);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(Constants.EXTRA_LIST_ITEM_RESID, mListItemResId);
		outState.putString(Constants.EXTRA_LIST_VIEW_TYPE, mListViewType);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void lazyLoad() {

		final ViewSwitcher switcher = (ViewSwitcher) mLoadingView.findViewById(R.id.animator_more);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				switcher.setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLazyLoadList");
				ModelResult result = mQuery.execute(mEntities.size(), null);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					Errors.handleError(getActivity(), result.serviceResponse);
				}
				else {
					if (result.data != null) {
						for (Entity entity : (List<Entity>) result.data) {
							mAdapter.add(entity);
						}
						mAdapter.sort(mReverseSort ? new Entity.SortByPositionSortDateAscending() : new Entity.SortByPositionSortDate());
						draw();
					}
				}
				switcher.setDisplayedChild(0);
			}
		}.execute();
	}

	@SuppressWarnings("ucd")
	protected Integer getVisibleListItemsHeight() {

		int totalHeight = 0;
		int listHeight = mListView.getHeight();

		for (int i = 0, len = mAdapter.getCount(); i < len; i++) {
			View listItem = mAdapter.getView(i, null, mListView);
			listItem.measure(0, 0);
			int itemHeight = listItem.getMeasuredHeight() + ((ListView) mListView).getDividerHeight();
			totalHeight += itemHeight;
			if (totalHeight >= listHeight) {
				return totalHeight;
			}
		}
		return totalHeight;
	}

	protected void saveListPosition() {
		mLastViewedPosition = mListView.getFirstVisiblePosition();
		View view = mListView.getChildAt(0);
		mTopOffset = (view == null) ? 0 : view.getTop();
	}

	public void setListPositionToEntity(String entityId) {
		final AtomicInteger position = new AtomicInteger(0);

		for (Entity entity : mEntities) {
			if (entity.id.equals(entityId)) {

				mListView.post(new Runnable() {
					@Override
					public void run() {
						Integer offsetToShowHeader = UI.getRawPixelsForDisplayPixels(100f);
						((ListView) mListView).setSelectionFromTop(position.get() + 1, offsetToShowHeader);
					}
				});

				return;
			}
			position.incrementAndGet();
		}
	}

	@Override
	public void setMenuVisibility(final boolean visible) {
		/*
		 * Called when fragment is going to be visible to the user and that's when
		 * we want to start the data binding work. CreateView will have already been called.
		 */
		super.setMenuVisibility(visible);
		doMenuVisibility(visible);
	}

	public void doMenuVisibility(boolean visible) {
		mIsVisible = visible;
		if (mSelfBindingEnabled && mIsVisible) {
			bind(BindingMode.AUTO);
		}
	}

	private void restoreState(Bundle savedInstanceState) {
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(Constants.EXTRA_LIST_ITEM_RESID)) {
			mListItemResId = savedInstanceState.getInt(Constants.EXTRA_LIST_ITEM_RESID);
		}
	}

	private void gridSizing() {

		GridView gridView = (GridView) mListView;

		/* Set spacing */
		Integer requestedHorizontalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
		Integer requestedVerticalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_vertical);
		gridView.setHorizontalSpacing(requestedHorizontalSpacing);
		gridView.setVerticalSpacing(requestedVerticalSpacing);

		/* Stash some sizing info */
		final DisplayMetrics metrics = mResources.getDisplayMetrics();
		final Integer availableWidth = metrics.widthPixels - gridView.getPaddingLeft() - gridView.getPaddingRight();
		final Integer availableHeight = metrics.heightPixels - gridView.getPaddingTop() - gridView.getPaddingBottom();

		Integer requestedColumnWidth = mResources.getDimensionPixelSize(R.dimen.grid_column_width_requested_large);

		mVisibleColumns = (availableWidth + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
		if (mVisibleColumns <= 0) {
			mVisibleColumns = 1;
		}

		mVisibleRows = (availableHeight + requestedVerticalSpacing) / (requestedColumnWidth + requestedVerticalSpacing);
		if (mVisibleRows <= 0) {
			mVisibleRows = 1;
		}

		int spaceLeftOver = availableWidth - (mVisibleColumns * requestedColumnWidth) - ((mVisibleColumns - 1) * requestedHorizontalSpacing);

		mPhotoWidthPixels = requestedColumnWidth + spaceLeftOver / mVisibleColumns;
		gridView.setColumnWidth(mPhotoWidthPixels);

	}

	protected void handleFooter(Boolean auto, Boolean visible, Integer duration) {

		if (mFooterHolder == null) return;
		if (mFooterHolderLocked) return;

		if (auto) {

			DragDirection direction = ((AirListView) mListView).getDragDirectionLast();
			if (direction == DragDirection.UP && mFooterHolderHidden) return;
			if (direction == DragDirection.DOWN && !mFooterHolderHidden) return;

			ObjectAnimator animator = ObjectAnimator.ofFloat(mFooterHolder
					, "translationY"
					, (direction == DragDirection.UP) ? 0 : mFooterHolder.getHeight()
					, (direction == DragDirection.UP) ? mFooterHolder.getHeight() : 0);

			animator.setDuration(duration).start();
			mFooterHolderHidden = (direction == DragDirection.UP);
			mFooterHolder.setClickable(direction == DragDirection.DOWN);
		}
		else {
			if (visible && (mFooterHolderHidden || ViewHelper.getTranslationY(mFooterHolder) != 0)) {
				ObjectAnimator animator = ObjectAnimator.ofFloat(mFooterHolder
						, "translationY"
						, mFooterHolder.getHeight()
						, 0);

				animator.setDuration(duration).start();
				mFooterHolderHidden = false;
				mFooterHolder.setClickable(true);
			}
			else if (!visible && (!mFooterHolderHidden || ViewHelper.getTranslationY(mFooterHolder) == 0)) {
				ObjectAnimator animator = ObjectAnimator.ofFloat(mFooterHolder
						, "translationY"
						, 0
						, mFooterHolder.getHeight());

				animator.setDuration(duration).start();
				mFooterHolderHidden = true;
				mFooterHolder.setClickable(false);
			}
		}
	}

	protected void lockFooter(Boolean lock) {
		mFooterHolderLocked = lock;
	}

	protected ListAdapter getAdapter() {
		return new ListAdapter(mEntities);
	}

	@SuppressWarnings("ucd")
	public void removeHeaderView() {
		if (mHeaderView != null) {
			((ListView) mListView).removeHeaderView(mHeaderView);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public String getActivityTitle() {
		BaseActivity activity = (BaseActivity) getActivity();
		return (String) ((activity.getActivityTitle() != null) ? activity.getActivityTitle() : getActivity().getTitle());
	}

	@Override
	protected int getLayoutId() {
		return mListLayoutResId;
	}

	public Map<String, Highlight> getHighlightEntities() {
		return mHighlightEntities;
	}

	public EntityListFragment setListButtonMessageResId(Integer listButtonMessageResId) {
		mListButtonMessageResId = listButtonMessageResId;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setListEmptyMessageResId(Integer listEmptyMessageResId) {
		mListEmptyMessageResId = listEmptyMessageResId;
		return this;
	}

	public EntityListFragment setListLoadingResId(Integer listLoadingResId) {
		mListLoadingResId = listLoadingResId;
		return this;
	}

	public EntityListFragment setListItemResId(Integer listItemResId) {
		mListItemResId = listItemResId;
		return this;
	}

	public EntityListFragment setListLayoutResId(Integer listLayoutResId) {
		mListLayoutResId = listLayoutResId;
		return this;
	}

	public EntityListFragment setListViewType(String listViewType) {
		mListViewType = listViewType;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setHeaderViewResId(Integer headerViewResId) {
		mHeaderViewResId = headerViewResId;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setHeaderView(View headerView) {
		if (mHeaderView != null) {
			((ListView) mListView).removeHeaderView(mHeaderView);
		}
		if (mParallaxHeader) {
			((AirListView) mListView).addParallaxedHeaderView(mHeaderView);
		}
		else {
			((ListView) mListView).addHeaderView(mHeaderView);
		}
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setFooterViewResId(Integer footerViewResId) {
		mFooterViewResId = footerViewResId;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setBackgroundResId(Integer backgroundResId) {
		mBackgroundResId = backgroundResId;
		return this;
	}

	public EntityListFragment setMonitor(SimpleMonitor monitor) {
		mMonitor = monitor;
		return this;
	}

	public EntityListFragment setQuery(IQuery query) {
		mQuery = query;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setParallaxHeader(Boolean parallaxHeader) {
		mParallaxHeader = parallaxHeader;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setListPagingEnabled(Boolean listPagingEnabled) {
		mListPagingEnabled = listPagingEnabled;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setEntityCacheEnabled(Boolean entityCacheEnabled) {
		mEntityCacheEnabled = entityCacheEnabled;
		return this;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setReverseSort(Boolean reverseSort) {
		mReverseSort = reverseSort;
		return this;
	}

	public View getHeaderView() {
		return mHeaderView;
	}

	public AbsListView getListView() {
		return mListView;
	}

	public IMonitor getMonitor() {
		return mMonitor;
	}

	public IQuery getQuery() {
		return mQuery;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void onResume() {
		/*
		 * Called when fragment is attached and active but might
		 * not be visible to the user yet. ViewPager has logic to
		 * pre-create fragments even if they aren't visible yet.
		 */
		super.onResume();
		resume();
	}

	protected void resume() {
		if (mSelfBindingEnabled && (getActivity() != null && !getActivity().isFinishing())) {
			bind(BindingMode.AUTO);
		}
		if (mFooterHolder != null) {
			handleFooter(false, true, 500);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		saveListPosition();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    protected class ListAdapter extends ArrayAdapter<Entity> {

		public ListAdapter(List<Entity> entities) {
			super(getActivity(), 0, entities);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (mListPagingEnabled && mQuery != null && mQuery.isMore() && position == mEntities.size())
				return mLoadingView;

			View view = convertView;

			if (mListPagingEnabled && position >= mEntities.size() && position < (mVisibleColumns * mVisibleRows)) {
				if (view == null || view.findViewById(R.id.item_placeholder) == null) {
					view = LayoutInflater.from(getActivity()).inflate(R.layout.temp_list_item_empty, null);
					if (mListView instanceof GridView) {
						Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
						final AbsListView.LayoutParams params = new AbsListView.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - (nudge / 2));
						view.findViewById(R.id.item_placeholder).setLayoutParams(params);
					}
				}
				view.setTag(null);
				view.setClickable(true);
				view.setOnClickListener(EntityListFragment.this);
			}
			else {

				Entity entity = mEntities.get(position);

				/* Perform cache lookup to make sure we are using the latest */
				if (mEntityCacheEnabled && EntityManager.getEntityCache().get(entity.id) != null) {
					entity = EntityManager.getEntityCache().get(entity.id);
				}

                /*
                 * Holder is created and bound to view elements by the controller in bindListItem.
                 */
				if (view == null
						|| view.findViewById(R.id.animator_more) != null
						|| view.findViewById(R.id.item_placeholder) != null) {
					view = LayoutInflater.from(getActivity()).inflate(mListItemResId, null);
				}

				if (entity != null) {
					bindListItem(entity, view);
					view.setClickable(true);
					view.setOnClickListener(EntityListFragment.this);
				}
			}
			return view;
		}

		@Override
		public int getCount() {
			if (mListPagingEnabled && mQuery != null && mQuery.isMore())
				return mEntities.size() + 1;
			else if (mListViewType.equals(ViewType.GRID)) {
				if (mEntities.size() == 0) return 0;
				return Math.max(mEntities.size(), mVisibleColumns * mVisibleRows);
			}
			else {
				return mEntities.size();
			}
		}

		@Override
		public Entity getItem(int position) {
			return mEntities.get(position);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		public List<Entity> getItems() {
			return mEntities;
		}
	}

	protected void bindListItem(Entity entity, View view) {
		IEntityController controller = Aircandi.getInstance().getControllerForEntity(entity);
		controller.bind(entity, view);

		/* Special highlighting */

		if (mHighlightEntities.size() > 0) {
			view.setBackgroundResource(mBackgroundResId);
			if (mHighlightEntities.containsKey(entity.id)) {
				Highlight highlight = mHighlightEntities.get(entity.id);
				if (!highlight.isOneShot() || !highlight.hasFired()) {
					if (Aircandi.themeTone.equals(ThemeTone.DARK)) {
						view.setBackgroundResource(R.drawable.selector_image_highlight_dark);
					}
					else {
						view.setBackgroundResource(R.drawable.selector_image_highlight_light);
					}
					highlight.setFired(true);
				}
			}
		}
	}

	public void drawHighlights(Entity entity, View view) {

	}

	public static class ViewType {
		public static String LIST = "list";
		public static String GRID = "grid";
	}

	public static class Highlight {
		private Boolean oneShot = false;
		private Boolean fired   = false;

		@SuppressWarnings("ucd")
		public Highlight(Boolean oneShot) {
			this.oneShot = oneShot;
		}

		public Boolean isOneShot() {
			return oneShot;
		}

		public void setOneShot(Boolean oneShot) {
			this.oneShot = oneShot;
		}

		public Boolean hasFired() {
			return fired;
		}

		public void setFired(Boolean fired) {
			this.fired = fired;
		}
	}
}