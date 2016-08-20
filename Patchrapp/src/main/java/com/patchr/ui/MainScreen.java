package com.patchr.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.onesignal.OneSignal;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Dispatcher;
import com.patchr.components.NetworkManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.ReportingManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.LocationStatusEvent;
import com.patchr.events.NetworkStatusEvent;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.LocationStatus;
import com.patchr.objects.enums.NetworkStatus;
import com.patchr.objects.enums.QueryName;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.Suggest;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.collections.SearchScreen;
import com.patchr.ui.edit.PatchEdit;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.ui.fragments.MapListFragment;
import com.patchr.ui.fragments.NearbyListFragment;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.ui.widgets.ListWidget;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

import io.realm.RealmList;
import io.realm.RealmResults;

@SuppressLint("Registered")
public class MainScreen extends BaseScreen {

	protected ListWidget notificationList;
	protected String     nextFragmentTag;
	protected String     prevFragmentTag;

	protected Boolean finishOnClose   = false;
	protected Boolean leftDrawerOpen  = false;
	protected Boolean rightDrawerOpen = false;

	protected String title = StringManager.getString(R.string.name_app);
	protected ImageWidget userPhoto;
	protected TextView    userName;
	protected TextView    userArea;
	protected TextView    authIdentifierLabel;
	protected TextView    authIdentifier;

	protected DrawerLayout          drawerLayout;
	protected ActionBarDrawerToggle drawerToggle;
	protected NavigationView        drawerLeft;
	protected View                  drawerLeftHeader;
	protected View                  drawerRight;
	protected View                  pendingClickView;

	private FloatingActionButton fab;

	protected View     notificationsBadgeGroup;
	protected TextView notificationsBadgeCount;
	protected View     notificationActionIcon;

	protected Map<String, Fragment> fragments = new HashMap<>();
	protected String currentFragmentTag;

