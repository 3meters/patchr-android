package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.BusyManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MapManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingCompleteEvent;
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
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Locale;

/*
 * Library Notes
 * 
 * - AWS: We are using the minimum libraries: core and S3. We could do the work to call AWS without their
 * libraries which should give us the biggest savings.
 */

/*
 * Threading Notes
 * 
 * - AsyncTasks: AsyncTask uses a static internal work queue with a hard-coded limit of 10 elements.
 * Once we have 10 tasks going concurrently, task 11 causes a RejectedExecutionException. ThreadPoolExecutor is a way to
 * get more control over thread pooling but it requires Android version 11/3.0 (we currently target 9/2.3 and higher).
 * AsyncTasks are hard-coded with a low priority and continue their work even if the activity is paused.
 */

/*
 * Lifecycle event sequences from Radar
 * 
 * First Launch: onCreate->onStart->onResume
 * Home: Pause->Stop->||Restart->Start->Resume
 * Back: Pause->Stop->Destroyed
 * Other Candi Activity: Pause->Stop||Restart->Start->Resume
 * 
 * Alert Dialog: none
 * Dialog Activity: Pause||Resume
 * Overflow menu: none
 * ProgressIndicator: none
 * 
 * Preferences: Pause->Stop->||Restart->Start->Resume
 * Profile: Pause->Stop->||Restart->Start->Resume
 * 
 * Power off with Aircandi in foreground: Pause->Stop
 * Power on with Aircandi in foreground: Nothing
 * Unlock screen with Aircandi in foreground: Restart->Start->Resume
 */

@SuppressLint("Registered")
public class AircandiForm extends BaseActivity {

	protected Number  mPauseDate;
	protected Boolean mConfiguredForAnonymous;

	protected DrawerLayout          mDrawerLayout;
	protected View                  mDrawerLeft;
	protected View                  mDrawerRight;
	protected ActionBarDrawerToggle mDrawerToggle;
	protected Fragment              mFragmentNotifications;
	protected View                  mNotificationsBadgeGroup;
	protected TextView              mNotificationsBadgeCount;
	protected View                  mNotificationActionIcon;

	protected Boolean mFinishOnClose   = false;
	protected Boolean mLeftDrawerOpen  = false;
	protected Boolean mRightDrawerOpen = false;

	protected String mTitle = StringManager.getString(R.string.name_app);
	protected UserView   mUserView;
	protected CacheStamp mCacheStamp;

