package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MapManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.monitors.TrendMonitor;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.queries.NotificationsQuery;
import com.aircandi.queries.TrendQuery;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.components.EmptyController;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
public class AircandiForm extends BaseActivity {

	protected Number  mPauseDate;
	protected Boolean mConfiguredForAnonymous;

	protected Fragment        mFragmentNotifications;
	protected EmptyController mDrawerEmptyController;

	protected Boolean mFinishOnClose   = false;
	protected Boolean mLeftDrawerOpen  = false;
	protected Boolean mRightDrawerOpen = false;

	protected String mTitle = StringManager.getString(R.string.name_app);
	protected UserView   mUserView;
	protected CacheStamp mCacheStamp;

	protected View mCurrentNavView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		View view = findViewById(R.id.item_nearby);
		if (view != null) {
			FontManager.getInstance().setTypefaceMedium((TextView) view.findViewById(R.id.name));
		}

		mUserView = (UserView) findViewById(R.id.user_current);
		if (mUserView != null) {
			mUserView.setTag(Patchr.getInstance().getCurrentUser());
		}

		mDrawerLeft = findViewById(R.id.left_drawer);
		mDrawerRight = findViewById(R.id.right_drawer);
		mDrawerEmptyController = new EmptyController(findViewById(R.id.empty_view_notifications).findViewById(R.id.empty_message));

		/* Check if the device is tethered */
		tetherAlert();

		/* Default fragment */
		mNextFragmentTag = Constants.FRAGMENT_TYPE_NEARBY;

		/* Notifications fragment */
		mFragmentNotifications = new NotificationListFragment();

		EntityMonitor monitor = new EntityMonitor(Patchr.getInstance().getCurrentUser().id);
		NotificationsQuery query = new NotificationsQuery();

		query.setEntityId(Patchr.getInstance().getCurrentUser().id)
		     .setPageSize(Integers.getInteger(R.integer.page_size_notifications))
		     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

