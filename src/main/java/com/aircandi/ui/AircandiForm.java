package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MapManager;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.monitors.TrendMonitor;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.queries.AlertsQuery;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.queries.MessagesQuery;
import com.aircandi.queries.TrendQuery;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Colors;
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
	protected View                  mDrawer;
	protected ActionBarDrawerToggle mDrawerToggle;
	protected Boolean mFinishOnClose = false;

	protected String mTitle = StringManager.getString(R.string.name_app);
	protected UserView   mUserView;
	protected CacheStamp mCacheStamp;

	protected View                  mCurrentNavView;
	protected ToolTipRelativeLayout mTooltips;
	protected View                  mFooterHolder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mActionBar == null) {
			Patch.firstStartIntent = getIntent();
			Patch.dispatch.route(this, Route.SPLASH, null, null, null);
		}
		else {
			FontManager.getInstance().setTypefaceMedium((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		/* Ui init */
		Integer drawerIconResId = R.drawable.ic_drawer_dark;
		if (mPrefTheme.equals("aircandi_theme_snow")) {
			drawerIconResId = R.drawable.ic_drawer_light;
		}

		mUserView = (UserView) findViewById(R.id.user_current);
		mUserView.setTag(Patch.getInstance().getCurrentUser());
		mDrawer = findViewById(R.id.drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setFocusableInTouchMode(false);

		mDrawerToggle = new ActionBarDrawerToggle(this
				, mDrawerLayout
				, drawerIconResId
				, R.string.label_drawer_open
				, R.string.label_drawer_close) {

			/** Called when a drawer has settled in a completely closed state. */
			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				if (!mNextFragmentTag.equals(mCurrentFragmentTag)) {
					setCurrentFragment(mNextFragmentTag);
				}
				else {
					if (!mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
						setActivityTitle(StringManager.getString(((BaseFragment) mCurrentFragment).getTitleResId()));
					}
				}
				onPrepareOptionsMenu(mMenu); //Hide/show action bar items
			}

			/** Called when a drawer has settled in a completely open state. */
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				setActivityTitle(mTitle);
				onPrepareOptionsMenu(mMenu); //Hide/show action bar items
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		/* Check if the device is tethered */
		tetherAlert();

		/* Default fragment */
		mNextFragmentTag = Constants.FRAGMENT_TYPE_NEARBY;

		mFooterHolder = findViewById(R.id.footer_holder);
		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		mTooltips.setSingleShot(Constants.TOOLTIPS_PATCH_LIST_ID);
	}

	protected void configureDrawer() {

		Boolean configChange = mConfiguredForAnonymous == null
				|| !Patch.getInstance().getCurrentUser().isAnonymous().equals(mConfiguredForAnonymous)
				|| (mCacheStamp != null && !mCacheStamp.equals(Patch.getInstance().getCurrentUser().getCacheStamp()));

		if (configChange) {
			if (Patch.getInstance().getCurrentUser().isAnonymous()) {
				mConfiguredForAnonymous = true;
				findViewById(R.id.group_messages_header).setVisibility(View.GONE);
				findViewById(R.id.item_feed_messages).setVisibility(View.GONE);
				findViewById(R.id.item_feed_alerts).setVisibility(View.GONE);
				findViewById(R.id.item_watch).setVisibility(View.GONE);
				findViewById(R.id.item_create).setVisibility(View.GONE);
				mUserView.databind(Patch.getInstance().getCurrentUser());
			}
			else {
				mConfiguredForAnonymous = false;
				findViewById(R.id.group_messages_header).setVisibility(View.VISIBLE);
				findViewById(R.id.item_feed_messages).setVisibility(View.VISIBLE);
				findViewById(R.id.item_feed_alerts).setVisibility(View.VISIBLE);
				findViewById(R.id.item_watch).setVisibility(View.VISIBLE);
				findViewById(R.id.item_create).setVisibility(View.VISIBLE);
				mUserView.databind(Patch.getInstance().getCurrentUser());
				mCacheStamp = Patch.getInstance().getCurrentUser().getCacheStamp();
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
				mActionBar.setHomeButtonEnabled((mDrawerLayout.getDrawerLockMode(mDrawer) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED));
				mActionBar.setDisplayHomeAsUpEnabled((mDrawerLayout.getDrawerLockMode(mDrawer) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED));
			}
		}
	}

	protected void actionBarIcon() {
		super.actionBarIcon();
		if (mActionBar != null && mCurrentFragmentTag != null) {
			Drawable icon = null;
			if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_ALERTS)) {
				icon = getResources().getDrawable(R.drawable.img_alert_dark);
			}
			else if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MESSAGES)) {
				icon = getResources().getDrawable(R.drawable.img_message_dark);
			}
			if (icon != null) {
				mActionBar.setIcon(icon);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onBackPressed() {
		if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
			mFooterHolder.callOnClick();
		}
		else {
			if (mDrawerLayout.isDrawerVisible(mDrawer)) {
				onCancel(false);
			}
			else {
				mDrawerLayout.openDrawer(mDrawer);
			}
		}
	}

	@Override
	public void onRefresh() {
		if (mCurrentFragment != null) {
			((BaseFragment) mCurrentFragment).onRefresh();
		}
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (!(entity.schema.equals(Constants.SCHEMA_ENTITY_USER) && ((User) entity).isAnonymous())) {
			Bundle extras = new Bundle();
			if (Type.isTrue(entity.autowatchable)) {
				if (Patch.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
						, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
					extras.putBoolean(Constants.EXTRA_AUTO_WATCH, true);
				}
			}
			Patch.dispatch.route(this, Route.BROWSE, entity, null, extras);
		}
		if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);
		}
	}

	@SuppressWarnings("ucd")
	public void onDrawerItemClick(View view) {
		mNextFragmentTag = (String) view.getTag();
		mCurrentNavView = view;
		updateDrawer();
		mDrawerLayout.closeDrawer(mDrawer);
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
		fragment.onMoreButtonClick(view);
	}

	@Override
	public void onAdd(Bundle extras) {
		if (!extras.containsKey(Constants.EXTRA_ENTITY_SCHEMA)) {
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);
		}
		extras.putString(Constants.EXTRA_MESSAGE, StringManager.getString(R.string.label_message_new_message));
		Patch.dispatch.route(this, Route.NEW, null, null, extras);
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
	public void onMessage(final MessageEvent event) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mCurrentFragment != null && mCurrentFragment instanceof BaseFragment) {
					((BaseFragment) mCurrentFragment).bind(BindingMode.AUTO);
				}
				updateActivityAlert();
			}
		});
	}

	@SuppressWarnings("ucd")
	public void onAddMessageButtonClick(View view) {
		if (!mClickEnabled) return;
		mClickEnabled = false;
		onAdd(new Bundle());
	}

	@SuppressWarnings("ucd")
	public void onListViewButtonClick(View view) {
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
		Fragment fragment;

		if (mFragments.containsKey(fragmentType)) {
			fragment = mFragments.get(fragmentType);
		}
		else {

			if (fragmentType.equals(Constants.FRAGMENT_TYPE_NEARBY)) {

				fragment = new RadarListFragment()
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.radar_fragment)
						.setListItemResId(R.layout.temp_listitem_radar)
						.setListEmptyMessageResId(R.string.label_radar_empty)
						.setTitleResId(R.string.label_radar_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh_special);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(com.aircandi.Constants.FRAGMENT_TYPE_MESSAGES)) {

				fragment = new MessageListFragment();

				EntityMonitor monitor = new EntityMonitor(Patch.getInstance().getCurrentUser().id);
				MessagesQuery query = new MessagesQuery();

				query.setEntityId(Patch.getInstance().getCurrentUser().id)
				     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
				     .setSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

				((EntityListFragment) fragment)
						.setMonitor(monitor)
						.setQuery(query)
						.setFooterEnabled(false)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.entity_list_fragment)
						.setListItemResId(R.layout.temp_listitem_message)
						.setListLoadingResId(R.layout.temp_listitem_loading)
						.setListEmptyMessageResId(R.string.label_feed_messages_empty)
						.setSelfBindingEnabled(true)
						.setActivityStream(true)
						.setTitleResId(R.string.label_feed_messages_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(com.aircandi.Constants.FRAGMENT_TYPE_ALERTS)) {

				fragment = new AlertListFragment();

				EntityMonitor monitor = new EntityMonitor(Patch.getInstance().getCurrentUser().id);
				AlertsQuery query = new AlertsQuery();

				query.setEntityId(Patch.getInstance().getCurrentUser().id)
				     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
				     .setSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

				((EntityListFragment) fragment)
						.setMonitor(monitor)
						.setQuery(query)
						.setFooterEnabled(false)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.entity_list_fragment)
						.setListItemResId(R.layout.temp_listitem_alert)
						.setListLoadingResId(R.layout.temp_listitem_loading)
						.setListEmptyMessageResId(R.string.label_feed_alerts_empty)
						.setSelfBindingEnabled(true)
						.setActivityStream(true)
						.setTitleResId(R.string.label_feed_alerts_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_WATCH)) {

				fragment = new EntityListFragment();

				EntityMonitor monitor = new EntityMonitor(Patch.getInstance().getCurrentUser().id);
				EntitiesQuery query = new EntitiesQuery();

				query.setEntityId(Patch.getInstance().getCurrentUser().id)
				     .setLinkDirection(Link.Direction.out.name())
				     .setLinkType(Constants.TYPE_LINK_WATCH)
				     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
				     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_radar)
				                               .setListLoadingResId(R.layout.temp_listitem_loading)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.place_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_watching_empty)
				                               .setTitleResId(R.string.label_watch_title)
				                               .setSelfBindingEnabled(true);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_CREATE)) {

				fragment = new EntityListFragment();

				EntityMonitor monitor = new EntityMonitor(Patch.getInstance().getCurrentUser().id);
				EntitiesQuery query = new EntitiesQuery();

				query.setEntityId(Patch.getInstance().getCurrentUser().id)
				     .setLinkDirection(Link.Direction.out.name())
				     .setLinkType(Constants.TYPE_LINK_CREATE)
				     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
				     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_radar)
				                               .setListLoadingResId(R.layout.temp_listitem_loading)
				                               .setListViewType(ViewType.LIST)
				                               .setListLayoutResId(R.layout.place_list_fragment)
				                               .setListEmptyMessageResId(R.string.label_created_empty)
				                               .setTitleResId(R.string.label_create_title)
				                               .setSelfBindingEnabled(true);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

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
				                               .setSelfBindingEnabled(true);

				((TrendListFragment) fragment).setCountLabelResId(R.string.label_trends_count_popular);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

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
				                               .setSelfBindingEnabled(true);

				((TrendListFragment) fragment).setCountLabelResId(R.string.label_trends_count_active);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_MAP)) {

				fragment = new MapListFragment();

				((MapListFragment) fragment).getMenuResIds().add(com.aircandi.R.menu.menu_refresh);
				((MapListFragment) fragment).getMenuResIds().add(com.aircandi.R.menu.menu_new_place);
			}

			else {
				return;
			}

			mFragments.put(fragmentType, fragment);
		}

		if (!fragmentType.equals(Constants.FRAGMENT_TYPE_MAP)) {
			//noinspection ConstantConditions
			setActivityTitle(StringManager.getString(((BaseFragment) fragment).getTitleResId()));
		}
		else {
			//noinspection ConstantConditions
			((MapListFragment) fragment)
					.setEntities(((EntityListFragment) getCurrentFragment()).getEntities())
					.setTitleResId(((EntityListFragment) getCurrentFragment()).getTitleResId())
					.setZoomLevel(null);

			if (!mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				//noinspection ConstantConditions
				((MapListFragment) fragment).setZoomLevel(MapManager.ZOOM_SCALE_COUNTY);
			}
		}

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, fragment)
				.commit();
		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
		mCurrentFragment = fragment;
		actionBarIcon();
		updateFooter();
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
		if (tethered || (!NetworkManager.getInstance().isWifiEnabled() && !Patch.usingEmulator)) {

			UI.showToastNotification(StringManager.getString(tethered
			                                                 ? R.string.alert_wifi_tethered
			                                                 : R.string.alert_wifi_disabled), Toast.LENGTH_SHORT);
		}
	}

	public void updateActivityAlert() {

		Logger.v(this, "updateActivityAlert for menus");

		Boolean showingMessages = (mCurrentFragment != null && mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MESSAGES));
		Boolean showingAlerts = (mCurrentFragment != null && mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_ALERTS));

		Boolean newActivity = MessagingManager.getInstance().getNewActivity();
		Boolean newAlert = MessagingManager.getInstance().getNewAlert();
		Boolean newMessage = MessagingManager.getInstance().getNewMessage();

		if (mMenu != null) {
			MenuItem notifications = mMenu.findItem(R.id.notifications);
			if (notifications != null) {
				notifications.setVisible((newActivity && !(showingMessages || showingAlerts)));
			}
		}

		Integer color = Colors.getColor(R.color.brand_primary);
		if (Patch.themeTone != null) {
			color = Patch.themeTone.equals(Patch.ThemeTone.LIGHT) ? R.color.text_secondary_light : R.color.text_secondary_dark;
		}
		Integer colorHighlighted = Colors.getColor(R.color.brand_primary);

		TextView alertCount = (TextView) findViewById(R.id.count_alerts);
		if (alertCount != null) {
			Integer count = MessagingManager.getInstance().getAlerts().size();
			alertCount.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
			if (count > 0) {
				alertCount.setText(String.valueOf(count));
				alertCount.setTextColor((newAlert && !showingAlerts) ? colorHighlighted : color);
			}
		}

		TextView messageCount = (TextView) findViewById(R.id.count_messages);
		if (messageCount != null) {
			Integer count = MessagingManager.getInstance().getMessages().size();
			messageCount.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
			if (count > 0) {
				messageCount.setText(String.valueOf(count));
				messageCount.setTextColor((newMessage && !showingMessages) ? colorHighlighted : color);
			}
		}
	}

	@SuppressWarnings("ucd")
	protected void updateDrawer() {
		if (mCurrentNavView != null) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_feed_messages).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_watch).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_create).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_feed_alerts).findViewById(com.aircandi.R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_trend_activity).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_trend_popular).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceMedium((TextView) mCurrentNavView.findViewById(R.id.name));
		}
	}

	protected void updateFooter() {
		if (mFooterHolder != null) {
			if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_ALERTS)
					|| mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MESSAGES)) {
				mFooterHolder.setVisibility(View.GONE);
			}
			else {
				mFooterHolder.setTag(mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)
				                     ? mPrevFragmentTag
				                     : Constants.FRAGMENT_TYPE_MAP);
				String label = StringManager.getString(mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)
				                                       ? R.string.label_view_list
				                                       : R.string.label_view_map);
				((TextView) mFooterHolder).setText(label);

				if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
					mFooterHolder.setVisibility(View.VISIBLE);
					mFooterHolder.setClickable(true);
				}
				else {
					Boolean hasEntities = (((EntityListFragment) getCurrentFragment()).getEntities().size() > 0);
					mFooterHolder.setVisibility(hasEntities ? View.VISIBLE : View.INVISIBLE);
					mFooterHolder.setClickable(hasEntities);
				}
			}
		}
	}

	protected Boolean showingMessages() {
		if (mCurrentFragment != null) {
			if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_ALERTS)
					|| mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MESSAGES)) {
				return true;
			}
		}
		return false;
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
			if (mDrawerLayout != null) {
				if (mDrawerLayout.isDrawerOpen(mDrawer)) {
					mDrawerLayout.closeDrawer(mDrawer);
				}
				else {
					mDrawerLayout.openDrawer(mDrawer);
				}
			}
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		/* Manage activity alert */
		if (!Patch.getInstance().getCurrentUser().isAnonymous()) {
			updateActivityAlert();
		}

        /* Hide/show actions based on drawer state */
		if (mDrawerLayout != null) {
			Boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);

			MenuItem menuItemNewPlace = menu.findItem(R.id.new_place);
			if (menuItemNewPlace != null) {
				menuItemNewPlace.setVisible(!(drawerOpen));
			}

			final MenuItem refresh = menu.findItem(R.id.refresh);
			if (refresh != null) {
				refresh.setVisible(!(drawerOpen));
			}

			final MenuItem search = menu.findItem(R.id.search);
			if (search != null) {
				search.setVisible(!(drawerOpen));
			}

			MenuItem menuItemAdd = menu.findItem(R.id.add);
			if (menuItemAdd != null) {
				menuItemAdd.setVisible(!(drawerOpen));
			}

			final MenuItem notifications = menu.findItem(R.id.notifications);
			if (notifications != null) {
				if (drawerOpen) {
					notifications.setVisible(false);
				}
			}

			/* Don't need to show the user email in two places */
			if (Patch.getInstance().getCurrentUser() != null && Patch.getInstance().getCurrentUser().name != null) {
				String subtitle = null;
				if (!drawerOpen) {
					if (Patch.getInstance().getCurrentUser().isAnonymous()) {
						subtitle = Patch.getInstance().getCurrentUser().name.toUpperCase(Locale.US);
					}
					else {
						subtitle = Patch.getInstance().getCurrentUser().email.toLowerCase(Locale.US);
					}
				}
				mActionBar.setSubtitle(subtitle);
			}
		}

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			notifications.getActionView().findViewById(R.id.notifications_frame).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					if (showingMessages()) {
						scrollToTopOfList();  // Scrolling will trigger an activity update
						MessagingManager.getInstance().setNewActivity(false);
						updateActivityAlert();
						MessagingManager.getInstance().cancelNotifications();
					}
					else {
						mDrawerLayout.openDrawer(mDrawer);
					}
				}
			});
		}

		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		super.onStart();

		/* Show current user */
		if (mActionBar != null
				&& Patch.getInstance().getCurrentUser() != null
				&& Patch.getInstance().getCurrentUser().name != null) {
			if (Patch.getInstance().getCurrentUser().isAnonymous()) {
				mActionBar.setSubtitle(Patch.getInstance().getCurrentUser().name.toUpperCase(Locale.US));
			}
			else {
				mActionBar.setSubtitle(Patch.getInstance().getCurrentUser().email.toLowerCase(Locale.US));
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
		Patch.getInstance().setCurrentPlace(null);
		Logger.v(this, "Setting current place to null");
		if (mPauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}

		/* Manage activity alert */
		if (!Patch.getInstance().getCurrentUser().isAnonymous()) {
			updateActivityAlert();
		}

		/* In case the user was edited from the drawer */
		mUserView.databind(Patch.getInstance().getCurrentUser());
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