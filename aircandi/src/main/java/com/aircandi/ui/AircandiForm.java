package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.User;
import com.aircandi.queries.ActivityByAffinityQuery;
import com.aircandi.queries.ActivityByUserQuery;
import com.aircandi.queries.ShortcutsQuery;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

	protected Number mPauseDate;
	protected Boolean  mConfiguredForAnonymous;

	protected DrawerLayout          mDrawerLayout;
	protected View                  mDrawer;
	protected ActionBarDrawerToggle mDrawerToggle;
	protected Boolean mFinishOnClose = false;

	protected String mTitle = StringManager.getString(R.string.name_app);
	protected UserView   mUserView;
	protected CacheStamp mCacheStamp;

	protected View mCurrentNavView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		mUserView.setTag(Aircandi.getInstance().getCurrentUser());
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
	}

	protected void configureDrawer() {

		Boolean configChange = mConfiguredForAnonymous == null
				|| !Aircandi.getInstance().getCurrentUser().isAnonymous().equals(mConfiguredForAnonymous)
				|| (mCacheStamp != null && !mCacheStamp.equals(Aircandi.getInstance().getCurrentUser().getCacheStamp()));

		if (configChange) {
			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				findViewById(R.id.item_feed).setVisibility(View.GONE);
				findViewById(R.id.item_watch).setVisibility(View.GONE);
				findViewById(R.id.item_create).setVisibility(View.GONE);
				mConfiguredForAnonymous = true;
			}
			else {
				mConfiguredForAnonymous = false;
				findViewById(R.id.item_feed).setVisibility(View.VISIBLE);
				findViewById(R.id.item_watch).setVisibility(View.VISIBLE);
				findViewById(R.id.item_create).setVisibility(View.VISIBLE);
				mUserView.databind(Aircandi.getInstance().getCurrentUser());
				mCacheStamp = Aircandi.getInstance().getCurrentUser().getCacheStamp();
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
			mActionBar.setDisplayShowTitleEnabled(true);
			mActionBar.setDisplayShowHomeEnabled(true);
			if (mDrawerLayout != null) {
				mActionBar.setHomeButtonEnabled((mDrawerLayout.getDrawerLockMode(mDrawer) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED));
				mActionBar.setDisplayHomeAsUpEnabled((mDrawerLayout.getDrawerLockMode(mDrawer) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED));
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerVisible(mDrawer)) {
			onCancel(false);
		}
		else {
			mDrawerLayout.openDrawer(mDrawer);
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
				if (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
						, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
					extras.putBoolean(Constants.EXTRA_AUTO_WATCH, true);
				}
			}
			Aircandi.dispatch.route(this, Route.BROWSE, entity, null, extras);
		}
		if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);
		}
	}

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		Aircandi.dispatch.shortcut(this, shortcut, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onDrawerItemClick(View view) {
		mNextFragmentTag = (String) view.getTag();
		mDrawerLayout.closeDrawer(mDrawer);
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		ActivityFragment fragment = (ActivityFragment) mCurrentFragment;
		fragment.onMoreButtonClick(view);
	}

	@Override
	public void onAdd(Bundle extras) {
		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_PLACE);
		super.onAdd(extras);
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void setCurrentFragment(String fragmentType) {
	    /*
	     * Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		Fragment fragment = null;

		if (mFragments.containsKey(fragmentType)) {
			fragment = mFragments.get(fragmentType);
		}
		else {

			if (fragmentType.equals(Constants.FRAGMENT_TYPE_NEARBY)) {

				fragment = new RadarListFragment()
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.radar_fragment)
						.setListItemResId(R.layout.temp_listitem_radar)
						.setSelfBindingEnabled(true)
						.setTitleResId(R.string.label_radar_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_beacons);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh_special);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_help);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_FEED)) {

				fragment = new ActivityFragment();
				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				ActivityByAffinityQuery query = new ActivityByAffinityQuery()
						.setEntityId(Aircandi.getInstance().getCurrentUser().id)
						.setPageSize(Integers.getInteger(R.integer.page_size_activities));

				((ActivityFragment) fragment)
						.setMonitor(monitor)
						.setQuery(query)
						.setActivityStream(true)
						.setSelfBindingEnabled(true)
						.setTitleResId(R.string.label_feed_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_HISTORY)) {

				fragment = new ActivityFragment();
				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				ActivityByUserQuery query = new ActivityByUserQuery()
						.setEntityId(Aircandi.getInstance().getCurrentUser().id)
						.setPageSize(Integers.getInteger(R.integer.page_size_activities));

				((ActivityFragment) fragment)
						.setMonitor(monitor)
						.setQuery(query)
						.setActivityStream(true)
						.setSelfBindingEnabled(true)
						.setTitleResId(R.string.label_history_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_WATCH)) {

				fragment = new ShortcutFragment();
				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				ShortcutsQuery query = new ShortcutsQuery().setEntityId(Aircandi.getInstance().getCurrentUser().id);

				((ShortcutFragment) fragment)
						.setQuery(query)
						.setMonitor(monitor)
						.setShortcutType(Constants.TYPE_LINK_WATCH)
						.setEmptyMessageResId(R.string.label_watching_empty)
						.setSelfBindingEnabled(true)
						.setTitleResId(R.string.label_watch_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_CREATE)) {

				fragment = new ShortcutFragment();
				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				ShortcutsQuery query = new ShortcutsQuery().setEntityId(Aircandi.getInstance().getCurrentUser().id);

				((ShortcutFragment) fragment)
						.setQuery(query)
						.setMonitor(monitor)
						.setShortcutType(Constants.TYPE_LINK_CREATE)
						.setEmptyMessageResId(R.string.label_created_empty)
						.setSelfBindingEnabled(true)
						.setTitleResId(R.string.label_create_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
			}

			mFragments.put(fragmentType, fragment);
		}

		if (fragment != null) {
			setActivityTitle(StringManager.getString(((BaseFragment) fragment).getTitleResId()));
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_holder, fragment);
			ft.commit();
			mCurrentFragment = (BaseFragment) fragment;
			mCurrentFragmentTag = fragmentType;
		}
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
		if (tethered || (!NetworkManager.getInstance().isWifiEnabled() && !Aircandi.usingEmulator)) {

			UI.showToastNotification(StringManager.getString(tethered
			                                                 ? R.string.alert_wifi_tethered
			                                                 : R.string.alert_wifi_disabled), Toast.LENGTH_SHORT);
		}
	}

	public void updateActivityAlert() {

		Logger.v(this, "updateActivityAlert for menus");

		Boolean newMessages = MessagingManager.getInstance().getNewActivity();
		if (mMenu != null) {
			MenuItem notifications = mMenu.findItem(R.id.notifications);
			if (notifications != null) {
				ImageView image = (ImageView) notifications.getActionView().findViewById(R.id.notifications_image);
				if (newMessages) {
					final int color = Colors.getColor(R.color.holo_blue_dark);
					image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
					notifications.setVisible(true);
				}
				else {
					image.setColorFilter(null);
					notifications.setVisible(false);
				}
				image.invalidate();
			}
		}

		ImageView drawerImage = (ImageView) findViewById(R.id.image_messages_all);
		if (drawerImage != null) {
			if (newMessages) {
				final int color = Colors.getColor(R.color.holo_blue_dark);
				drawerImage.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				drawerImage.setVisibility(View.VISIBLE);
			}
			else {
				drawerImage.setColorFilter(null);
			}
		}
	}

	@SuppressWarnings("ucd")
	protected void updateDrawer() {
		if (mCurrentNavView != null) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_feed).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_watch).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_create).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceMedium((TextView) mCurrentNavView.findViewById(R.id.name));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

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
		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			updateActivityAlert();
		}

        /* Hide/show actions based on drawer state */
		if (mDrawerLayout != null) {
			Boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);

			MenuItem menuItemAdd = menu.findItem(R.id.new_place);
			if (menuItemAdd != null) {
				menuItemAdd.setVisible(!(drawerOpen));
			}

			final MenuItem refresh = menu.findItem(R.id.refresh);
			if (refresh != null) {
				refresh.setVisible(!(drawerOpen));
			}

			final MenuItem search = menu.findItem(R.id.search);
			if (search != null) {
				search.setVisible(!(drawerOpen));
			}

			final MenuItem notifications = menu.findItem(R.id.notifications);
			if (notifications != null) {
				Boolean newMessages = MessagingManager.getInstance().getNewActivity();
				notifications.setVisible(!(drawerOpen || !newMessages));
			}

			/* Don't need to show the user email in two places */
			if (Aircandi.getInstance().getCurrentUser() != null && Aircandi.getInstance().getCurrentUser().name != null) {
				String subtitle = null;
				if (!drawerOpen) {
					if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
						subtitle = Aircandi.getInstance().getCurrentUser().name.toUpperCase(Locale.US);
					}
					else {
						subtitle = Aircandi.getInstance().getCurrentUser().email.toLowerCase(Locale.US);
					}
				}
				mActionBar.setSubtitle(subtitle);
			}
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
				&& Aircandi.getInstance().getCurrentUser() != null
				&& Aircandi.getInstance().getCurrentUser().name != null) {
			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				mActionBar.setSubtitle(Aircandi.getInstance().getCurrentUser().name.toUpperCase(Locale.US));
			}
			else {
				mActionBar.setSubtitle(Aircandi.getInstance().getCurrentUser().email.toLowerCase(Locale.US));
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
		Aircandi.getInstance().setCurrentPlace(null);
		Logger.v(this, "Setting current place to null");
		if (mPauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}

		/* Manage activity alert */
		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			updateActivityAlert();
		}

		/* In case the user was edited from the drawer */
		mUserView.databind(Aircandi.getInstance().getCurrentUser());
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