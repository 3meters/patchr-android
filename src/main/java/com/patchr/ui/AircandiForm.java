package com.patchr.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.appevents.AppEventsLogger;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController.ActionType;
import com.patchr.components.Dispatcher;
import com.patchr.components.FontManager;
import com.patchr.components.Logger;
import com.patchr.components.MapManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.NotificationManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.LocationAllowedEvent;
import com.patchr.events.LocationDeniedEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.events.RegisterInstallEvent;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.EntityListFragment.ViewType;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.base.BaseFragment;
import com.patchr.ui.components.ListController;
import com.patchr.ui.widgets.UserView;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Integers;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class AircandiForm extends BaseActivity {

	protected Number  mPauseDate;
	protected Boolean mConfiguredForAuthenticated;

	protected Fragment mFragmentNotifications;
	protected String   mNextFragmentTag;
	protected String   mPrevFragmentTag;

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
		if (mUserView != null && UserManager.getInstance().authenticated()) {
			mUserView.setTag(UserManager.getInstance().getCurrentUser());
		}

		mDrawerLeft = findViewById(R.id.left_drawer);
		mDrawerRight = findViewById(R.id.right_drawer);
		if (!UserManager.getInstance().authenticated()) {
			((ViewGroup) mDrawerRight.getParent()).removeView(mDrawerRight);
			mDrawerRight = null;
		}

		/* Check if the device is tethered */
		tetherAlert();

		/* Default fragment */
		mNextFragmentTag = Constants.FRAGMENT_TYPE_NEARBY;

		/* Notifications fragment */
		mFragmentNotifications = new NotificationListFragment();

		/*
		 * Keyed on current user. Activity date tickled each time:
		 * - a notification is received
		 * - insert entity
		 * - delete entity
		 * - insert link
		 * - delete link
		 * - like/unlike entity
		 */
		((EntityListFragment) mFragmentNotifications)
				.setScopingEntityId(UserManager.getInstance().authenticated() ? UserManager.getInstance().getCurrentUser().id : null)
				.setActionType(ActionType.ACTION_GET_NOTIFICATIONS)
				.setPageSize(Integers.getInteger(R.integer.page_size_notifications))
				.setListViewType(ViewType.LIST)
				.setListLayoutResId(R.layout.notification_list_fragment)
				.setListItemResId(R.layout.temp_listitem_notification)
				.setListLoadingResId(R.layout.temp_listitem_loading_notifications)
				.setListEmptyMessageResId(R.string.label_notifications_empty)
				.setFabEnabled(false)
				.setSelfBind(false);

		if (mDrawerRight != null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragment_holder_notifications, mFragmentNotifications)
					.commit();
		}

		setCurrentFragment(mNextFragmentTag);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onViewClick(ActionEvent event) {
		super.onViewClick(event);
	}

	@Override
	public void onRefresh() {
		/*
		 * Triggers either searchForPlaces or bind(BindingMode.MANUAL).
		 */
		if (mCurrentFragment != null) {
			if (mCurrentFragment instanceof NotificationListFragment) {
				((NotificationListFragment) mCurrentFragment).onRefresh();
			}
			if (mCurrentFragment instanceof EntityListFragment) {
				((EntityListFragment) mCurrentFragment).onRefresh();
			}
		}
	}

	@Override
	public void onBackPressed() {

		if (mDrawerLayout != null) {
			if (mDrawerRight != null && mDrawerLayout.isDrawerOpen(mDrawerRight)) {
				mNotificationActionIcon.animate().rotation(0f).setDuration(200);
				mDrawerLayout.closeDrawer(mDrawerRight);
				return;
			}
			else if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
				mDrawerLayout.closeDrawer(mDrawerLeft);
				return;
			}
		}

		if (mCurrentFragmentTag != null && mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
			String listFragment = ((MapListFragment) getCurrentFragment()).getRelatedListFragment();
			if (listFragment != null) {
				Bundle extras = new Bundle();
				extras.putString(Constants.EXTRA_FRAGMENT_TYPE, listFragment);
				Patchr.router.route(this, Route.VIEW_AS_LIST, null, extras);
			}
			return;
		}

		super.onBackPressed();
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity != null) {
			if (!(entity.schema.equals(Constants.SCHEMA_ENTITY_USER) && !UserManager.getInstance().authenticated())) {
				Bundle extras = new Bundle();
				Patchr.router.route(this, Route.BROWSE, entity, extras);
			}
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

	@Override
	public void onAdd(Bundle extras) {
		if (!extras.containsKey(Constants.EXTRA_ENTITY_SCHEMA)) {
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
		}
		extras.putString(Constants.EXTRA_MESSAGE, StringManager.getString(R.string.label_message_new_message));
		Patchr.router.route(this, Route.NEW, null, extras);
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
	public void onNotificationReceived(final NotificationReceivedEvent event) {
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			((EntityListFragment) mCurrentFragment).bind(BindingMode.AUTO);
		}
		updateNotificationIndicator(false);
	}

	@SuppressWarnings("ucd")
	public void onFabButtonClick(View view) {
		Patchr.router.route(this, Route.NEW_PLACE, null, null);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				if (PermissionUtil.verifyPermissions(grantResults)) {
					Dispatcher.getInstance().post(new LocationAllowedEvent());
				}
				else {
					Dispatcher.getInstance().post(new LocationDeniedEvent());
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public synchronized void setCurrentFragment(String fragmentType) {
		/*
		 * - Called from BaseActivity.onCreate (once), configureActionBar and DispatchManager.
		 * - Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		Fragment fragment;

		if (mFragments.containsKey(fragmentType)) {
			fragment = mFragments.get(fragmentType);
		}
		else {

			/* Nearby */

			if (fragmentType.equals(Constants.FRAGMENT_TYPE_NEARBY)) {

				fragment = new NearbyListFragment();

				((EntityListFragment) fragment)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.nearby_fragment)
						.setListItemResId(R.layout.temp_listitem_nearby)
						.setListEmptyMessageResId(R.string.label_radar_empty)
						.setHeaderViewResId(R.layout.widget_list_header_nearby)
						.setTitleResId(R.string.form_title_nearby);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_sign_in);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Watching */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_WATCH)) {

				fragment = new EntityListFragment();

				((EntityListFragment) fragment)
						.setScopingEntityId(UserManager.getInstance().authenticated() ? UserManager.getInstance().getCurrentUser().id : null)
						.setActionType(ActionType.ACTION_GET_ENTITIES)
						.setLinkSchema(Constants.SCHEMA_ENTITY_PATCH)
						.setLinkType(Constants.TYPE_LINK_WATCH)
						.setLinkDirection(Link.Direction.out.name())
						.setListPagingEnabled(true)
						.setPageSize(Integers.getInteger(R.integer.page_size_entities))
						.setListItemResId(R.layout.temp_listitem_nearby)
						.setListLoadingResId(R.layout.temp_listitem_loading)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.patch_list_fragment)
						.setListEmptyMessageResId(R.string.label_watching_empty)
						.setTitleResId(R.string.form_title_watch);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_sign_in);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Owner */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_CREATE)) {

				fragment = new EntityListFragment();

				((EntityListFragment) fragment)
						.setScopingEntityId(UserManager.getInstance().authenticated() ? UserManager.getInstance().getCurrentUser().id : null)
						.setActionType(ActionType.ACTION_GET_ENTITIES)
						.setLinkSchema(Constants.SCHEMA_ENTITY_PATCH)
						.setLinkType(Constants.TYPE_LINK_CREATE)
						.setLinkDirection(Link.Direction.out.name())
						.setListPagingEnabled(true)
						.setPageSize(Integers.getInteger(R.integer.page_size_entities))
						.setListItemResId(R.layout.temp_listitem_nearby)
						.setListLoadingResId(R.layout.temp_listitem_loading)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.patch_list_fragment)
						.setListEmptyMessageResId(R.string.label_created_empty)
						.setTitleResId(R.string.form_title_create);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_sign_in);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			/* Trending active */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_TREND_ACTIVE)) {

				fragment = new TrendListFragment();

				((EntityListFragment) fragment)
						.setActionType(ActionType.ACTION_GET_TREND)
						.setLinkType(Constants.TYPE_LINK_CONTENT)
						.setListPagingEnabled(false)
						.setEntityCacheEnabled(false)
						.setHeaderViewResId(R.layout.widget_list_header_trends_active)
						.setPageSize(Integers.getInteger(R.integer.page_size_entities))
						.setListItemResId(R.layout.temp_listitem_trends)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.trends_list_fragment)
						.setListEmptyMessageResId(R.string.label_created_empty)
						.setTitleResId(R.string.form_title_trends_active);

				((TrendListFragment) fragment)
						.setToSchema(Constants.SCHEMA_ENTITY_PATCH)
						.setFromSchema(Constants.SCHEMA_ENTITY_MESSAGE)
						.setCountLabelResId(R.string.label_trends_count_active);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_sign_in);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_view_as_map);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_SETTINGS)) {

				mNextFragmentTag = mCurrentFragmentTag;
				Patchr.router.route(this, Route.SETTINGS, null, null);
				return;
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_FEEDBACK)) {

				mNextFragmentTag = mCurrentFragmentTag;
				Patchr.router.route(this, Route.FEEDBACK, null, null);
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

		if (fragment instanceof MapListFragment) {
			((MapListFragment) fragment)
					.setEntities(((EntityListFragment) getCurrentFragment()).getEntities())
					.setTitleResId(((EntityListFragment) getCurrentFragment()).getTitleResId())
					.setRelatedListFragment(mCurrentFragmentTag)
					.setShowIndex(true)
					.setZoomLevel(null);

			if (!mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				((MapListFragment) fragment).setZoomLevel(MapManager.ZOOM_SCALE_COUNTY);
			}

			ListController controller = ((EntityListFragment) mCurrentFragment).getListController();
			if (controller != null) {
				controller.getFloatingActionController().fadeOut();
			}
		}

		if (fragment instanceof EntityListFragment) {
			setActivityTitle(StringManager.getString(((EntityListFragment) fragment).getTitleResId()));
			ListController controller = ((EntityListFragment) fragment).getListController();
			if (controller != null) {
				controller.getFloatingActionController().fadeIn();
			}
		}

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		if (mCurrentFragment != null) {
			ft.detach(mCurrentFragment);
		}

		if (fragment.isDetached()) {
			ft.attach(fragment);
		}
		else {
			ft.add(R.id.fragment_holder, fragment);
		}

		ft.commit();

		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
		mCurrentFragment = fragment;
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
		if (tethered || (!NetworkManager.getInstance().isWifiEnabled())) {
			UI.showToastNotification(StringManager.getString(tethered
			                                                 ? R.string.alert_wifi_tethered
			                                                 : R.string.alert_wifi_disabled), Toast.LENGTH_SHORT);
		}
	}

	public void updateNotificationIndicator(final Boolean ifDrawerVisible) {

		Logger.v(this, "updateNotificationIndicator for menus");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Boolean showingNotifications = (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END));
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
		});
	}

	protected void updateDrawer() {
		if (mCurrentNavView != null) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_watch).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_own).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_explore).findViewById(R.id.name));
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

		Boolean configChange = mConfiguredForAuthenticated == null
				|| !UserManager.getInstance().authenticated().equals(mConfiguredForAuthenticated)
				|| (mCacheStamp != null && !mCacheStamp.equals(UserManager.getInstance().getCurrentUser().getCacheStamp()));

		if (configChange) {
			if (UserManager.getInstance().authenticated()) {
				mConfiguredForAuthenticated = true;
				findViewById(R.id.item_watch).setVisibility(View.VISIBLE);
				findViewById(R.id.item_own).setVisibility(View.VISIBLE);
				mUserView.databind(UserManager.getInstance().getCurrentUser());
				mCacheStamp = UserManager.getInstance().getCurrentUser().getCacheStamp();
			}
			else {
				mConfiguredForAuthenticated = false;
				findViewById(R.id.item_watch).setVisibility(View.GONE);
				findViewById(R.id.item_own).setVisibility(View.GONE);
				mUserView.databind(null);
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
				else if (drawerView.getId() == R.id.right_drawer && mDrawerRight != null) {
					NotificationManager.getInstance().setNewNotificationCount(0);
					updateNotificationIndicator(false);
				}
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);

				if (drawerView.getId() == R.id.right_drawer && mDrawerRight != null) {
					NotificationManager.getInstance().setNewNotificationCount(0);
					NotificationManager.getInstance().cancelAllNotifications();
					updateNotificationIndicator(true);
					((EntityListFragment) mFragmentNotifications).bind(BindingMode.AUTO);
				}
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);

				if (drawerView.getId() == R.id.right_drawer && mDrawerRight != null) {
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
				if (mDrawerRight != null && mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
					mNotificationActionIcon.animate().rotation(0f).setDuration(200);
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
				else if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
					mDrawerLayout.closeDrawer(mDrawerLeft);
				}
				else {
					mDrawerLayout.openDrawer(GravityCompat.START);
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
		configureDrawer(); /* Make sure we are configured properly depending on user status */
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
		if (mPauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}

		/* In case the user was edited from the drawer */
		if (mUserView != null && UserManager.getInstance().authenticated()) {
			mUserView.databind(UserManager.getInstance().getCurrentUser());
		}

		/* Ensure install is registered. */
		Boolean registered = Patchr.settings.getBoolean(StringManager.getString(R.string.setting_install_registered), false);
		if (!registered) {
			Dispatcher.getInstance().post(new RegisterInstallEvent());
		}

		AppEventsLogger.activateApp(this);
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
		AppEventsLogger.deactivateApp(this);
	}

	@Override
	protected void onDestroy() {
		/*
		 * The activity is getting destroyed but the application level state
		 * like singletons, statics, etc will continue as long as the application
		 * is running.
		 */
		super.onDestroy();
	}
}