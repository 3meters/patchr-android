package com.aircandi.catalina.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Catalina;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.queries.MessagesQuery;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.monitors.TrendMonitor;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.queries.TrendQuery;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.utilities.Integers;
import com.squareup.otto.Subscribe;

public class AircandiForm extends com.aircandi.ui.AircandiForm {

	protected ToolTipRelativeLayout mTooltips;
	protected View                  mFooterHolder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (mActionBar == null) {
			Aircandi.firstStartIntent = getIntent();
			Aircandi.dispatch.route(this, Route.SPLASH, null, null, null);
		}
		else {
			FontManager.getInstance().setTypefaceMedium((TextView) findViewById(R.id.item_nearby).findViewById(R.id.name));
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mFooterHolder = findViewById(R.id.footer_holder);
		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		mTooltips.setSingleShot(Constants.TOOLTIPS_PATCH_LIST_ID);
	}

	@Override
	protected void configureDrawer() {

		Boolean configChange = mConfiguredForAnonymous == null
				|| !Aircandi.getInstance().getCurrentUser().isAnonymous().equals(mConfiguredForAnonymous)
				|| (mCacheStamp != null && !mCacheStamp.equals(Aircandi.getInstance().getCurrentUser().getCacheStamp()));

		if (configChange) {
			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				mConfiguredForAnonymous = true;
				findViewById(R.id.group_messages_header).setVisibility(View.GONE);
				findViewById(R.id.item_feed).setVisibility(View.GONE);
				findViewById(R.id.item_watch).setVisibility(View.GONE);
				findViewById(R.id.item_create).setVisibility(View.GONE);
				mUserView.databind(Aircandi.getInstance().getCurrentUser());
			}
			else {
				mConfiguredForAnonymous = false;
				findViewById(R.id.group_messages_header).setVisibility(View.VISIBLE);
				findViewById(R.id.item_feed).setVisibility(View.VISIBLE);
				findViewById(R.id.item_watch).setVisibility(View.VISIBLE);
				findViewById(R.id.item_create).setVisibility(View.VISIBLE);
				mUserView.databind(Aircandi.getInstance().getCurrentUser());
				mCacheStamp = Aircandi.getInstance().getCurrentUser().getCacheStamp();
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
	public void onAdd(Bundle extras) {
		if (!extras.containsKey(Constants.EXTRA_ENTITY_SCHEMA)) {
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
		}
		extras.putString(Constants.EXTRA_MESSAGE, StringManager.getString(R.string.label_message_new_message));
		Catalina.dispatch.route(this, Route.NEW, null, null, extras);
	}

	@Override
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		super.onMessage(event);
	}

	@Override
	public void onMoreButtonClick(View view) {
		EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
		fragment.onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onAddMessageButtonClick(View view) {
		if (!mClickEnabled) return;
		mClickEnabled = false;
		onAdd(new Bundle());
	}

	@Override
	@SuppressWarnings("ucd")
	public void onDrawerItemClick(View view) {
		mNextFragmentTag = (String) view.getTag();
		mCurrentNavView = view;
		updateDrawer();
		mDrawerLayout.closeDrawer(mDrawer);
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

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_FEED)) {

				fragment = new MessageListFragment();

				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				MessagesQuery query = new MessagesQuery();

				query.setEntityId(Aircandi.getInstance().getCurrentUser().id)
				     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
				     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

				((EntityListFragment) fragment)
						.setMonitor(monitor)
						.setQuery(query)
						.setListViewType(ViewType.LIST)
						.setListLayoutResId(R.layout.entity_list_fragment)
						.setListItemResId(R.layout.temp_listitem_message)
						.setListLoadingResId(R.layout.temp_list_item_loading)
						.setListEmptyMessageResId(R.string.label_feed_empty)
						.setSelfBindingEnabled(true)
						.setActivityStream(true)
						.setTitleResId(R.string.label_feed_title);

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_notifications);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_patch);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_search);
			}

			else if (fragmentType.equals(Constants.FRAGMENT_TYPE_WATCH)) {

				fragment = new EntityListFragment();

				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				EntitiesQuery query = new EntitiesQuery();

				query.setEntityId(Aircandi.getInstance().getCurrentUser().id)
				     .setLinkDirection(Direction.out.name())
				     .setLinkType(Constants.TYPE_LINK_WATCH)
				     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
				     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_radar)
				                               .setListLoadingResId(R.layout.temp_list_item_loading)
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

				EntityMonitor monitor = new EntityMonitor(Aircandi.getInstance().getCurrentUser().id);
				EntitiesQuery query = new EntitiesQuery();

				query.setEntityId(Aircandi.getInstance().getCurrentUser().id)
				     .setLinkDirection(Direction.out.name())
				     .setLinkType(Constants.TYPE_LINK_CREATE)
				     .setPageSize(Integers.getInteger(R.integer.page_size_entities))
				     .setSchema(Constants.SCHEMA_ENTITY_PLACE);

				((EntityListFragment) fragment).setQuery(query)
				                               .setMonitor(monitor)
				                               .setListPagingEnabled(true)
				                               .setListItemResId(R.layout.temp_listitem_radar)
				                               .setListLoadingResId(R.layout.temp_list_item_loading)
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
			mActionBar.setTitle(StringManager.getString(((BaseFragment) fragment).getTitleResId()));
		}
		else {
			Integer zoomLevel = MapListFragment.ZOOM_COUNTY;
			if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_NEARBY)) {
				zoomLevel = MapListFragment.ZOOM_NEARBY;
			}
			((MapListFragment) fragment)
					.setEntities(((EntityListFragment) getCurrentFragment()).getEntities())
					.setTitleResId(((EntityListFragment) getCurrentFragment()).getTitleResId())
					.setZoomLevel(zoomLevel);
		}

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_holder, fragment);
		ft.commit();
		mPrevFragmentTag = mCurrentFragmentTag;
		mCurrentFragmentTag = fragmentType;
		mCurrentFragment = fragment;
		updateFooter();
	}

	protected void updateFooter() {
		if (mFooterHolder != null) {
			if (mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_FEED)) {
				mFooterHolder.setVisibility(View.GONE);
			}
			else {
				mFooterHolder.setTag(mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP) ? mPrevFragmentTag : Constants.FRAGMENT_TYPE_MAP);
				String label = StringManager.getString(mCurrentFragmentTag.equals(Constants.FRAGMENT_TYPE_MAP)
				                                       ? R.string.label_view_list
				                                       : R.string.label_view_map);
				((TextView) mFooterHolder).setText(label);
				mFooterHolder.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	protected void updateDrawer() {
		super.updateDrawer();
		if (mCurrentNavView != null) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_trend_activity).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.item_trend_popular).findViewById(R.id.name));
			FontManager.getInstance().setTypefaceMedium((TextView) mCurrentNavView.findViewById(R.id.name));
		}
	}

	protected Boolean showingMessages() {
		return (mCurrentFragment != null && ((Object) mCurrentFragment).getClass().equals(MessageListFragment.class));
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		if (mDrawerLayout != null) {
			Boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);

			MenuItem menuItemAdd = menu.findItem(R.id.add);
			if (menuItemAdd != null) {
				menuItemAdd.setVisible(!(drawerOpen));
			}
		}

		final MenuItem notifications = menu.findItem(com.aircandi.R.id.notifications);
		if (notifications != null) {
			notifications.getActionView().findViewById(com.aircandi.R.id.notifications_frame).setOnClickListener(new View.OnClickListener() {
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mTooltips.hide(false);
		return super.onOptionsItemSelected(item);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

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