	protected View currentNavView;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
	}

	@Override protected void onStart() {
		super.onStart();
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
		if (NetworkManager.getInstance().isConnected()) {
			if (snackbar.isShownOrQueued()) {
				snackbar.dismiss();
			}
		}
		else if (!snackbar.isShownOrQueued()) {
			displayConnectionIndicator();
		}
		bind();
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_view_as_map, menu);
		getMenuInflater().inflate(R.menu.menu_view_as_list, menu);
		getMenuInflater().inflate(R.menu.menu_search, menu);
		getMenuInflater().inflate(R.menu.menu_notifications, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.view_as_map) {
			this.fab.hide();
			switchToFragment(Constants.FRAGMENT_TYPE_MAP);
		}
		else if (item.getItemId() == R.id.view_as_list) {
			this.fab.show();
			Fragment fragment = getCurrentFragment();
			if (fragment instanceof MapListFragment) {
				String listFragment = ((MapListFragment) fragment).relatedListFragment;
				if (listFragment == null) {
					listFragment = Constants.FRAGMENT_TYPE_NEARBY;
				}
				switchToFragment(listFragment);
			}
		}

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
		else if (item.getItemId() == R.id.search) {
			searchAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
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
			String listFragment = ((MapListFragment) getCurrentFragment()).relatedListFragment;
			if (listFragment == null) {
				listFragment = Constants.FRAGMENT_TYPE_NEARBY;
			}
			switchToFragment(listFragment);
			return;
		}

		super.onBackPressed();
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (drawerToggle != null) {
			drawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				if (PermissionUtil.verifyPermissions(grantResults)) {
					Dispatcher.getInstance().post(new LocationStatusEvent(LocationStatus.ALLOWED, null));
				}
				else {
					Dispatcher.getInstance().post(new LocationStatusEvent(LocationStatus.DENIED, null));
				}
			}
		}
	}

	public void onClick(View view) {

		/* Hack to wait for drawer to close before doing any work */
		if (drawerLayout != null && drawerLayout.isDrawerOpen(drawerLeft)) {
			this.pendingClickView = view;
			drawerLayout.closeDrawer(drawerLeft);
			return;
		}

		if (view.getId() == R.id.action_button) {
			addAction();
		}
		else if (view.getId() == R.id.fab) {
			addAction();
		}
		else if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				UI.browsePhoto(photo, this);
			}
			else if (view.getTag() instanceof RealmEntity) {
				final RealmEntity entity = (RealmEntity) view.getTag();
				UI.browseEntity(entity.id, this);
			}
		}

		this.pendingClickView = null;
	}

	public void addAction() {
		Intent intent = new Intent(this, PatchEdit.class);
		intent.putExtra(Constants.EXTRA_STATE, State.Inserting);
		startActivityForResult(intent, Constants.ACTIVITY_ENTITY_INSERT);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	public void searchAction() {

		Intent intent = new Intent(this, SearchScreen.class);
		intent.putExtra(Constants.EXTRA_SEARCH_SCOPE, Suggest.Patches);
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onNetworkStatusChange(final NetworkStatusEvent event) {
		if (event.status == NetworkStatus.CONNECTED) {
			if (snackbar.isShownOrQueued()) {
				snackbar.dismiss();
			}
		}
		else if (!snackbar.isShownOrQueued()) {
			displayConnectionIndicator();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		this.fab = (FloatingActionButton) findViewById(R.id.fab);
		this.drawerLeft = (NavigationView) findViewById(R.id.left_drawer);
		this.drawerRight = findViewById(R.id.right_drawer);
		this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		this.notificationList = (ListWidget) findViewById(R.id.notifications_view);

		if (this.drawerLeft != null) {

			this.drawerLeftHeader = drawerLeft.getHeaderView(0);

			if (this.drawerLeftHeader != null) {
				this.userPhoto = (ImageWidget) this.drawerLeftHeader.findViewById(R.id.user_photo);
				this.userName = (TextView) this.drawerLeftHeader.findViewById(R.id.user_name);
				this.userArea = (TextView) this.drawerLeftHeader.findViewById(R.id.user_area);
				this.authIdentifier = (TextView) this.drawerLeftHeader.findViewById(R.id.auth_identifier);
				this.authIdentifierLabel = (TextView) this.drawerLeftHeader.findViewById(R.id.auth_identifier_label);
				this.drawerLeftHeader.setTag(UserManager.currentUser);
			}

			this.drawerLeft.setNavigationItemSelectedListener(item -> {
				if (item.getItemId() == R.id.item_nearby) {
					nextFragmentTag = "nearby";
				}
				else if (item.getItemId() == R.id.item_member) {
					nextFragmentTag = "watch";
				}
				else if (item.getItemId() == R.id.item_own) {
					nextFragmentTag = "create";
				}
				else if (item.getItemId() == R.id.item_explore) {
					nextFragmentTag = "trend_active";
				}
				else if (item.getItemId() == R.id.item_settings) {
					nextFragmentTag = "settings";
				}

				if (item.getItemId() != R.id.item_settings) {
					item.setChecked(true);
				}

				drawerLayout.closeDrawers();
				return true;
			});
		}

		if (drawerLayout != null) {

			drawerLayout.setFocusableInTouchMode(false);
			drawerToggle = new ActionBarDrawerToggle(this
				, drawerLayout
				, toolbar
				, R.string.label_drawer_open
				, R.string.label_drawer_close) {

				@Override public void onDrawerClosed(View drawerView) {
					super.onDrawerClosed(drawerView);

					if (drawerView.getId() == R.id.left_drawer) {
						if (!nextFragmentTag.equals(currentFragmentTag)) {
							switchToFragment(nextFragmentTag);
						}
						else if (pendingClickView != null) {    // Hack to avoid jerky drawer close
							onClick(pendingClickView);
						}
					}
				}

				@Override public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);

					if (drawerView.getId() == R.id.right_drawer && drawerRight != null) {
						OneSignal.clearOneSignalNotifications();
						notificationList.onResume();
						ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "NotificationsList");
					}
				}

				@Override public void onDrawerSlide(View drawerView, float slideOffset) {
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

			toolbar.setNavigationOnClickListener(view -> {
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
			});
		}

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
		if (this.drawerRight != null && this.notificationList != null) {
			notificationList.setRealm(this.realm);
			notificationList.bind(QueryName.NotificationsForUser, null);
		}

		switchToFragment(nextFragmentTag);
	}

	public void displayConnectionIndicator() {

		AsyncTask.execute(() -> {
			int count = Patchr.jobManager.count();
			runOnUiThread(() -> {
				String pending = "";
				if (count > 0) {
					String label = getResources().getQuantityString(R.plurals.pending_updates, count, count);
					pending = String.format(": %1$s", label);
				}
				showSnackbar(String.format("Connection is offline%1$s", pending), Snackbar.LENGTH_INDEFINITE);
			});
		});
	}

	@Override protected void configureStandardMenuItems(Menu menu) {
		super.configureStandardMenuItems(menu);

		boolean showingMap = (currentFragment instanceof MapListFragment);
		MenuItem asListItem = menu.findItem(R.id.view_as_list);
		MenuItem asMapItem = menu.findItem(R.id.view_as_map);
		asListItem.setVisible(showingMap);
		asMapItem.setVisible(!showingMap);

		final MenuItem notifications = menu.findItem(R.id.notifications);
		if (notifications != null) {
			notificationActionIcon = MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_image);
			MenuItemCompat.getActionView(notifications).findViewById(R.id.notifications_frame).setOnClickListener(view -> {
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
			});

			View view = MenuItemCompat.getActionView(notifications);
			notificationsBadgeGroup = view.findViewById(R.id.badge_group);
			notificationsBadgeCount = (TextView) view.findViewById(R.id.badge_count);
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_main;
	}

	@Override protected String getScreenName() {
		return null;
	}

	public void bind() {

		/* In case the user was edited from the drawer */
		if (drawerLeft.getHeaderView(0) != null) {
			RealmEntity user = UserManager.currentUser;
			this.userPhoto.setImageWithPhoto(user.getPhoto(), user.name, null);
			this.userName.setText(user.name);
			UI.setTextOrGone(this.userArea, user.area);
			UI.setVisibility(this.userPhoto, View.VISIBLE);
			UI.setVisibility(this.authIdentifier, View.VISIBLE);
			UI.setVisibility(this.authIdentifierLabel, View.VISIBLE);

			UI.setTextOrGone(this.authIdentifier, user.email);

			this.drawerLeftHeader.setTag(user);
			drawerLeft.getMenu().findItem(R.id.item_member).setVisible(true);
			drawerLeft.getMenu().findItem(R.id.item_own).setVisible(true);
		}

		if (!NetworkManager.getInstance().isConnected()) {
			displayConnectionIndicator();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public synchronized void switchToFragment(String fragmentType) {
		/*
		 * - Called from BaseActivity.onCreate (once), configureActionBar and DispatchManager.
		 * - Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		Fragment fragment;

		if (fragments.containsKey(fragmentType)) {
			fragment = fragments.get(fragmentType);
		}
		else {
			switch (fragmentType) {
				case Constants.FRAGMENT_TYPE_NEARBY: {

					fragment = new NearbyListFragment();
					EntityListFragment listFragment = (EntityListFragment) fragment;
					listFragment.queryName = QueryName.PatchesNearby;
					listFragment.listTitleResId = R.string.screen_title_nearby;
					listFragment.topPadding = UI.getRawPixelsForDisplayPixels(6f);
					break;
				}
				case Constants.FRAGMENT_TYPE_MEMBER_OF: {

					fragment = new EntityListFragment();
					EntityListFragment listFragment = (EntityListFragment) fragment;
					listFragment.queryName = QueryName.PatchesUserMemberOf;
					listFragment.listTitleResId = R.string.screen_title_member_of;
					listFragment.contextEntityId = UserManager.userId;
					listFragment.topPadding = UI.getRawPixelsForDisplayPixels(6f);
					break;
				}
				case Constants.FRAGMENT_TYPE_OWNER_OF: {

					fragment = new EntityListFragment();
					EntityListFragment listFragment = (EntityListFragment) fragment;
					listFragment.queryName = QueryName.PatchesOwnedByUser;
					listFragment.listTitleResId = R.string.screen_title_owner_of;
					listFragment.contextEntityId = UserManager.userId;
					listFragment.topPadding = UI.getRawPixelsForDisplayPixels(6f);
					break;
				}
				case Constants.FRAGMENT_TYPE_TREND_ACTIVE: {

					fragment = new EntityListFragment();
					EntityListFragment listFragment = (EntityListFragment) fragment;
					listFragment.listTitleResId = R.string.screen_title_explore;
					listFragment.queryName = QueryName.PatchesToExplore;
					listFragment.headerResId = R.layout.view_list_header_trends_active;
					break;
				}
				case Constants.FRAGMENT_TYPE_SETTINGS:

					nextFragmentTag = currentFragmentTag;
					final Intent intent = new Intent(this, SettingsScreen.class);
					startActivityForResult(intent, Constants.ACTIVITY_PREFERENCES);
					AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
					return;
				case Constants.FRAGMENT_TYPE_MAP:

					fragment = new MapListFragment();
					break;
				default:
					return;
			}

			fragments.put(fragmentType, fragment);
		}

		if (fragment instanceof MapListFragment) {

			MapListFragment mapFragment = (MapListFragment) fragment;

			RealmResults<RealmEntity> realmEntities = ((EntityListFragment) getCurrentFragment()).listWidget.getEntities();
			RealmList<RealmEntity> entities = new RealmList<>();
			entities.addAll(realmEntities.subList(0, realmEntities.size()));

			mapFragment.entities = entities;
			mapFragment.relatedListFragment = currentFragmentTag;
			mapFragment.showIndex = true;
			mapFragment.zoomLevel = null;
			mapFragment.bottomPadding = UI.getRawPixelsForDisplayPixels(48f);

			if (!currentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				mapFragment.zoomLevel = Constants.ZOOM_SCALE_COUNTY;
			}
		}

		if (fragment instanceof EntityListFragment) {
			this.actionBarTitle.setText(StringManager.getString(((EntityListFragment) fragment).listTitleResId));
		}

		switch (fragmentType) {
			case Constants.FRAGMENT_TYPE_NEARBY:
				ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "NearbyPatchList");
				break;
			case Constants.FRAGMENT_TYPE_MEMBER_OF:
				ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "MemberOfPatchList");
				break;
			case Constants.FRAGMENT_TYPE_OWNER_OF:
				ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "OwnerOfPatchList");
				break;
			case Constants.FRAGMENT_TYPE_TREND_ACTIVE:
				ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "ExplorePatchList");
				break;
			case Constants.FRAGMENT_TYPE_MAP:
				ReportingManager.getInstance().screen(AnalyticsCategory.VIEW, "PatchListMap");
				break;
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
		supportInvalidateOptionsMenu();
	}

	public Fragment getCurrentFragment() {
		return currentFragment;
	}
}