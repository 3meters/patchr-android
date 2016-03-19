package com.patchr.ui;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.appevents.AppEventsLogger;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.FontManager;
import com.patchr.components.Logger;
import com.patchr.components.MapManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.NotificationManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.events.LocationAllowedEvent;
import com.patchr.events.LocationDeniedEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.events.NotificationsQueryEvent;
import com.patchr.events.RegisterInstallEvent;
import com.patchr.events.TrendQueryEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.Notification;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.components.ListPresenter;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.ui.fragments.MapListFragment;
import com.patchr.ui.fragments.NearbyListFragment;
import com.patchr.ui.views.ImageLayout;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Integers;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

@SuppressLint("Registered")
public class AircandiForm extends BaseActivity implements View.OnClickListener, ListPresenter.OnInjectEntitiesHandler {

	protected Number  pauseDate;
	protected Boolean configuredForAuthenticated;

	protected EntityListFragment fragmentNotifications;
	protected String             nextFragmentTag;
	protected String             prevFragmentTag;

	protected Boolean finishOnClose   = false;
	protected Boolean leftDrawerOpen  = false;
	protected Boolean rightDrawerOpen = false;

	protected String title = StringManager.getString(R.string.name_app);
	protected ViewGroup   userGroup;
	protected ImageLayout userPhoto;
	protected TextView    userName;
	protected TextView    userArea;
	protected CacheStamp  cacheStamp;

	protected DrawerLayout          drawerLayout;
	protected View                  drawerLeft;
	protected View                  drawerRight;
	protected ActionBarDrawerToggle drawerToggle;

	protected View     notificationsBadgeGroup;
	protected TextView notificationsBadgeCount;
	protected View     notificationActionIcon;

	protected Map<String, Fragment> fragments = new HashMap<>();
	protected String currentFragmentTag;

	protected View currentNavView;

	@Override protected void onStart() {
		super.onStart();
		configureDrawer(); /* Make sure we are configured properly depending on user status */
		Dispatcher.getInstance().register(this);
	}

	@Override protected void onResume() {
		super.onResume();
		/*
		 * Lifecycle ordering: (onCreate/onRestart)->onStart->onResume->onAttachedToWindow->onWindowFocusChanged
		 *
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus.
		 */
		Patchr.getInstance().setCurrentPatch(null);
		if (pauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - pauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}

		/* In case the user was edited from the drawer */
		if (userGroup != null && UserManager.shared().authenticated()) {
			updateUser();
		}

		/* Ensure install is registered. */
		Boolean registered = Patchr.settings.getBoolean(StringManager.getString(R.string.setting_install_registered), false);
		if (!registered) {
			Dispatcher.getInstance().post(new RegisterInstallEvent());
		}

		AppEventsLogger.activateApp(this);
	}