		((EntityListFragment) mFragmentNotifications)
				.setMonitor(monitor)
				.setQuery(query)
				.setListViewType(ViewType.LIST)
				.setListLayoutResId(R.layout.notification_list_fragment)
				.setListItemResId(R.layout.temp_listitem_notification)
				.setListLoadingResId(R.layout.temp_listitem_loading_notifications)
				.setListEmptyMessageResId(R.string.label_notifications_empty)
				.setFabEnabled(false)
				.setSelfBindingEnabled(false);

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder_notifications, mFragmentNotifications)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onRefresh() {
		/*
		 * Triggers either searchForPlaces or bind(BindingMode.MANUAL).
		 */
		if (mCurrentFragment != null) {
			((BaseFragment) mCurrentFragment).onRefresh();
		}
	}

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {
		/*
		 * Can be called from ui or background thread.
		 */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				mBusy.hide(false);
				/*
				 * Notification list
				 */
				final EntityListFragment fragmentNotifications = (EntityListFragment) mFragmentNotifications;
				if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
					final Integer countNotifications = fragmentNotifications.getAdapter().getCount();
					fragmentNotifications.onProcessingFinished();
					if (mDrawerEmptyController.isEnabled()) {
						if (countNotifications == 0) {
							mDrawerEmptyController.setText(fragmentNotifications.getListEmptyMessageResId());
							mDrawerEmptyController.fadeIn();
						}
						else {
							mDrawerEmptyController.fadeOut();
						}
					}
				}
				/*
				 * Current list fragment
				 */
				if (mCurrentFragment instanceof EntityListFragment) {
					final EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
					final Integer count = fragment.getAdapter().getCount();

					((BaseFragment) mCurrentFragment).onProcessingFinished();

					if (mEmptyView.isEnabled()) {
						if (count == 0) {
							mEmptyView.setText(fragment.getListEmptyMessageResId());
							mEmptyView.fadeIn();
						}
						else {
							mEmptyView.fadeOut();
						}
					}

					if (mCurrentFragment instanceof NearbyListFragment) {
						if (!NetworkManager.getInstance().isWifiEnabled()
								&& !LocationManager.getInstance().isLocationAccessEnabled()) {
							mFab.fadeOut();
						}
					}
				}
				else if (mCurrentFragment instanceof MapListFragment) {
					((MapListFragment) mCurrentFragment).onProcessingFinished();
				}
			}
		});
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (!(entity.schema.equals(Constants.SCHEMA_ENTITY_USER) && ((User) entity).isAnonymous())) {
			Bundle extras = new Bundle();
			if (Type.isTrue(entity.autowatchable)) {
				extras.putBoolean(Constants.EXTRA_PRE_APPROVED, true);
			}
			Patchr.dispatch.route(this, Route.BROWSE, entity, extras);
		}
		if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
			mDrawerLayout.closeDrawer(mDrawerLeft);
		}
	}

	@SuppressWarnings("ucd")
	public void onDrawerItemClick(View view) {

		String tag = (String) view.getTag();
		if (!tag.equals(Constants.FRAGMENT_TYPE_SETTINGS)
				&& !tag.equals(Constants.FRAGMENT_TYPE_FEEDBACK)) {
			mCurrentNavView = view;
			updateDrawer();
		}
		mNextFragmentTag = (String) view.getTag();
		mDrawerLayout.closeDrawer(mDrawerLeft);
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
		fragment.onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onMoreNotificationsButtonClick(View view) {
		EntityListFragment fragment = (EntityListFragment) mFragmentNotifications;
		fragment.onMoreButtonClick(view);
	}

	@Override
	public void onAdd(Bundle extras) {
		if (!extras.containsKey(Constants.EXTRA_ENTITY_SCHEMA)) {
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
		}
		extras.putString(Constants.EXTRA_MESSAGE, StringManager.getString(R.string.label_message_new_message));
		Patchr.dispatch.route(this, Route.NEW, null, extras);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerToggle != null) {
			mDrawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final NotificationEvent event) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mCurrentFragment != null && mCurrentFragment instanceof BaseFragment) {
					((BaseFragment) mCurrentFragment).bind(BindingMode.AUTO);
				}
				mDrawerEmptyController.fadeOut();
				updateNotificationIndicator(false);
			}
		});
	}

	@SuppressWarnings("ucd")
	public void onFabButtonClick(View view) {
		Patchr.dispatch.route(this, Route.NEW_PLACE, null, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public synchronized void setCurrentFragment(String fragmentType) {
		/*
		 * Called from BaseActivity.onCreate (once), configureActionBar and DispatchManager.
		 */
		Logger.i(this, "setCurrentFragment entered");
		/*
		 * Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		Fragment fragment;

		if (mFragments.containsKey(fragmentType)) {
			fragment = mFragments.get(fragmentType);
		}
		else {

			/* Nearby */

			if (fragmentType.equals(Constants.FRAGMENT_TYPE_NEARBY)) {

				fragment = new NearbyListFragment()
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.nearby_fragment)
						.setListItemResId(R.layout.temp_listitem_nearby)
						.setListEmptyMessageResId(R.string.label_radar_empty)
						.setHeaderViewResId(R.layout.widget_list_header_nearby)
						.setTitleResId(R.string.form_title_nearby);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Watching */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_WATCH)) {

				fragment = new EntityListFragment();

				EntityMonitor monitor = new EntityMonitor(Patchr.getInstance().getCurrentUser().id);
				EntitiesQuery query = new EntitiesQuery();

				query.setEntityId(Patchr.getInstance().getCurrentUser().id)
				     .setLinkDirection(Link.Direction.out.name())
				     .setLinkType(Constants.TYPE_LINK_WATCH)
				     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
				     .setSchema(Constants.SCHEMA_ENTITY_PATCH);

				((EntityListFragment) fragment)
						.setMonitor(monitor)
						.setQuery(query)
						.setListPagingEnabled(true)
						.setListItemResId(R.layout.temp_listitem_patch)
						.setListLoadingResId(R.layout.temp_listitem_loading)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.patch_list_fragment)
						.setListEmptyMessageResId(R.string.label_watching_empty)
						.setTitleResId(R.string.form_title_watch)
						.setSelfBindingEnabled(true);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Owner */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_CREATE)) {

				fragment = new EntityListFragment();

				EntityMonitor monitor = new EntityMonitor(Patchr.getInstance().getCurrentUser().id);
				EntitiesQuery query = new EntitiesQuery();

				query.setEntityId(Patchr.getInstance().getCurrentUser().id)
				     .setLinkDirection(Link.Direction.out.name())
				     .setLinkType(Constants.TYPE_LINK_CREATE)
				     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
				     .setSchema(Constants.SCHEMA_ENTITY_PATCH);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_patch)
				                               .setListLoadingResId(R.layout.temp_listitem_loading)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.patch_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_created_empty)
				                               .setTitleResId(R.string.form_title_create)
				                               .setSelfBindingEnabled(true);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Trending popular */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_TREND_POPULAR)) {

				fragment = new TrendListFragment();

				TrendMonitor monitor = new TrendMonitor();
				TrendQuery query = new TrendQuery();

				query.setToSchema(Constants.SCHEMA_ENTITY_PATCH)
				     .setFromSchema(Constants.SCHEMA_ENTITY_USER)
				     .setTrendType(Constants.TYPE_LINK_WATCH);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(false)
				                               .setEntityCacheEnabled(false)
				                               .setHeaderViewResId(R.layout.widget_list_header_trends_popular)
				                               .setListItemResId(R.layout.temp_listitem_trends)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.trends_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_created_empty)
				                               .setTitleResId(R.string.form_title_trends_popular)
				                               .setSelfBindingEnabled(true);

				((TrendListFragment) fragment).setCountLabelResId(R.string.label_trends_count_popular);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Trending active */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_TREND_ACTIVE)) {

				fragment = new TrendListFragment();

				TrendMonitor monitor = new TrendMonitor();
				TrendQuery query = new TrendQuery();

				query.setToSchema(Constants.SCHEMA_ENTITY_PATCH)
				     .setFromSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				     .setTrendType(Constants.TYPE_LINK_CONTENT);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(false)
				                               .setEntityCacheEnabled(false)
				                               .setHeaderViewResId(R.layout.widget_list_header_trends_active)
				                               .setListItemResId(R.layout.temp_listitem_trends)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.trends_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_created_empty)
				                               .setTitleResId(R.string.form_title_trends_active)
				                               .setSelfBindingEnabled(true);

				((TrendListFragment) fragment).setCountLabelResId(R.string.label_trends_count_active);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_SETTINGS)) {

				mNextFragmentTag = mCurrentFragmentTag;
				Patchr.dispatch.route(this, Route.SETTINGS, null, null);
				return;
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_FEEDBACK)) {

				mNextFragmentTag = mCurrentFragmentTag;
				Patchr.dispatch.route(this, Route.FEEDBACK, null, null);
				return;
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_MAP)) {

				fragment = new MapListFragment();
				((MapListFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_list);
			}

			else {

				return;
			}

			mFragments.put(fragmentType, fragment);
		}

		if (fragment instanceof BaseFragment) {
			setActivityTitle(StringManager.getString(((BaseFragment) fragment).getTitleResId()));
		}
		else if (fragment instanceof MapListFragment) {
			((MapListFragment) fragment)
					.setEntities(((EntityListFragment) getCurrentFragment()).getEntities())
					.setTitleResId(((EntityListFragment) getCurrentFragment()).getTitleResId())
					.setListFragment(mCurrentFragmentTag)
					.setZoomLevel(null);

			if (!mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				((MapListFragment) fragment).setZoomLevel(MapManager.ZOOM_SCALE_COUNTY);
			}

			mFab.fadeOut();
		}

		if (fragment instanceof EntityListFragment) {
			mFab.fadeIn();
		}

		/* We hide/show to save the trouble of rebuilding the fragments. */
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
		if (mCurrentFragment != null) {
			ft.hide(mCurrentFragment);
		}
		if (fragment.isHidden()) {
			ft.show(fragment);
		}
		else {
			ft.add(R.id.fragment_holder, fragment);
		}
		ft.commit();

		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
		mCurrentFragment = fragment;
		Logger.i(this, "setCurrentFragment exited");
	}

	public Fragment getCurrentFragment() {
		return mCurrentFragment;
	}

	private void tetherAlert() {
	    /*
	     * We alert that wifi isn't enabled. If the user ends up enabling wifi,
		 * we will get that event and refresh radar with beacon support.
		 */
		Boolean tethered = NetworkManager.getInstance().isWifiTethered();
		if (tethered || (!NetworkManager.getInstance().isWifiEnabled() && !Patchr.usingEmulator)) {

			UI.showToastNotification(StringManager.getString(tethered
			                                                 ? R.string.alert_wifi_tethered
			                                                 : R.string.alert_wifi_disabled), Toast.LENGTH_SHORT);
		}
	}

	public void updateNotificationIndicator(Boolean ifDrawerVisible) {

		Logger.v(this, "updateNotificationIndicator for menus");

		Boolean showingNotifications = (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.END));
		Integer newNotificationCount = NotificationManager.getInstance().getNewNotificationCount();

		if ((ifDrawerVisible || !showingNotifications) && mNotificationsBadgeGroup != null) {
			if (newNotificationCount > 0) {
				mNotificationsBadgeCount.setText(String.valueOf(newNotificationCount));
				mNotificationsBadgeGroup.setVisibility(View.VISIBLE);
			}
			else {
				mNotificationsBadgeGroup.setVisibility(View.GONE);
			}
		}
	}

	protected void updateDrawer() {
		if (mCurrentNavView != null) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_watch).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_create).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_trend_activity).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_trend_popular).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_more_settings).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_more_feedback).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceMedium((TextView) mCurrentNavView.findViewById(R.id.name));
		}
	}

	protected void scrollToTopOfList() {
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			AbsListView list = ((EntityListFragment) mCurrentFragment).getListView();
			((ListView) list).setSelectionAfterHeaderView();
		}
	}

	protected void configureDrawer() {

		Boolean configChange = mConfiguredForAnonymous == null
				|| !Patchr.getInstance().getCurrentUser().isAnonymous().equals(mConfiguredForAnonymous)
				|| (mCacheStamp != null && !mCacheStamp.equals(Patchr.getInstance().getCurrentUser().getCacheStamp()));

		if (configChange) {
			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				mConfiguredForAnonymous = true;
				findViewById(R.id.item_watch).setVisibility(View.GONE);
				findViewById(R.id.item_create).setVisibility(View.GONE);
				mUserView.databind(Patchr.getInstance().getCurrentUser());
			}
			else {
				mConfiguredForAnonymous = false;
				findViewById(R.id.item_watch).setVisibility(View.VISIBLE);
				findViewById(R.id.item_create).setVisibility(View.VISIBLE);
				mUserView.databind(Patchr.getInstance().getCurrentUser());
				mCacheStamp = Patchr.getInstance().getCurrentUser().getCacheStamp();
			}
		}
	}

	protected void configureActionBar() {
		super.configureActionBar();
		/*
		 * Only called when form is created
		 */
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setFocusableInTouchMode(false);
		mDrawerToggle = new ActionBarDrawerToggle(this
				, mDrawerLayout
				, getActionBarToolbar()
				, R.string.label_drawer_open
				, R.string.label_drawer_close) {

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);

				if (drawerView.getId() == R.id.left_drawer) {
					if (!mNextFragmentTag.equals(mCurrentFragmentTag)) {
						setCurrentFragment(mNextFragmentTag);
					}
				}
				else if (drawerView.getId() == R.id.right_drawer) {
					NotificationManager.getInstance().setNewNotificationCount(0);
					updateNotificationIndicator(false);
				}
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);

				if (drawerView.getId() == R.id.right_drawer) {
					NotificationManager.getInstance().setNewNotificationCount(0);
					NotificationManager.getInstance().cancelNotifications();
					updateNotificationIndicator(true);
					((BaseFragment) mFragmentNotifications).bind(BindingMode.AUTO);
				}
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);

				if (drawerView.getId() == R.id.right_drawer) {
					mNotificationActionIcon.setRotation(90 * slideOffset);
				}
			}
		};

		/* Set the drawer toggle as the DrawerListener */
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}

		getActionBarToolbar().setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
					mNotificationActionIcon.animate().rotation(0f).setDuration(200);
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
				else if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
					mDrawerLayout.closeDrawer(mDrawerLeft);
				}
				else {
					mDrawerLayout.openDrawer(Gravity.START);
				}
			}
		});
	}

	@Override
	protected int getLayoutId() {
		return R.layout.aircandi_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		super.onStart();

		/* Make sure we are configured properly depending on user status */
		configureDrawer();
	}

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * Lifecycle ordering: (onCreate/onRestart)->onStart->onResume->onAttachedToWindow->onWindowFocusChanged
		 * 
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus.
		 */
		Patchr.getInstance().setCurrentPatch(null);
		Logger.v(this, "Setting current patch to null");
		if (mPauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}

		/* Manage activity alert */
		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			updateNotificationIndicator(false);
		}

		/* In case the user was edited from the drawer */
		if (mUserView != null) {
			mUserView.databind(Patchr.getInstance().getCurrentUser());
		}
	}

	@Override
	protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible. Does not fire if the activity window
		 * loses focus but the activity is still active.
		 */
		mPauseDate = DateTime.nowDate().getTime();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		/*
		 * The activity is getting destroyed but the application level state
		 * like singletons, statics, etc will continue as long as the application
		 * is running.
		 */
		Logger.d(this, "Destroyed");
		super.onDestroy();
	}
}