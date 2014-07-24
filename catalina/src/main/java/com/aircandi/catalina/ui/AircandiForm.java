package com.aircandi.catalina.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.catalina.Catalina;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.queries.MessagesQuery;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
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
import com.aircandi.ui.RadarListFragment;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Integers;
import com.squareup.otto.Subscribe;

public class AircandiForm extends com.aircandi.ui.AircandiForm {

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

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

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

		String navId = (String) view.getTag();
		if (!mCurrentNavId.equals(navId)) {
			mRequestedFragmentTag = navId;
			mCurrentNavId = navId;
			mCurrentNavView = view;
			updateDrawer();
		}
		mDrawerLayout.closeDrawer(mDrawer);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void setCurrentFragment(String fragmentType, View view) {
		Logger.i(this, "setCurrentFragment called");
		/*
		 * Fragment menu items are in addition to any menu items added by the parent activity.
		 */
		BaseFragment fragment = null;

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

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_beacons);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh_special);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
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

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
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

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
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

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
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

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
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

				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_refresh);
				((BaseFragment) fragment).getMenuResIds().add(R.menu.menu_new_place);
			}

			else {
				return;
			}

			mFragments.put(fragmentType, fragment);
		}

		mDrawerTitle = StringManager.getString(fragment.getTitleResId());
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_holder, fragment);
		ft.commit();
		mCurrentFragment = (BaseFragment) fragment;
		mCurrentFragmentTag = fragmentType;
		updateActionBar();
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

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

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

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.aircandi_form;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

}