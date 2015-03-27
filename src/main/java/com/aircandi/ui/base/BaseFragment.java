package com.aircandi.ui.base;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.ScrollView;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.Logger;
import com.aircandi.components.MenuManager;
import com.aircandi.interfaces.IForm;
import com.aircandi.objects.Entity;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.DateTime;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFragment extends Fragment implements IForm {
	/*
	 * Fragment lifecycle
	 *
	 * - onAttach (activity may not be fully initialized)
	 * - onCreate
	 * - onCreateView
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

	protected Resources mResources;
	protected String    mGroupTag;
	protected Boolean mIsVisible = false;

	/* Resources */
	protected List<Integer> mMenuResIds = new ArrayList<Integer>();

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAttach(Activity activity) {
		/* Called when the fragment has been associated with the activity. */
		super.onAttach(activity);
		Logger.d(this, "Fragment attached");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(this, "Fragment created: contextId: " + this.hashCode());
		/*
		 * Triggers fragment menu construction in some android versions
		 * so mBusyManager must have already been created.
		 */
		setHasOptionsMenu(true);
		mResources = getResources();
		mGroupTag = String.valueOf(DateTime.nowDate().getTime());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logger.d(this, "Fragment view created");
		if (getActivity() == null || getActivity().isFinishing()) return null;

		final View view = inflater.inflate(getLayoutId(), container, false);
		return view;
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {

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

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Logger.d(this, "Activity for fragment created");
	}

	public void onViewLayout() {
		/*
		 * Called when initial view layout has completed and
		 * views have been measured and sized.
		 */
		Logger.d(this, "Fragment view layout completed");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Logger.d(this, "Fragment detached");
	}

	@Override
	public void onAdd(Bundle extras) {}

	@Override
	public void onHelp() {}

	@Override
	public void onError() {}

	public abstract void onScollToTop();

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void unpackIntent() {}

	@Override
	public void initialize(Bundle savedInstanceState) {}

	@Override
	public void draw(View view) {}

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

	@Override
	public void share() {}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public List<Integer> getMenuResIds() {
		return mMenuResIds;
	}

	protected int getLayoutId() {
		return 0;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Patchr.router.route(getActivity(), Patchr.router.routeForMenuId(item.getItemId()), null, null);
		return true;
	}

	public void configureStandardMenuItems(Menu menu) {

		/* Remove menu items per policy */
		Entity entity = ((BaseActivity) getActivity()).getEntity();

		MenuItem item = menu.findItem(R.id.edit);
		if (item != null) {
			item.setVisible(MenuManager.canUserEdit(entity));
		}

		item = menu.findItem(R.id.delete);
		if (item != null) {
			item.setVisible(MenuManager.canUserDelete(entity));
		}

		item = menu.findItem(R.id.share);
		if (item != null) {
			item.setVisible(MenuManager.canUserShare(entity));
		}

		item = menu.findItem(R.id.share_photo);
		if (item != null) {
			item.setVisible(MenuManager.canUserShare(entity));
		}

		item = menu.findItem(R.id.invite);
		if (item != null) {
			item.setVisible(MenuManager.canUserShare(entity));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		/*
		 * Called everytime the fragment is started or restarted.
		 */
		Logger.d(this, "Fragment start");
		Dispatcher.getInstance().register(this);
		super.onStart();
	}

	@Override
	public void onResume() {
		Logger.d(this, "Fragment resume");
		if (getActivity() != null && getActivity() instanceof AircandiForm) {
			((AircandiForm) getActivity()).updateNotificationIndicator(false);
		}
		super.onResume();
	}

	@Override
	public void onPause() {
		/*
		 * user might be leaving fragment so do any work needed
		 * because they might not come back.
		 */
		Logger.d(this, "Fragment pause");

		super.onPause();
	}

	@Override
	public void onStop() {
		/*
		 * Triggers
		 * - Switching to another fragment.
		 * - Switching to launcher.
		 * - Killing activity.
		 * - Navigating to another activity.
		 */
		Logger.d(this, "Fragment stop");
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		Logger.d(this, "Fragment destroy view");
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Logger.d(this, "Fragment destroy");
		super.onDestroy();
	}
}