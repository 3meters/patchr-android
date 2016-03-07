package com.patchr.ui.base;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.ScrollView;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.interfaces.IForm;
import com.patchr.objects.Entity;
import com.patchr.objects.Route;
import com.patchr.ui.AircandiForm;
import com.patchr.utilities.DateTime;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFragment extends Fragment implements IForm {
	/*
	 * Fragment lifecycle
	 *
	 * - onAttach (activity may not be fully initialized but fragment has been associated with it)
	 * - onCreate
	 * - onCreateView
	 * - onViewCreated
	 * - onActivityCreated (views created, safe to use findById)
	 * - onViewStateRestored
	 * - onStart (fragment becomes visible)
	 * - onResume
	 *
	 * - onPause
	 * - onStop
	 * - onSaveInstanceState
	 * - onDestroyView
	 * - onDestroy
	 * - onDetach
	 */
	protected List<Integer> mMenuResIds = new ArrayList<>();
	protected Boolean       mIsVisible  = false;
	protected Boolean       mProcessing = false;
	protected Resources mResources;
	protected String    mGroupTag;

	@Override public void unpackIntent() {}

	@Override public void onCreate(Bundle savedInstanceState) {
		/* Called after onAttach */
		super.onCreate(savedInstanceState);
		initialize(savedInstanceState);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/* Called between onCreate and onActivityCreate */
		if (getActivity() == null || getActivity().isFinishing()) return null;
		return inflater.inflate(getLayoutId(), container, false);
	}

	@Override public void onViewCreated(final View view, Bundle savedInstanceState) {
		/* View hierarchy created but not attached to parent yet */
		if (view != null && view.getViewTreeObserver().isAlive()) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				public void onGlobalLayout() {
					//noinspection deprecation
					view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					onViewLayout();
				}
			});
		}
	}

	@Override public void onStart() {
		/* Called everytime the fragment is started or restarted. */
		super.onStart();
		Dispatcher.getInstance().register(this);
		if (getActivity() != null && !getActivity().isFinishing()) {
			configureStandardMenuItems(((BaseActivity) getActivity()).getOptionMenu());
		}
	}

	@Override public void onResume() {
		super.onResume();
		if (getActivity() != null && getActivity() instanceof AircandiForm) {
			((AircandiForm) getActivity()).updateNotificationIndicator(false);
		}
	}

	@Override public void onStop() {
		/*
		 * Triggers
		 * - Switching to another fragment.
		 * - Switching to launcher.
		 * - Killing activity.
		 * - Navigating to another activity.
		 */
		super.onStop();
		Dispatcher.getInstance().unregister(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		/*
		 * This is triggered by onCreate in some android versions
		 * so any dependencies must have already been created.
		 */
		Logger.d(this, "Creating fragment options menu");
		for (Integer menuResId : mMenuResIds) {
			inflater.inflate(menuResId, menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
		configureStandardMenuItems(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Patchr.router.route(getActivity(), Patchr.router.routeForMenuId(item.getItemId()), null, null);
		return true;
	}

	@Override public void onAdd(Bundle extras) {}

	@Override public void onHelp() {}

	@Override public void onError() {}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	public void onViewLayout() {
		/*
		 * Called when initial view layout has completed and
		 * views have been measured and sized.
		 */
		Logger.d(this, "Fragment view layout completed");
	}

	public abstract void onScollToTop();

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		/*
		 * Triggers fragment menu construction in some android versions
		 * so mBusyManager must have already been created.
		 */
		setHasOptionsMenu(true);    // Calls invalidateOptionsMenu on parent activity
		mResources = getResources();
		mGroupTag = String.valueOf(DateTime.nowDate().getTime());
	}

	@Override public void draw(View view) {}

	@Override public void share() {}

	public void configureStandardMenuItems(final Menu menu) {

		FragmentActivity fragmentActivity = getActivity();

		if (menu == null || fragmentActivity == null) return;

		fragmentActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				/* Sign-in isn't dependent on an entity for policy */

				MenuItem item = menu.findItem(R.id.signin);
				if (item != null) {
					item.setVisible(!UserManager.getInstance().authenticated());
				}

				/* Remove menu items per policy */
				Entity entity = ((BaseActivity) getActivity()).getEntity();

				if (entity == null) return;

				item = menu.findItem(R.id.edit);
				if (item != null) {
					item.setVisible(MenuManager.canUserEdit(entity));
				}

				item = menu.findItem(R.id.add);
				if (item != null) {
					item.setVisible(MenuManager.showAction(Route.ADD, entity, null));
					item.setTitle(StringManager.getString(R.string.menu_item_add_entity, entity.schema));
				}

				item = menu.findItem(R.id.delete);
				if (item != null) {
					item.setVisible(MenuManager.canUserDelete(entity));
				}

				item = menu.findItem(R.id.remove);
				if (item != null) {
					item.setVisible(MenuManager.showAction(Route.REMOVE, entity, null));
				}

				item = menu.findItem(R.id.share);
				if (item != null) {
					item.setVisible(MenuManager.canUserShare(entity));
				}

				item = menu.findItem(R.id.share_photo);
				if (item != null) {
					item.setVisible(MenuManager.canUserShare(entity));
				}

				item = menu.findItem(R.id.signout);
				if (item != null) {
					item.setVisible(MenuManager.showAction(Route.EDIT, entity, null));
				}

				item = menu.findItem(R.id.navigate);
				if (item != null && UserManager.getInstance().authenticated()) {
					item.setVisible(entity.getLocation() != null);
				}

				item = menu.findItem(R.id.invite);
				if (item != null) {
					item.setVisible(MenuManager.canUserShare(entity));
				}
			}
		});
	}

	protected void scrollToTop(final Object scroller) {
		if (scroller instanceof ListView) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					((ListView) scroller).setSelection(0);
				}
			});
		}
		else if (scroller instanceof ScrollView) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					((ScrollView) scroller).smoothScrollTo(0, 0);
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public List<Integer> getMenuResIds() {
		return mMenuResIds;
	}

	protected int getLayoutId() {
		return 0;
	}
}