	@Override protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible. Does not fire if the activity window
		 * loses focus but the activity is still active.
		 */
		pauseDate = DateTime.nowDate().getTime();
		super.onPause();
		AppEventsLogger.deactivateApp(this);
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {
		UI.showToastNotification("Click!", Toast.LENGTH_SHORT);

		if (view.getId() == R.id.action_button) {
			Patchr.router.route(this, Route.NEW_PLACE, null, null);
		}

		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				if (photo != null) {
					final String jsonPhoto = Json.objectToJson(photo);
					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
					Patchr.router.route(this, Route.PHOTO, null, extras);
				}
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				final Bundle extras = new Bundle();
				extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

				if (entity instanceof Notification) {
					Notification notification = (Notification) entity;
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Entity.getSchemaForId(notification.targetId));
					extras.putString(Constants.EXTRA_ENTITY_ID, notification.targetId);
					extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, notification.parentId);
				}

				Patchr.router.route(this, Route.BROWSE, entity, extras);
			}
		}

		if (drawerLayout != null && drawerLayout.isDrawerOpen(drawerLeft)) {
			drawerLayout.closeDrawer(drawerLeft);
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;
		getMenuInflater().inflate(R.menu.menu_log_in, menu);
		getMenuInflater().inflate(R.menu.menu_view_as_map, menu);
		getMenuInflater().inflate(R.menu.menu_search, menu);
		if (UserManager.shared().authenticated()) {
			getMenuInflater().inflate(R.menu.menu_notifications, menu);
		}

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		UI.showToastNotification("Menu click!", Toast.LENGTH_SHORT);
		Bundle extras = null;
		if (item.getItemId() == android.R.id.home) {
			if (drawerToggle != null) {
				drawerToggle.onOptionsItemSelected(item);
			}
			if (drawerLayout != null) {
				if (drawerRight != null && drawerLayout.isDrawerOpen(drawerRight)) {
					notificationActionIcon.animate().rotation(0f).setDuration(200);
					drawerLayout.closeDrawer(drawerRight);
				}
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override public void onBackPressed() {

		if (drawerLayout != null) {
			if (drawerRight != null && drawerLayout.isDrawerOpen(drawerRight)) {
				notificationActionIcon.animate().rotation(0f).setDuration(200);
				drawerLayout.closeDrawer(drawerRight);
				return;
			}
			else if (drawerLayout.isDrawerOpen(drawerLeft)) {
				drawerLayout.closeDrawer(drawerLeft);
				return;
			}
		}

		if (currentFragmentTag != null && currentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)) {
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

	@Override public void onAdd(Bundle extras) {

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to make patches and more.");
			return;
		}

		if (!extras.containsKey(Constants.EXTRA_ENTITY_SCHEMA)) {
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
		}

		extras.putString(Constants.EXTRA_MESSAGE, StringManager.getString(R.string.label_message_new_message));

		Patchr.router.route(this, Route.NEW, null, extras);
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (drawerToggle != null) {
			drawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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

	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity != null) {
			if (!(entity.schema.equals(Constants.SCHEMA_ENTITY_USER) && !UserManager.shared().authenticated())) {
				Bundle extras = new Bundle();
				Patchr.router.route(this, Route.BROWSE, entity, extras);
			}
		}
	}

	public void onDrawerItemClick(View view) {

		String tag = (String) view.getTag();
		if (!tag.equals(Constants.FRAGMENT_TYPE_SETTINGS)
				&& !tag.equals(Constants.FRAGMENT_TYPE_FEEDBACK)) {
			currentNavView = view;
			updateDrawer();
		}
		nextFragmentTag = (String) view.getTag();
		drawerLayout.closeDrawer(drawerLeft);
	}

	public void onFabButtonClick(View view) {
		Patchr.router.route(this, Route.NEW_PLACE, null, null);
	}


	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {
		if (currentFragment != null && currentFragment instanceof EntityListFragment) {
			((EntityListFragment) currentFragment).listPresenter.fetch(FetchMode.AUTO);
		}
		updateNotificationIndicator(false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		View view = findViewById(R.id.item_nearby);
		if (view != null) {
			FontManager.getInstance().setTypefaceMedium((TextView) view.findViewById(R.id.name));
		}

		this.userGroup = (ViewGroup) findViewById(R.id.user_group);

		if (userGroup != null) {
			this.userPhoto = (ImageLayout) userGroup.findViewById(R.id.user_photo);
			this.userName = (TextView) userGroup.findViewById(R.id.user_name);
			this.userArea = (TextView) userGroup.findViewById(R.id.user_area);
			if (UserManager.shared().authenticated()) {
				userGroup.setTag(UserManager.currentUser);
			}
		}

		drawerLeft = findViewById(R.id.left_drawer);
		drawerRight = findViewById(R.id.right_drawer);
		if (!UserManager.shared().authenticated()) {
			((ViewGroup) drawerRight.getParent()).removeView(drawerRight);
			drawerRight = null;
		}

		configureActionBar();

		/* Check if the device is tethered */
		tetherAlert();

		/* Default fragment */
		nextFragmentTag = Constants.FRAGMENT_TYPE_NEARBY;

		/*
		 * Notifications fragment
		 *
		 * Keyed on current user. Activity date tickled each time:
		 * - a notification is received
		 * - insert entity
		 * - delete entity
		 * - insert link
		 * - delete link
		 * - like/unlike entity
		 */
		fragmentNotifications = new EntityListFragment();
		fragmentNotifications.layoutResId = R.layout.notification_list_fragment;
		fragmentNotifications.injectEntitiesHandler = this;
		fragmentNotifications.listItemResId = R.layout.temp_listitem_notification;
		fragmentNotifications.emptyMessageResId = R.string.label_notifications_empty;
		fragmentNotifications.query = NotificationsQueryEvent.build(ActionType.ACTION_GET_NOTIFICATIONS, Integers.getInteger(R.integer.page_size_notifications), UserManager.userId);

		if (drawerRight != null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragment_holder_notifications, fragmentNotifications)
					.commit();
		}

		setCurrentFragment(nextFragmentTag);
	}

	@Override protected void configureStandardMenuItems(Menu menu) {
		super.configureStandardMenuItems(menu);

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			notificationActionIcon = MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_image);
			MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_frame).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
						drawerLayout.closeDrawer(drawerLeft);
					}
					if (drawerRight != null) {
						if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
							notificationActionIcon.animate().rotation(0f).setDuration(200);
							drawerLayout.closeDrawer(drawerRight);
						}
						else {
							notificationActionIcon.animate().rotation(90f).setDuration(200);
							drawerLayout.openDrawer(drawerRight);
						}
					}
				}
			});

			if (UserManager.shared().authenticated()) {
				View view = MenuItemCompat.getActionView(notifications);
				notificationsBadgeGroup = view.findViewById(R.id.badge_group);
				notificationsBadgeCount = (TextView) view.findViewById(R.id.badge_count);
			}
		}
	}

	private void updateUser() {
		if (UserManager.shared().authenticated()) {
			configuredForAuthenticated = true;
			User user = UserManager.currentUser;
			this.userPhoto.setImageWithEntity(user);
			this.userName.setText(user.name);
			this.userArea.setText(user.area);
			this.userGroup.setTag(user);
			findViewById(R.id.item_watch).setVisibility(View.VISIBLE);
			findViewById(R.id.item_own).setVisibility(View.VISIBLE);
			cacheStamp = UserManager.currentUser.getCacheStamp();
		}
		else {
			configuredForAuthenticated = false;
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_default_user_light);
			final BitmapDrawable bitmapDrawable = new BitmapDrawable(Patchr.applicationContext.getResources(), bitmap);
			UI.showDrawableInImageView(bitmapDrawable, this.userPhoto.imageView, Constants.ANIMATE_IMAGES);
			this.userName.setText("Guest");
			this.userArea.setText(null);
			this.userGroup.setTag(null);
			findViewById(R.id.item_watch).setVisibility(View.GONE);
			findViewById(R.id.item_own).setVisibility(View.GONE);
		}
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
				Boolean showingNotifications = (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END));
				Integer newNotificationCount = NotificationManager.getInstance().getNewNotificationCount();

				if ((ifDrawerVisible || !showingNotifications) && notificationsBadgeGroup != null) {
					if (newNotificationCount > 0) {
						notificationsBadgeCount.setText(String.valueOf(newNotificationCount));
						notificationsBadgeGroup.setVisibility(View.VISIBLE);
					}
					else {
						notificationsBadgeGroup.setVisibility(View.GONE);
					}
				}
			}
		});
	}

	protected void updateDrawer() {
		if (currentNavView != null) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_watch).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_own).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_explore).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_more_settings).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_more_feedback).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceMedium((TextView) currentNavView.findViewById(R.id.name));
		}
	}

	protected void configureDrawer() {

		Boolean configChange = configuredForAuthenticated == null
				|| !UserManager.shared().authenticated().equals(configuredForAuthenticated)
				|| (cacheStamp != null && !cacheStamp.equals(UserManager.currentUser.getCacheStamp()));

		if (configChange) {
			updateUser();
		}
	}

	protected void configureActionBar() {
		/*
		 * Only called when form is created
		 */
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		if (drawerLayout != null) {

			drawerLayout.setFocusableInTouchMode(false);
			drawerToggle = new ActionBarDrawerToggle(this
					, drawerLayout
					, toolbar
					, R.string.label_drawer_open
					, R.string.label_drawer_close) {

				@Override
				public void onDrawerClosed(View drawerView) {
					super.onDrawerClosed(drawerView);

					if (drawerView.getId() == R.id.left_drawer) {
						if (!nextFragmentTag.equals(currentFragmentTag)) {
							setCurrentFragment(nextFragmentTag);
						}
					}
					else if (drawerView.getId() == R.id.right_drawer && drawerRight != null) {
						NotificationManager.getInstance().setNewNotificationCount(0);
						updateNotificationIndicator(false);
					}
				}

				@Override
				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);

					if (drawerView.getId() == R.id.right_drawer && drawerRight != null) {
						NotificationManager.getInstance().setNewNotificationCount(0);
						NotificationManager.getInstance().cancelAllNotifications();
						updateNotificationIndicator(true);
						((EntityListFragment) fragmentNotifications).fetch(FetchMode.AUTO);
					}
				}

				@Override
				public void onDrawerSlide(View drawerView, float slideOffset) {
					super.onDrawerSlide(drawerView, slideOffset);

					if (drawerView.getId() == R.id.right_drawer && drawerRight != null) {
						notificationActionIcon.setRotation(90 * slideOffset);
					}
				}
			};

			/* Set the drawer toggle as the DrawerListener */
			drawerLayout.addDrawerListener(drawerToggle);

			if (drawerToggle != null) {
				drawerToggle.syncState();
			}

			toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (drawerRight != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
						notificationActionIcon.animate().rotation(0f).setDuration(200);
						drawerLayout.closeDrawer(drawerRight);
					}
					else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
						drawerLayout.closeDrawer(drawerLeft);
					}
					else {
						drawerLayout.openDrawer(GravityCompat.START);
					}
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@Override protected int getLayoutId() {
		return R.layout.aircandi_form;
	}

	public synchronized void setCurrentFragment(String fragmentType) {
		/*
		 * - Called from BaseActivity.onCreate (once), configureActionBar and DispatchManager.
		 * - Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		Fragment fragment;

		if (fragments.containsKey(fragmentType)) {
			fragment = fragments.get(fragmentType);
		}
		else {

			/* Nearby */

			if (fragmentType.equals(Constants.FRAGMENT_TYPE_NEARBY)) {

				fragment = new NearbyListFragment();

				EntityListFragment listFragment = (EntityListFragment) fragment;
				listFragment.listItemResId = R.layout.temp_listitem_patch;
				listFragment.emptyMessageResId = R.string.label_nearby_empty;
				listFragment.headerView = LayoutInflater.from(this).inflate(R.layout.widget_list_header_nearby, null);
			}

			/* Watching */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_WATCH)) {

				fragment = new EntityListFragment();

				EntityListFragment listFragment = (EntityListFragment) fragment;
				listFragment.listItemResId = R.layout.temp_listitem_patch;
				listFragment.emptyMessageResId = R.string.label_member_of_empty;
				listFragment.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
						, Integers.getInteger(R.integer.page_size_entities)
						, Maps.asMap("enabled", true)
						, Link.Direction.out.name()
						, Constants.TYPE_LINK_WATCH
						, Constants.SCHEMA_ENTITY_PATCH
						, UserManager.shared().authenticated() ? UserManager.currentUser.id : null);

			}

			/* Owner */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_CREATE)) {

				fragment = new EntityListFragment();

				EntityListFragment listFragment = (EntityListFragment) fragment;
				listFragment.listItemResId = R.layout.temp_listitem_patch;
				listFragment.emptyMessageResId = R.string.label_owner_of_empty;
				listFragment.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
						, Integers.getInteger(R.integer.page_size_entities)
						, Maps.asMap("enabled", true)
						, Link.Direction.out.name()
						, Constants.TYPE_LINK_CREATE
						, Constants.SCHEMA_ENTITY_PATCH
						, UserManager.shared().authenticated() ? UserManager.currentUser.id : null);
			}

			/* Trending active */

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_TREND_ACTIVE)) {

				fragment = new EntityListFragment();

				EntityListFragment listFragment = (EntityListFragment) fragment;
				listFragment.listItemResId = R.layout.temp_listitem_patch;
				listFragment.entityCacheEnabled = false;
				listFragment.headerView = LayoutInflater.from(this).inflate(R.layout.widget_list_header_trends_active, null);
				listFragment.query = TrendQueryEvent.build(ActionType.ACTION_GET_TREND
						, Constants.SCHEMA_ENTITY_MESSAGE
						, Constants.SCHEMA_ENTITY_PATCH
						, Constants.TYPE_LINK_CONTENT
						, Integers.getInteger(R.integer.page_size_entities));
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_SETTINGS)) {

				nextFragmentTag = currentFragmentTag;
				Patchr.router.route(this, Route.SETTINGS, null, null);
				return;
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_FEEDBACK)) {

				nextFragmentTag = currentFragmentTag;
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

			fragments.put(fragmentType, fragment);
		}

		if (fragment instanceof MapListFragment) {
			((MapListFragment) fragment)
					.setEntities(((EntityListFragment) getCurrentFragment()).listPresenter.entities)
					.setRelatedListFragment(currentFragmentTag)
					.setShowIndex(true)
					.setZoomLevel(null);

			if (!currentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				((MapListFragment) fragment).setZoomLevel(MapManager.ZOOM_SCALE_COUNTY);
			}

			ListPresenter controller = ((EntityListFragment) currentFragment).listPresenter;
		}

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		if (currentFragment != null) {
			ft.detach(currentFragment);
		}

		if (fragment.isDetached()) {
			ft.attach(fragment);
		}
		else {
			ft.add(R.id.fragment_holder, fragment);
		}

		ft.commit();

		prevFragmentTag = currentFragmentTag;
		currentFragmentTag = fragmentType;
		currentFragment = fragment;
	}

	public Fragment getCurrentFragment() {
		return currentFragment;
	}

	@Override public void injectEntities(ArrayAdapter adapter, ActionType actionType) {
		if (actionType == ActionType.ACTION_GET_NOTIFICATIONS) {
			/* Nearby notifications are local only so inject them */
			for (Map.Entry<String, Notification> entry : NotificationManager.getInstance().getNotifications().entrySet()) {
				if (entry.getValue().getTriggerCategory().equals(Notification.TriggerCategory.NEARBY)) {
					this.fragmentNotifications.listPresenter.adapter.add(entry.getValue());
				}
			}
		}
	}
}