	protected View                  mCurrentNavView;
	protected ToolTipRelativeLayout mTooltips;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mActionBar == null) {
			Patchr.firstStartIntent = getIntent();
			Patchr.dispatch.route(this, Route.SPLASH, null, null, null);
		}
		else {
			FontManager.getInstance().setTypefaceMedium((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
		}
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		/* Ui init */
		Integer drawerIconResId = R.drawable.ic_navigation_drawer_dark;
		if (Patchr.themeTone.equals(Patchr.ThemeTone.LIGHT)) {
			drawerIconResId = R.drawable.ic_navigation_drawer_light;
		}

		mUserView = (UserView) findViewById(R.id.user_current);
		mUserView.setTag(Patchr.getInstance().getCurrentUser());

		mDrawerLeft = findViewById(R.id.left_drawer);
		mDrawerRight = findViewById(R.id.right_drawer);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setFocusableInTouchMode(false);

		mDrawerToggle = new ActionBarDrawerToggle(this
				, mDrawerLayout
				, drawerIconResId
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
					updateNotificationIndicator();
				}
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);

				if (drawerView.getId() == R.id.right_drawer) {
					NotificationManager.getInstance().setNewNotificationCount(0);
					NotificationManager.getInstance().cancelNotifications();
					((BaseFragment) mFragmentNotifications).bind(BindingMode.AUTO);
				}
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);

				if (drawerView.getId() == R.id.left_drawer) {
					if (slideOffset > .55 && !mLeftDrawerOpen) {
						mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerRight);
						setActivityTitle(mTitle);
						leftDrawerState(true, mMenu);
						mLeftDrawerOpen = true;
					}
					else if (slideOffset < .45 && mLeftDrawerOpen) {
						if (!mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
							setActivityTitle(StringManager.getString(((BaseFragment) mCurrentFragment).getTitleResId()));
						}
						leftDrawerState(false, mMenu);
						mLeftDrawerOpen = false;
					}
				}
				else if (drawerView.getId() == R.id.right_drawer) {
					if (slideOffset > .55 && !mRightDrawerOpen) {
						mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerRight);
						setActivityTitle(StringManager.getString(R.string.label_notifications_title));
						mRightDrawerOpen = true;
					}
					else if (slideOffset < .45 && mRightDrawerOpen) {
						mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerRight);
						setActivityTitle(mTitle);
						mRightDrawerOpen = false;
					}
				}
			}
		};

		/* Set the drawer toggle as the DrawerListener */
		mDrawerLayout.setDrawerListener(mDrawerToggle);

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
		     .setSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

		((EntityListFragment) mFragmentNotifications)
				.setMonitor(monitor)
				.setQuery(query)
				.setListViewType(ViewType.LIST)
				.setListLayoutResId(R.layout.notification_list_fragment)
				.setListItemResId(R.layout.temp_listitem_notification)
				.setListLoadingResId(R.layout.temp_listitem_loading_notifications)
				.setListEmptyMessageResId(R.string.label_notifications_empty)
				.setSelfBindingEnabled(false)
				.setFabEnabled(false)
				.setTitleResId(R.string.label_feed_alerts_title);

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder_notifications, mFragmentNotifications)
				.commit();

		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		mTooltips.setSingleShot(Constants.TOOLTIPS_PATCH_LIST_ID);
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

	@Override
	protected void configureActionBar() {
	    /*
	     * Only called when form is created
		 */
		super.configureActionBar();
		if (mActionBar != null) {
			if (mDrawerLayout != null) {
				mActionBar.setHomeButtonEnabled((mDrawerLayout.getDrawerLockMode(mDrawerLeft) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED));
				mActionBar.setDisplayHomeAsUpEnabled((mDrawerLayout.getDrawerLockMode(mDrawerLeft) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED));
			}
		}
	}

	protected void setActionBarIcon() {
		super.setActionBarIcon();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onBackPressed() {
		if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
			mFab.click();
		}
		else {
			if (mDrawerLayout.isDrawerVisible(mDrawerLeft)) {
				onCancel(false);
			}
			else {
				if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
					mNotificationActionIcon.animate().rotation(0f).setDuration(200);
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
				else {
					mDrawerLayout.openDrawer(mDrawerLeft);
				}
			}
		}
	}

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
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		if (mCurrentFragment instanceof EntityListFragment) {
			((EntityListFragment) mCurrentFragment).onProcessingComplete();
		}
		else if (mCurrentFragment instanceof MapListFragment) {
			((MapListFragment) mCurrentFragment).onProcessingComplete();
		}
		((EntityListFragment) mFragmentNotifications).onProcessingComplete();
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (!(entity.schema.equals(Constants.SCHEMA_ENTITY_USER) && ((User) entity).isAnonymous())) {
			Bundle extras = new Bundle();
			if (Type.isTrue(entity.autowatchable)) {
				if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
						, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
					extras.putBoolean(Constants.EXTRA_AUTO_WATCH, true);
				}
			}
			Patchr.dispatch.route(this, Route.BROWSE, entity, null, extras);
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
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);
		}
		extras.putString(Constants.EXTRA_MESSAGE, StringManager.getString(R.string.label_message_new_message));
		Patchr.dispatch.route(this, Route.NEW, null, null, extras);
	}

	@Override
	public void onHelp() {
		if (mCurrentFragment != null) {
			((BaseFragment) mCurrentFragment).onHelp();
		}
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
				updateNotificationIndicator();
			}
		});
	}

	@SuppressWarnings("ucd")
	public void onFabButtonClick(View view) {
		mNextFragmentTag = (String) view.getTag();
		setCurrentFragment(mNextFragmentTag);
		onPrepareOptionsMenu(mMenu);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void setCurrentFragment(String fragmentType) {
		Logger.i(this, "setCurrentFragment called");
		/*
		 * Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		Fragment fragment = null;

		if (mFragments.containsKey(fragmentType)) {
			fragment = mFragments.get(fragmentType);
		}
		else {

			/* Nearby */

			if (fragmentType.equals(Constants.FRAGMENT_TYPE_NEARBY)) {

				fragment = new RadarListFragment()
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.radar_fragment)
						.setListItemResId(R.layout.temp_listitem_radar)
						.setListEmptyMessageResId(R.string.label_radar_empty)
						.setHeaderViewResId(R.layout.widget_list_header_nearby)
						.setFabEnabled(true)
						.setTitleResId(R.string.label_radar_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh_special);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
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
				     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_place)
				                               .setListLoadingResId(R.layout.temp_listitem_loading)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.place_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_watching_empty)
				                               .setTitleResId(R.string.label_watch_title)
				                               .setFabEnabled(true)
				                               .setSelfBindingEnabled(true);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
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
				     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_place)
				                               .setListLoadingResId(R.layout.temp_listitem_loading)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.place_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_created_empty)
				                               .setTitleResId(R.string.label_create_title)
				                               .setFabEnabled(true)
				                               .setSelfBindingEnabled(true);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Trending popular */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_TREND_POPULAR)) {

				fragment = new TrendListFragment();

				TrendMonitor monitor = new TrendMonitor();
				TrendQuery query = new TrendQuery();

				query.setToSchema(Constants.SCHEMA_ENTITY_PLACE)
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
				                               .setTitleResId(R.string.label_trends_popular)
				                               .setFabEnabled(true)
				                               .setSelfBindingEnabled(true);

				((TrendListFragment) fragment).setCountLabelResId(R.string.label_trends_count_popular);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Trending active */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_TREND_ACTIVE)) {

				fragment = new TrendListFragment();

				TrendMonitor monitor = new TrendMonitor();
				TrendQuery query = new TrendQuery();

				query.setToSchema(Constants.SCHEMA_ENTITY_PLACE)
				     .setFromSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)
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
				                               .setTitleResId(R.string.label_trends_active)
				                               .setFabEnabled(true)
				                               .setSelfBindingEnabled(true);

				((TrendListFragment) fragment).setCountLabelResId(R.string.label_trends_count_active);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_SETTINGS)) {

				mNextFragmentTag = mCurrentFragmentTag;
				Patchr.dispatch.route(this, Route.SETTINGS, null, null, null);
				return;
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_FEEDBACK)) {

				mNextFragmentTag = mCurrentFragmentTag;
				Patchr.dispatch.route(this, Route.FEEDBACK, null, null, null);
				return;
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_MAP)) {

				fragment = new MapListFragment();
			}

			else {

				return;
			}

			mFragments.put(fragmentType, fragment);
		}

		if (!fragmentType.equals(Constants.FRAGMENT_TYPE_MAP)) {
			setActivityTitle(StringManager.getString(((BaseFragment) fragment).getTitleResId()));
			mFab.setEnabled(((BaseFragment) fragment).getFabEnabled());
		}
		else {
			((MapListFragment) fragment)
					.setEntities(((EntityListFragment) getCurrentFragment()).getEntities())
					.setTitleResId(((EntityListFragment) getCurrentFragment()).getTitleResId())
					.setZoomLevel(null);

			if (!mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				((MapListFragment) fragment).setZoomLevel(MapManager.ZOOM_SCALE_COUNTY);
			}
			mFab.setEnabled(((MapListFragment) fragment).getFabEnabled());
		}

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, fragment)
				.commit();

		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
		mCurrentFragment = fragment;
		setActionBarIcon();
		updateFab();
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

	public void updateNotificationIndicator() {

		Logger.v(this, "updateNotificationIndicator for menus");

		Boolean showingNotifications = (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.END));
		Integer newNotificationCount = NotificationManager.getInstance().getNewNotificationCount();

		if (!showingNotifications && mNotificationsBadgeGroup != null) {
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

	protected void updateFab() {
		/*
		 * Called everytime a fragment is loaded.
		 */
		if (mFab.isEnabled()) {

			mFab.setTag(mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)
			            ? mPrevFragmentTag
			            : Constants.FRAGMENT_TYPE_MAP);
			mFab.setText(StringManager.getString(mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)
			                                     ? R.string.label_view_list
			                                     : R.string.label_view_map));
			mFab.fadeIn();
		}
		else {
			mFab.fadeOut();
		}
	}

	protected void scrollToTopOfList() {
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			AbsListView list = ((EntityListFragment) mCurrentFragment).getListView();
			((ListView) list).setSelectionAfterHeaderView();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		mTooltips.hide(false);
		if (item.getItemId() == android.R.id.home) {
			if (mDrawerToggle != null) {
				mDrawerToggle.onOptionsItemSelected(item);
			}
			if (mDrawerLayout != null) {
				if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
					mNotificationActionIcon.animate().rotation(0f).setDuration(200);
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		/* Manage notifications alert */
		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			MenuItem notifications = menu.findItem(R.id.notifications);
			if (notifications != null) {
				View view = notifications.getActionView();
				mNotificationsBadgeGroup = view.findViewById(R.id.badge_group);
				mNotificationsBadgeCount = (TextView) view.findViewById(R.id.badge_count);
			}
			updateNotificationIndicator();
		}

        /* Hide/show actions based on drawer state */
		if (mDrawerLayout != null) {
			//			Boolean leftDrawerOpen = mDrawerLayout.isDrawerOpen(mDrawerLeft);
			//			leftDrawerState(leftDrawerOpen, menu);

			Boolean rightDrawerOpen = mDrawerLayout.isDrawerOpen(mDrawerRight);
			if (rightDrawerOpen) {
				if (mNotificationsBadgeGroup != null) {
					mNotificationsBadgeGroup.setVisibility(View.GONE);
				}
			}
		}

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			mNotificationActionIcon = notifications.getActionView().findViewById(R.id.notifications_image);
			notifications.getActionView().findViewById(R.id.notifications_frame).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
						mNotificationActionIcon.animate().rotation(0f).setDuration(200);
						mDrawerLayout.closeDrawer(mDrawerRight);
					}
					else {
						mNotificationActionIcon.animate().rotation(90f).setDuration(200);
						mDrawerLayout.openDrawer(mDrawerRight);
					}
				}
			});
		}

		return true;
	}

	protected void leftDrawerState(Boolean open, Menu menu) {

		final MenuItem newPlace = menu.findItem(R.id.new_place);
		if (newPlace != null) {
			newPlace.setVisible(!(open));
		}

		final MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			refresh.setVisible(!(open));
		}

		final MenuItem search = menu.findItem(R.id.search);
		if (search != null) {
			search.setVisible(!(open));
		}

		final MenuItem add = menu.findItem(R.id.add);
		if (add != null) {
			add.setVisible(!(open));
		}

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			notifications.setVisible(!(open));
		}

			/* Don't need to show the user email in two places */
		if (Patchr.getInstance().getCurrentUser() != null && Patchr.getInstance().getCurrentUser().name != null) {
			String subtitle = null;
			if (!open) {
				if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
					subtitle = Patchr.getInstance().getCurrentUser().name.toUpperCase(Locale.US);
				}
				else {
					subtitle = Patchr.getInstance().getCurrentUser().email.toLowerCase(Locale.US);
				}
			}
			mActionBar.setSubtitle(subtitle);
		}
	}



	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		super.onStart();

		/* Show current user */
		if (mActionBar != null
				&& Patchr.getInstance().getCurrentUser() != null
				&& Patchr.getInstance().getCurrentUser().name != null) {
			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				mActionBar.setSubtitle(Patchr.getInstance().getCurrentUser().name.toUpperCase(Locale.US));
			}
			else {
				mActionBar.setSubtitle(Patchr.getInstance().getCurrentUser().email.toLowerCase(Locale.US));
			}
		}

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
		Patchr.getInstance().setCurrentPlace(null);
		Logger.v(this, "Setting current place to null");
		if (mPauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}

		/* Manage activity alert */
		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			updateNotificationIndicator();
		}

		/* In case the user was edited from the drawer */
		mUserView.databind(Patchr.getInstance().getCurrentUser());
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

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		/*
		 * Sync the toggle state after onRestoreInstanceState has occurred.
		 */
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.aircandi_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}