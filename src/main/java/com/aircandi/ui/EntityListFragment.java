package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.Patchr.ThemeTone;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataReadyEvent;
import com.aircandi.events.EntitiesLoadedEvent;
import com.aircandi.events.EntitiesRequestEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.ActionType;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkSpecType;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.components.AnimationFactory;
import com.aircandi.ui.components.BusyController;
import com.aircandi.ui.components.FloatingActionController;
import com.aircandi.ui.components.ListController;
import com.aircandi.ui.components.MessageController;
import com.aircandi.ui.widgets.AirListView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Maps;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityListFragment extends BaseFragment
		implements AirListView.OnDragListener,
		           OnClickListener,
		           SwipeRefreshLayout.OnRefreshListener,
		           AbsListView.OnScrollListener {

	/* Widgets */
	protected AbsListView    mListView;
	protected View           mLoadingView;
	protected View           mHeaderView;
	protected View           mHeaderCandiView;
	private   ViewAnimator   mHeaderViewAnimator;
	protected View           mFooterView;
	protected ListController mListController;

	/* Resources */
	protected Integer mHeaderViewResId;
	protected Integer mFooterViewResId;
	protected Integer mBackgroundResId;
	protected Integer mListItemResId;
	protected Integer mListLayoutResId = R.layout.entity_list_fragment;
	protected Integer mListLoadingResId;
	protected Integer mBubbleButtonMessageResId;
	protected Integer mListEmptyMessageResId;
	protected Integer mTitleResId;

	/* Configuration */
	protected String mListViewType;
	protected Boolean mListPagingEnabled  = true;
	protected Boolean mParallaxHeader     = false;
	protected Boolean mEntityCacheEnabled = true;
	protected Boolean mReverseSort        = false;
	protected Boolean mFabEnabled         = true;

	/* Runtime data */
	protected Integer mPhotoWidthPixels;
	protected Integer mVisibleColumns = 1;
	protected Integer mVisibleRows    = 3;
	protected Integer mTopOffset;
	protected Integer mLastViewedPosition;
	protected Boolean mNotEmpty = false; // Used to control busy feedback
	protected Boolean mMore     = false;

	/* Data binding */
	protected Integer mActionType = ActionType.GET_ENTITIES;
	protected String  mMonitorEntityId;
	protected Entity  mMonitorEntity;
	protected Integer mPageSize;
	protected String  mLinkSchema;
	protected String  mLinkType;
	protected String  mLinkDirection;

	@NonNull
	protected Boolean                mSelfBind          = true;
	protected List<Entity>           mEntities          = new ArrayList<Entity>();
	protected Map                    mLinkWhere         = Maps.asMap("enabled", true);
	protected Map<String, Highlight> mHighlightEntities = new HashMap<String, Highlight>();
	protected ListAdapter mAdapter;

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
			mAdapter = new ListAdapter(mEntities);
		}
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		/*
		 * The listview scroll position seems to be preserved between destroy view
		 * and create view. Probably falls into the bucket of view properties that are
		 * auto restored by android.
		 */
		restoreState(savedInstanceState);

		if (view == null) return null;

		if (mListItemResId == null) {
			throw new IllegalArgumentException("List item resource is required by EntityListFragment");
		}

		mNotEmpty = (mAdapter != null && mAdapter.getCount() != 0);
		mListView = (AbsListView) view.findViewById(R.id.list);
		if (mListView != null) {
			((AirListView) mListView).setScrollListener(this);
		}

		/* Setup list controller */
		mListController = new ListController();
		mListController.setFloatingActionController(new FloatingActionController());
		mListController.setBusyController(new BusyController());

		mListController.setMessageController(new MessageController(view.findViewById(R.id.list_message)));
		mListController.getBusyController().setProgressBar(view.findViewById(R.id.list_progress));
		mListController.resume();
		if (mBubbleButtonMessageResId != null) {
			mListController.getMessageController().setMessage(StringManager.getString(mBubbleButtonMessageResId));
		}

		/* Inject floating action button view */
		View fab = view.findViewById(R.id.list_fab);
		if (fab != null) {
			mListController.getFloatingActionController().setView(fab);
		}

		/* Inject swipe refresh component */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(UI.getResIdForAttribute(getActivity(), R.attr.refreshColor)));
			swipeRefresh.setProgressBackgroundColor(UI.getResIdForAttribute(getActivity(), R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			mListController.getBusyController().setSwipeRefresh(swipeRefresh);
		}

		if (mListLoadingResId != null) {
			mLoadingView = LayoutInflater.from(getActivity()).inflate(mListLoadingResId, null);
		}

		if (mHeaderViewResId != null && mListView != null && mListViewType.equals(ViewType.LIST)) {
			mHeaderView = inflater.inflate(mHeaderViewResId, mListView, false);
			if (mParallaxHeader && mListView instanceof AirListView) {
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

		/* Pulled from host activity by super */
		if (mListView != null && mFabEnabled) {
			((AirListView) mListView).setDragListener(this);
		}

		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (mListView != null) {
			if (mListViewType.equals(ViewType.LIST)) {
				if (isAdded()) {
					((ListView) mListView).setAdapter(mAdapter);
				}
			}
			else if (mListViewType.equals(ViewType.GRID)) {
				((GridView) mListView).setAdapter(mAdapter);
			}
		}

		if (mHeaderView != null) {

			/* Draw the header */
			if (((BaseActivity) getActivity()).getEntity() != null) {
				((BaseEntityForm) getActivity()).draw(view);
			}
			/*
			 * Parallax the photo
			 */
			mHeaderCandiView = mHeaderView.findViewById(R.id.candi_view);
			if (mHeaderCandiView != null && mListView instanceof AirListView) {
				View photo = mHeaderCandiView.findViewById(R.id.photo);
				((AirListView) mListView).addParallaxedView(photo);
			}
			/*
			 * Grab the animator
			 */
			mHeaderViewAnimator = (ViewAnimator) mHeaderView.findViewById(R.id.animator_header);
		}

		return view;
	}

	@Override
	public void draw(View view) {
		mAdapter.notifyDataSetChanged();
		Dispatcher.getInstance().post(new EntitiesLoadedEvent()); // Used by MessageForm to trigger item highlighting
	}

	@Override
	public void bind(final BindingMode mode) {
		/*
		 * If additional entities have been paged in, we include them as part of the request size.
		 */
		Integer limit = mPageSize;
		if (mEntities.size() > 0) {
			limit = (int) Math.ceil((float) mEntities.size() / mPageSize) * mPageSize;
		}

		fetch(0, limit, mode == BindingMode.MANUAL);

		if (mode != BindingMode.MANUAL) {
			mListController.getMessageController().showMessage(false);
		}

		if (!mNotEmpty) {
			mListController.getBusyController().show(BusyAction.Refreshing_Empty);
		}
	}

	public void fetch(Integer skip, Integer limit, Boolean force) {
		/*
		 * Sorting is applied to links not the entities on the service side.
		 */
		Integer skipCount = ((int) Math.ceil((double) skip / mPageSize) * mPageSize);
		Cursor cursor = new Cursor()
				.setLimit(limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skipCount)
				.setWhere(mLinkWhere)
				.setDirection(mLinkDirection);

		if (mLinkType != null) {
			List<String> linkTypes = new ArrayList<String>();
			linkTypes.add(mLinkType);
			cursor.setLinkTypes(linkTypes);
		}

		Integer linkProfile = LinkSpecType.NO_LINKS;
		if (mLinkSchema != null) {
			List<String> toSchemas = new ArrayList<String>();
			toSchemas.add(mLinkSchema);
			cursor.setToSchemas(toSchemas);
			IEntityController controller = Patchr.getInstance().getControllerForSchema(mLinkSchema);
			linkProfile = controller.getLinkProfile();
		}

		EntitiesRequestEvent request = new EntitiesRequestEvent()
				.setCursor(cursor)
				.setLinkProfile(linkProfile);

		request.setActionType(mActionType)
		       .setEntityId(mMonitorEntityId)
		       .setTag(System.identityHashCode(this));
		/*
		 * Providing a CacheStamp means we won't get called back unless something fresher is available.
		 */
		if (!force && mMonitorEntity != null && skip == 0) {
			request.setCacheStamp(mMonitorEntity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataReady(final DataReadyEvent event) {

		if (event.tag != null && event.tag.equals(System.identityHashCode(this))) {

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {

					if (event.entities != null && event.entities.size() > 0) {
						mMore = event.more;
						if (event.cursor != null && event.cursor.getSkip() == 0) {
							mEntities.clear();
							mAdapter.setNotifyOnChange(false);
							mAdapter.clear();

							if (getActivity() != null && !getActivity().isFinishing()) {
								configureStandardMenuItems(((BaseActivity) getActivity()).getOptionMenu());
							}
						}

						/* Chance for sub class to inject additional entities */
						injectEntities(mAdapter);

						for (Entity entity : event.entities) {
							mAdapter.add(entity);
						}

						mAdapter.sort(mReverseSort ? new Entity.SortByPositionSortDateAscending() : new Entity.SortByPositionSortDate());

						if (isAdded()) {
							draw(null);
						}
					}

					if (event.entity != null) {
						mMonitorEntity = event.entity;
					}

					if (mLoadingView != null) {
						ViewSwitcher switcher = (ViewSwitcher) mLoadingView.findViewById(R.id.animator_more);
						if (switcher != null) {
							switcher.setDisplayedChild(0);
						}
					}
					postBind();
					mNotEmpty = (mAdapter != null && mAdapter.getCount() != 0);
					onProcessingComplete(ResponseCode.SUCCESS);
				}
			});
		}
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		onProcessingComplete(ResponseCode.FAILED);
		Errors.handleError(getActivity(), event.errorResponse);
	}

	public void onProcessingComplete(final ResponseCode responseCode) {

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mListController.getBusyController().hide(false);
				if (getAdapter().getCount() == 0
						&& mListEmptyMessageResId != null
						&& (responseCode == null || responseCode == ResponseCode.SUCCESS)) {
					mListController.getMessageController().setMessage(StringManager.getString(mListEmptyMessageResId));
					mListController.getMessageController().fadeIn(Constants.TIME_ONE_SECOND);
				}
				else {
					mListController.getMessageController().fadeOut(); // Only fades if currently visible
				}
			}
		});
	}

	@Override
	public void onClick(View v) {

		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
		final Bundle extras = new Bundle();

		extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

		if (mLinkType != null) {
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, mLinkType);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mMonitorEntityId);
			Patchr.router.route(getActivity(), Route.BROWSE, entity, extras);
			return;
		}

		Patchr.router.route(getActivity(), Route.BROWSE, entity, extras);
	}

	public void onHeaderClick(View view) {
		AnimationFactory.flipTransition(mHeaderViewAnimator, AnimationFactory.FlipDirection.BOTTOM_TOP, 200);
	}

	@Override
	public void onRefresh() {
		/*
		 * Called by swipe refresh or from AircandiForm triggered by menu item.
		 * Super class calls bind(BindingMode.Manual). The swipe refresh is shut
		 * down and other busy ui takes over.
		 */
		saveListPosition();
		super.onRefresh();
	}

	@Override
	public void onViewLayout() {
		/*
		 * Position bubble button initially allowing for the
		 * list header height.
		 */
		if (mHeaderView != null) {
			mListController.getMessageController().position(null, mHeaderView, null);
			mListController.getBusyController().position(mHeaderView, null);
		}
	}

	public boolean onDragEvent(AirListView.DragEvent event, Float dragX, Float dragY) {
		/*
		 * Fired by list fragments.
		 */
		if (event == AirListView.DragEvent.DRAG) {
			handleListDrag();
		}
		return false;
	}

	public void onMoreButtonClick(View view) {
		((ViewSwitcher) mLoadingView.findViewById(R.id.animator_more)).setDisplayedChild(1);
		fetch(mEntities.size(), mPageSize, true);
	}

	public void handleListDrag() {
		AirListView.DragDirection direction = ((AirListView) mListView).getDragDirectionLast();
		if (direction == AirListView.DragDirection.DOWN) {
			mListController.getFloatingActionController().slideIn(AnimationManager.DURATION_SHORT, 0);
		}
		else {
			mListController.getFloatingActionController().slideOut(AnimationManager.DURATION_SHORT);
		}
	}

	public void onDragBottom() {}

	public void onScollToTop() {
		scrollToTop(mListView);
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			DownloadManager.getInstance().resumeTag(mGroupTag);
		}
		else if (scrollState == SCROLL_STATE_FLING) {
			DownloadManager.getInstance().pauseTag(mGroupTag);
		}
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		/* Do nothing */
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

	protected void injectEntities(ListAdapter adapter) {}

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
		if (mIsVisible) {
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

	public void removeHeaderView() {
		if (mHeaderView != null) {
			((ListView) mListView).removeHeaderView(mHeaderView);
		}
	}

	@Override
	protected int getLayoutId() {
		return mListLayoutResId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public EntityListFragment setTitleResId(Integer titleResId) {
		mTitleResId = titleResId;
		return this;
	}

	public EntityListFragment setBubbleButtonMessageResId(Integer bubbleButtonMessageResId) {
		mBubbleButtonMessageResId = bubbleButtonMessageResId;
		return this;
	}

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

	public EntityListFragment setHeaderViewResId(Integer headerViewResId) {
		mHeaderViewResId = headerViewResId;
		return this;
	}

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

	public EntityListFragment setFooterViewResId(Integer footerViewResId) {
		mFooterViewResId = footerViewResId;
		return this;
	}

	public EntityListFragment setBackgroundResId(Integer backgroundResId) {
		mBackgroundResId = backgroundResId;
		return this;
	}

	public EntityListFragment setParallaxHeader(Boolean parallaxHeader) {
		mParallaxHeader = parallaxHeader;
		return this;
	}

	public EntityListFragment setListPagingEnabled(Boolean listPagingEnabled) {
		mListPagingEnabled = listPagingEnabled;
		return this;
	}

	public EntityListFragment setEntityCacheEnabled(Boolean entityCacheEnabled) {
		mEntityCacheEnabled = entityCacheEnabled;
		return this;
	}

	public EntityListFragment setReverseSort(Boolean reverseSort) {
		mReverseSort = reverseSort;
		return this;
	}

	public EntityListFragment setFabEnabled(Boolean fabEnabled) {
		mFabEnabled = fabEnabled;
		return this;
	}

	public EntityListFragment setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public EntityListFragment setLinkSchema(String linkSchema) {
		mLinkSchema = linkSchema;
		return this;
	}

	public EntityListFragment setLinkType(String linkType) {
		mLinkType = linkType;
		return this;
	}

	public EntityListFragment setLinkDirection(String linkDirection) {
		mLinkDirection = linkDirection;
		return this;
	}

	public EntityListFragment setMonitorEntityId(String monitorEntityId) {
		mMonitorEntityId = monitorEntityId;
		return this;
	}

	public EntityListFragment setMonitorEntity(Entity monitorEntity) {
		mMonitorEntity = monitorEntity;
		return this;
	}

	public EntityListFragment setLinkWhere(Map linkWhere) {
		mLinkWhere = linkWhere;
		return this;
	}

	public EntityListFragment setActionType(Integer actionType) {
		mActionType = actionType;
		return this;
	}

	public EntityListFragment setSelfBind(@NonNull Boolean selfBind) {
		mSelfBind = selfBind;
		return this;
	}

	public Integer getTitleResId() {
		return mTitleResId;
	}

	public Boolean getFabEnabled() {
		return mFabEnabled;
	}

	public String getActivityTitle() {
		BaseActivity activity = (BaseActivity) getActivity();
		return (String) ((activity.getActivityTitle() != null) ? activity.getActivityTitle() : getActivity().getTitle());
	}

	public ListController getListController() {
		return mListController;
	}

	public Map<String, Highlight> getHighlightEntities() {
		return mHighlightEntities;
	}

	public View getHeaderView() {
		return mHeaderView;
	}

	public AbsListView getListView() {
		return mListView;
	}

	public List<Entity> getEntities() {
		return mEntities;
	}

	public ListAdapter getAdapter() {
		return mAdapter;
	}

	public Boolean isMore() {
		return mMore;
	}

	public String getMonitorEntityId() {
		return mMonitorEntityId;
	}

	public Integer getPageSize() {
		return mPageSize;
	}

	public Entity getMonitorEntity() {
		return mMonitorEntity;
	}

	public String getLinkType() {
		return mLinkType;
	}

	/*--------------------------------------------------------------------------------------------
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
		if (mListController != null) {
			/* Slides it in only if it is currently out. */
			mListController.getFloatingActionController().slideIn(AnimationManager.DURATION_SHORT, 500);
		}

		if (mSelfBind && getActivity() != null && !getActivity().isFinishing()) {
			bind(BindingMode.AUTO);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mListController != null) {
			mListController.pause();
		}
		saveListPosition();
	}

	@Override
	public void onDestroyView() {
		if (DownloadManager.getInstance() != null) {
			DownloadManager.getInstance().cancelTag(mGroupTag);
		}
		super.onDestroyView();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	protected class ListAdapter extends ArrayAdapter<Entity> {

		public ListAdapter(List<Entity> entities) {
			super(getActivity(), 0, entities);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (mListPagingEnabled
					&& mMore
					&& position == mEntities.size()
					&& mEntities.size() > 0)
				return mLoadingView;

			View view = convertView;

			if (mListPagingEnabled
					&& position >= mEntities.size()
					&& position < (mVisibleColumns * mVisibleRows)) {
				/*
				 * Make the widget used to request more list items.
				 */
				if (view == null || view.findViewById(R.id.item_placeholder) == null) {
					view = LayoutInflater.from(getActivity()).inflate(R.layout.temp_listitem_empty, null);
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
				entity.index = position + 1;

				/* Perform cache lookup to make sure we are using the latest */
				if (mEntityCacheEnabled && DataController.getStoreEntity(entity.id) != null) {
					entity = DataController.getStoreEntity(entity.id);
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
					bindListItem(entity, view, mGroupTag);
					view.setClickable(true);
					view.setOnClickListener(EntityListFragment.this);
				}
			}
			return view;
		}

		@Override
		public int getCount() {
			if (mListPagingEnabled && mMore)
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

	protected void bindListItem(Entity entity, View view, String groupTag) {
		IEntityController controller = Patchr.getInstance().getControllerForEntity(entity);
		controller.bind(entity, view, groupTag);

		/* Special highlighting */

		if (mHighlightEntities.size() > 0) {
			view.setBackgroundResource(mBackgroundResId);
			if (mHighlightEntities.containsKey(entity.id)) {
				Highlight highlight = mHighlightEntities.get(entity.id);
				if (!highlight.isOneShot() || !highlight.hasFired()) {
					if (Patchr.themeTone.equals(ThemeTone.DARK)) {
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

	public void drawHighlights(Entity entity, View view) {}

	public static class ViewType {
		public static String LIST = "list";
		public static String GRID = "grid";
	}

	public static enum AnimateType {
		FADE,
		SLIDE
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