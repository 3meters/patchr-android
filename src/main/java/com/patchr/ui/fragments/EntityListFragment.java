package com.patchr.ui.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.MenuManager;
import com.patchr.components.UserManager;
import com.patchr.events.AbsEntitiesQueryEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Route;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.ListPresenter;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Fragment lifecycle
 * <p/>
 * - onAttach (activity may not be fully initialized but fragment has been associated with it)
 * - onCreate
 * - onCreateView
 * - onViewCreated
 * - onActivityCreated (views created, safe to use findById)
 * - onViewStateRestored
 * - onStart (fragment becomes visible)
 * - onResume
 * <p/>
 * - onPause
 * - onStop
 * - onSaveInstanceState
 * - onDestroyView
 * - onDestroy
 * - onDetach
 */
public class EntityListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

	public ListPresenter                         listPresenter;
	public AbsEntitiesQueryEvent                 query;
	public ListPresenter.OnInjectEntitiesHandler injectEntitiesHandler;
	public View                                  headerView;

	public Integer layoutResId;
	public Integer listItemResId;
	public Integer titleResId;
	public Integer emptyMessageResId;

	public boolean entityCacheDisabled;            // true == always call service

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.layoutResId = R.layout.entity_list_fragment;

		/* Force complete initialization if being recreated by the system */
		boolean recreated = (savedInstanceState != null && !savedInstanceState.isEmpty());
		if (recreated) {
			Intent intent = getActivity().getIntent();
			getActivity().finish();
			startActivity(intent);
		}

		this.listPresenter = new ListPresenter(getContext());
		this.listPresenter.listItemResId = this.listItemResId;
		this.listPresenter.emptyMessageResId = this.emptyMessageResId;
		this.listPresenter.query = this.query;
		this.listPresenter.headerView = this.headerView;
		this.listPresenter.entityCacheDisabled = this.entityCacheDisabled;
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(layoutResId, container, false);
	}

	@Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onActivityCreated(savedInstanceState);
		initialize(getView());
	}

	@Override public void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
		if (getActivity() != null && !getActivity().isFinishing()) {
			configureStandardMenuItems(((BaseScreen) getActivity()).optionMenu);
		}
		fetch(FetchMode.AUTO);
	}

	@Override public void onResume() {
		super.onResume();
		if (listPresenter != null) {
			listPresenter.onResume();
		}
	}

	@Override public void onPause() {
		super.onPause();
		if (listPresenter != null) {
			listPresenter.onPause();
		}
	}

	@Override public void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Patchr.router.route(getActivity(), Patchr.router.routeForMenuId(item.getItemId()), null, null);
		return true;
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		listPresenter.getAdapter().notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onNotificationReceived(final NotificationReceivedEvent event) {
		/*
		 * If list is showing notifications and contains notification entity then replace it.
		 */
		if (listPresenter.entities.contains(event.notification)) {
			listPresenter.entities.remove(event.notification);
		}
		listPresenter.emptyPresenter.hide(true);
		listPresenter.adapter.insert(event.notification, 0);
		listPresenter.adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(View view) {

		assert view != null;

		this.listPresenter.listView = (AbsListView) ((ViewGroup) view.findViewById(R.id.swipe)).getChildAt(1);
		this.listPresenter.emptyPresenter = new EmptyPresenter(view.findViewById(R.id.list_message));
		this.listPresenter.busyPresenter = new BusyPresenter();
		this.listPresenter.busyPresenter.setProgressBar(view.findViewById(R.id.list_progress));

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getContext(), R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyPresenter.setSwipeRefresh(swipeRefresh);
		}

		this.listPresenter.initialize(getContext(), view);        // We init after everything is setup
	}

	public void fetch(FetchMode fetchMode) {
		if (listPresenter != null) {
			listPresenter.fetch(fetchMode);
		}
	}

	public void configureStandardMenuItems(final Menu menu) {

		FragmentActivity fragmentActivity = getActivity();
		if (menu == null || fragmentActivity == null) return;

		fragmentActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				/* Sign-in isn't dependent on an entity for policy */

				MenuItem item = menu.findItem(R.id.login);
				if (item != null) {
					item.setVisible(!UserManager.shared().authenticated());
				}

				/* Remove menu items per policy */
				Entity entity = ((BaseScreen) getActivity()).entity;

				if (entity == null) return;

				item = menu.findItem(R.id.edit);
				if (item != null) {
					item.setVisible(MenuManager.canUserEdit(entity));
				}

				item = menu.findItem(R.id.delete);
				if (item != null) {
					item.setVisible(MenuManager.canUserDelete(entity));
				}

				item = menu.findItem(R.id.remove);
				if (item != null) {
					item.setVisible(MenuManager.showAction(Route.REMOVE, entity));
				}

				item = menu.findItem(R.id.share);
				if (item != null) {
					item.setVisible(MenuManager.canUserShare(entity));
				}

				item = menu.findItem(R.id.share_photo);
				if (item != null) {
					item.setVisible(MenuManager.canUserShare(entity));
				}

				item = menu.findItem(R.id.logout);
				if (item != null) {
					item.setVisible(MenuManager.showAction(Route.EDIT, entity));
				}

				item = menu.findItem(R.id.navigate);
				if (item != null && UserManager.shared().authenticated()) {
					item.setVisible(entity.getLocation() != null);
				}

				item = menu.findItem(R.id.invite);
				if (item != null) {
					item.setVisible(MenuManager.canUserShare(entity));
				}
			}
		});
	}
}