package com.aircandi.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.BusProvider;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.EntitiesLoadedEvent;
import com.aircandi.monitors.IMonitor;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.queries.IQuery;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirListView;
import com.aircandi.ui.widgets.AirListView.DragDirection;
import com.aircandi.ui.widgets.AirListView.DragEvent;
import com.aircandi.ui.widgets.AirListView.OnDragListener;
import com.aircandi.ui.widgets.AirSwipeRefreshLayout;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class EntityListFragment extends BaseFragment implements OnClickListener {

	/* Widgets */
	protected AbsListView				mListView;
	protected View						mLoadingView;
	protected View						mHeaderView;
	protected View						mHeaderCandiView;										// NO_UCD (unused code)
	protected View						mFooterView;
	protected View						mFooterHolder;

	/* Resources */
	protected Integer					mHeaderViewResId;
	protected Integer					mFooterViewResId;
	protected Integer					mBackgroundResId;
	protected Integer					mListItemResId;
	protected Integer					mListLayoutResId	= R.layout.entity_list_fragment;
	protected Integer					mListLoadingResId;
	protected Integer					mListButtonMessageResId;
	protected Integer					mListEmptyMessageResId;

	/* Configuration */
	protected String					mListViewType;
	protected Boolean					mListPagingEnabled	= true;
	protected Boolean					mParallaxHeader		= false;
	protected Boolean					mEntityCacheEnabled	= true;
	protected Boolean					mFooterHolderHidden	= false;
	protected Boolean					mFooterHolderLocked	= false;
	protected Boolean					mReverseSort		= false;

	/* Runtime data */
	protected Integer					mPhotoWidthPixels;
	protected Integer					mVisibleColumns		= 1;
	protected Integer					mVisibleRows		= 3;
	protected Integer					mTopOffset;											// NO_UCD (unused code)
	protected Integer					mLastViewedPosition;									// NO_UCD (unused code)

	/* Data binding */
	protected List<Entity>				mEntities			= new ArrayList<Entity>();
	protected Map<String, Highlight>	mHighlightEntities	= new HashMap<String, Highlight>();
	protected IQuery					mQuery;
	protected SimpleMonitor				mMonitor;
	protected ListAdapter				mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * If the bundle includes state then we know the fragment is being completely
		 * recreated. Rather than try to restore all the state that was passed in
		 * using settings when originally created, just bail.
		 */
		if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
			Intent intent = getSherlockActivity().getIntent();
			getSherlockActivity().finish();
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
		 * and create view. Probably falls into the buck of view properties that are
		 * auto restored by android.
		 */
		restoreState(savedInstanceState);

		if (view == null) return view;

		if (mListItemResId == null) {
			throw new IllegalArgumentException("List item resource is required by EntityListFragment");
		}

		mListView = (AbsListView) view.findViewById(R.id.list);

		if (mListLoadingResId != null) {
			mLoadingView = LayoutInflater.from(getSherlockActivity()).inflate(mListLoadingResId, null);
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

		mFooterHolder = getSherlockActivity().findViewById(R.id.footer_holder);
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
	protected void preBind() {}

	@Override
	public void bind(final BindingMode mode) {

		/*
		 * Guard binding if this is a private place and user isn't approved
		 */
		Entity entity = ((BaseActivity) getSherlockActivity()).getEntity();
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
					mLoaded = true;
					postBind();
					onActivityComplete();
				}
				else {
					Errors.handleError(getSherlockActivity(), result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected void postBind() {
		/*
		 * Clear notifications and activity indicator if visible and acting as an activity stream
		 */
		if (mActivityStream && getUserVisibleHint()) {
			MessagingManager.getInstance().setNewActivity(false);
			if (getSherlockActivity() != null) {
				((AircandiForm) getSherlockActivity()).updateActivityAlert();
			}
			MessagingManager.getInstance().cancelNotifications();
		}
	}

	@Override
	public void draw() {
		mAdapter.notifyDataSetChanged();
		BusProvider.getInstance().post(new EntitiesLoadedEvent()); // Used to trigger item highlighting
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onClick(View v) {
		if (v.getTag() == null) {
			((BaseActivity) getSherlockActivity()).onAdd(new Bundle());
		}
		else {

			final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
			if (entity instanceof Applink) {
				Aircandi.dispatch.shortcut(getSherlockActivity(), entity.getShortcut(), null, null, null);
			}
			else {
				if (mQuery instanceof EntitiesQuery) {
					String linkType = ((EntitiesQuery) mQuery).getLinkType();
					if (linkType != null) {
						Bundle extras = new Bundle();
						extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
						extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, ((EntitiesQuery) mQuery).getEntityId());
						Aircandi.dispatch.route(getSherlockActivity(), Route.BROWSE, entity, null, extras);
						return;
					}
				}
				Aircandi.dispatch.route(getSherlockActivity(), Route.BROWSE, entity, null, null);
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

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

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
					Errors.handleError(getSherlockActivity(), result.serviceResponse);
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setListPositionToEntity(String entityId) {
		final AtomicInteger position = new AtomicInteger(0);

		for (Entity entity : mEntities) {
			if (entity.id.equals(entityId)) {

				mListView.post(new Runnable() {
					@Override
					public void run() {
						if (Constants.SUPPORTS_HONEYCOMB) {
							Integer offsetToShowHeader = UI.getRawPixelsForDisplayPixels(Aircandi.applicationContext, 100f);
							((ListView) mListView).setSelectionFromTop(position.get() + 1, offsetToShowHeader);
						}
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

	public ViewHolder bindHolder(View view, ViewHolder holder) {

		if (holder == null) {
			holder = new ViewHolder();
		}

		holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
		holder.photoView = (AirImageView) view.findViewById(R.id.entity_photo);
		holder.name = (TextView) view.findViewById(R.id.name);
		holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
		holder.type = (TextView) view.findViewById(R.id.type);
		holder.description = (TextView) view.findViewById(R.id.description);
		holder.creator = (UserView) view.findViewById(R.id.creator);
		holder.area = (TextView) view.findViewById(R.id.area);
		holder.createdDate = (TextView) view.findViewById(R.id.created_date);
		holder.comments = (TextView) view.findViewById(R.id.comments);
		holder.checked = (CheckBox) view.findViewById(R.id.checked);
		holder.overflow = (ComboButton) view.findViewById(R.id.button_overflow);

		if (holder.checked != null) {
			holder.checked.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					final CheckBox checkBox = (CheckBox) view;
					final Entity entity = (Entity) checkBox.getTag();
					entity.checked = checkBox.isChecked();
				}
			});
		}

		holder.parent = (EntityView) view.findViewById(R.id.parent);
		holder.userPhotoView = (AirImageView) view.findViewById(R.id.user_photo);
		holder.userName = (TextView) view.findViewById(R.id.user_name);
		holder.placeName = (TextView) view.findViewById(R.id.place_name);
		holder.toName = (TextView) view.findViewById(R.id.to_name);

		if (mListView instanceof GridView) {
			Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
			final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
			holder.photoView.getImageView().setLayoutParams(params);
			holder.photoView.getMissingMessage().setLayoutParams(params);
		}

		return holder;
	}

	@SuppressWarnings("ucd")
	public void removeHeaderView() {
		if (mHeaderView != null) {
			((ListView) mListView).removeHeaderView(mHeaderView);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public String getActivityTitle() {
		BaseActivity activity = (BaseActivity) getSherlockActivity();
		return (String) ((activity.getActivityTitle() != null) ? activity.getActivityTitle() : getSherlockActivity().getTitle());
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

	public Boolean getReverseSort() {
		return mReverseSort;
	}

	@SuppressWarnings("ucd")
	public EntityListFragment setReverseSort(Boolean reverseSort) {
		mReverseSort = reverseSort;
		return this;
	}

	public View getHeaderView() {
		return mHeaderView;
	}

	public IMonitor getMonitor() {
		return mMonitor;
	}

	public IQuery getQuery() {
		return mQuery;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

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
		if (mSelfBindingEnabled && (getSherlockActivity() != null && !getSherlockActivity().isFinishing())) {
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

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		Logger.d(this, "Preparing options menu");
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	protected class ListAdapter extends ArrayAdapter<Entity> {

		public ListAdapter(List<Entity> entities) {
			super(getSherlockActivity(), 0, entities);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (mListPagingEnabled && mQuery != null && mQuery.isMore() && position == mEntities.size()) return mLoadingView;

			View view = convertView;
			final ViewHolder holder;

			if (mListPagingEnabled && position >= mEntities.size() && position < (mVisibleColumns * mVisibleRows)) {
				if (view == null || view.findViewById(R.id.item_placeholder) == null) {
					view = LayoutInflater.from(getSherlockActivity()).inflate(R.layout.temp_list_item_empty, null);
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

				if (view == null
						|| view.findViewById(R.id.animator_more) != null
						|| view.findViewById(R.id.item_placeholder) != null) {
					view = LayoutInflater.from(getSherlockActivity()).inflate(mListItemResId, null);
					holder = bindHolder(view, null);
					view.setTag(holder);
				}
				else {
					holder = (ViewHolder) view.getTag();
				}

				if (entity != null) {

					drawListItem(entity, view, holder);

					holder.data = entity;
					holder.position = position;
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

	protected void drawListItem(Entity entity, View view, final ViewHolder holder) {

		IEntityController controller = Aircandi.getInstance().getControllerForEntity(entity);

		/* Candi View */

		UI.setVisibility(holder.candiView, View.GONE);
		if (holder.candiView != null) {
			holder.candiView.databind(entity, new IndicatorOptions());
			UI.setVisibility(holder.candiView, View.VISIBLE);
			return;
		}

		/* Checkbox */

		UI.setVisibility(holder.checked, View.GONE);
		if (holder.checked != null && entity.checked != null) {
			holder.checked.setChecked(entity.checked);
			holder.checked.setTag(entity);
			UI.setVisibility(holder.checked, View.VISIBLE);
		}

		/* Overflow button */

		UI.setVisibility(holder.overflow, View.GONE);
		if (holder.overflow != null) {
			holder.overflow.setTag(entity);
			UI.setVisibility(holder.overflow, View.VISIBLE);
		}

		/* Name */

		UI.setVisibility(holder.name, View.GONE);
		if (holder.name != null && entity.name != null && entity.name.length() > 0) {
			holder.name.setText(entity.name);
			UI.setVisibility(holder.name, View.VISIBLE);
		}

		/* Subtitle */

		UI.setVisibility(holder.subtitle, View.GONE);
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) entity;
			if (holder.subtitle != null) {
				if (place.subtitle != null) {
					holder.subtitle.setText(place.subtitle);
					UI.setVisibility(holder.subtitle, View.VISIBLE);
				}
				else {
					if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
						holder.subtitle.setText(Html.fromHtml(place.category.name));
						UI.setVisibility(holder.subtitle, View.VISIBLE);
					}
				}
			}
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			String subtitle = null;
			if (entity.type.equals(Constants.TYPE_APP_WEBSITE)) {
				subtitle = ((Applink) entity).appUrl;
			}
			else if (entity.type.equals(Constants.TYPE_APP_EMAIL)) {
				subtitle = ((Applink) entity).appId;
			}
			else if (entity.type.equals(Constants.TYPE_APP_OPENTABLE)
					|| entity.type.equals(Constants.TYPE_APP_URBANSPOON)
					|| entity.type.equals(Constants.TYPE_APP_TRIPADVISOR)) {
				subtitle = ((Applink) entity).appUrl;
			}
			else if (entity.type.equals(Constants.TYPE_APP_GOOGLEPLUS)) {
				subtitle = ((Applink) entity).appUrl;
			}

			if (subtitle != null) {
				holder.subtitle.setText(subtitle);
				UI.setVisibility(holder.subtitle, View.VISIBLE);
			}
		}
		else {
			if (holder.subtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
				holder.subtitle.setText(entity.subtitle);
				UI.setVisibility(holder.subtitle, View.VISIBLE);
			}
		}

		/* Type */

		UI.setVisibility(holder.type, View.GONE);
		if (holder.type != null && entity.type != null && entity.type.length() > 0) {

			String type = entity.type;
			String typeVerbose = controller.getType(entity, true);
			if (typeVerbose != null) {
				type = typeVerbose;
			}

			if (type.equals(Constants.TYPE_APP_GOOGLEPLUS)) {
				type = type.replaceFirst("plus", "+");
			}

			holder.type.setText(type);
			UI.setVisibility(holder.type, View.VISIBLE);
		}

		/* Description */

		UI.setVisibility(holder.description, View.GONE);
		if (holder.description != null && entity.description != null && entity.description.length() > 0) {
			holder.description.setText(entity.description);
			UI.setVisibility(holder.description, View.VISIBLE);
		}

		/* Place context */

		UI.setVisibility(holder.placeName, View.GONE);
		if (holder.placeName != null) {
			Entity parentEntity = entity.place;
			if (parentEntity == null) {
				parentEntity = EntityManager.getCacheEntity(entity.placeId);
			}
			if (parentEntity != null) {
				holder.placeName.setText(parentEntity.name);
				UI.setVisibility(holder.placeName, View.VISIBLE);
			}
		}

		/* Comments */

		UI.setVisibility(holder.comments, View.GONE);
		if (holder.comments != null) {
			Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, null, Direction.in);
			Integer commentCount = (count != null) ? count.count.intValue() : 0;
			if (commentCount != null && commentCount > 0) {
				holder.comments.setText(String.valueOf(commentCount) + ((commentCount == 1) ? " Comment" : " Comments"));
				holder.comments.setTag(entity);
				UI.setVisibility(holder.comments, View.VISIBLE);
			}
		}

		/* Creator */

		UI.setVisibility(holder.creator, View.GONE);
		if (holder.creator != null && entity.creator != null) {
			if (!entity.ownerId.equals(ServiceConstants.ADMIN_USER_ID)
					&& !entity.ownerId.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
				holder.creator.databind(entity.creator, entity.modifiedDate.longValue(), entity.locked);
				UI.setVisibility(holder.creator, View.VISIBLE);
			}
		}

		/* User photo */

		UI.setVisibility(holder.userPhotoView, View.GONE);
		if (holder.userPhotoView != null && entity.creator != null) {
			/*
			 * Acting a cheap proxy for user view so setting photoview to entity instead of photo.
			 */
			Photo photo = entity.creator.getPhoto();
			if (holder.userPhotoView.getPhoto() == null || !holder.userPhotoView.getPhoto().getUri().equals(photo.getUri())) {
				holder.userPhotoView.setTag(entity.creator);
				UI.drawPhoto(holder.userPhotoView, photo);
			}
			UI.setVisibility(holder.userPhotoView, View.VISIBLE);
		}

		/* User name */

		UI.setVisibility(holder.userName, View.GONE);
		if (holder.userName != null && entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
			holder.userName.setText(entity.creator.name);
			UI.setVisibility(holder.userName, View.VISIBLE);
		}

		/* User area */

		UI.setVisibility(holder.area, View.GONE);
		if (holder.area != null && entity.creator != null && entity.creator.area != null && entity.creator.area.length() > 0) {
			holder.area.setText(entity.creator.area);
			UI.setVisibility(view.findViewById(R.id.separator), View.VISIBLE);
			UI.setVisibility(holder.area, View.VISIBLE);
		}
		else {
			UI.setVisibility(view.findViewById(R.id.separator), View.GONE);
		}

		/* Created date */

		UI.setVisibility(holder.createdDate, View.GONE);
		if (holder.createdDate != null && entity.createdDate != null) {
			String compactAgo = DateTime.dateStringAt(entity.createdDate.longValue());
			holder.createdDate.setText(compactAgo);
			UI.setVisibility(holder.createdDate, View.VISIBLE);
		}

		/* Parent context */

		UI.setVisibility(holder.parent, View.GONE);
		if (entity.toId != null && holder.parent != null) {
			Entity parentEntity = EntityManager.getCacheEntity(entity.toId);
			if (parentEntity != null) {
				holder.parent.databind(parentEntity);
				UI.setVisibility(holder.parent, View.VISIBLE);
			}
		}

		/* Photo */

		UI.setVisibility(holder.photoView, View.GONE);
		if (holder.photoView != null) {
			final Photo photo = entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT) ? entity.creator.getPhoto() : entity.getPhoto();

			if (photo != null) {
				if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
					UI.drawPhoto(holder.photoView, photo);
				}
				UI.setVisibility(holder.photoView, View.VISIBLE);
			}
		}

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

	public static class ViewHolder {

		public CandiView	candiView;
		public AirImageView	photoView;
		public TextView		name;
		public TextView		subtitle;
		public TextView		description;
		public TextView		type;
		public TextView		createdDate;
		public UserView		creator;
		public TextView		userName;
		public AirImageView	userPhotoView;
		public TextView		placeName;
		public TextView		toName;		// NO_UCD (unused code)
		public TextView		area;
		public EntityView	parent;

		public ComboButton	overflow;
		public CheckBox		checked;
		public Integer		position;		// Used to optimize item view rendering // NO_UCD (unused code)

		public String		photoUri;		// Used for verification after fetching image // NO_UCD (unused code)
		public Object		data;			// object binding to
		public TextView		comments;

	}

	public static class ViewType {
		public static String	LIST	= "list";
		public static String	GRID	= "grid";
	}

	public static class Highlight {
		private Boolean	oneShot	= false;
		private Boolean	fired	= false;

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