package com.patchr.ui.base;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Entity;
import com.patchr.objects.Route;
import com.patchr.utilities.DateTime;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFragment extends Fragment {
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
	protected Boolean mRecreated = false;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRecreated = (savedInstanceState != null && !savedInstanceState.isEmpty());
		mResources = getResources();
		mGroupTag = String.valueOf(DateTime.nowDate().getTime());
		setHasOptionsMenu(true);    // Calls invalidateOptionsMenu on parent activity
	}

	@Override public void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
		if (getActivity() != null && !getActivity().isFinishing()) {
			configureStandardMenuItems(((BaseActivity) getActivity()).getOptionMenu());
		}
	}

	@Override public void onStop() {
		super.onStop();
		Dispatcher.getInstance().unregister(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		/*
		 * Called after the same method on activity. We get called because
		 * we set setHasOptionsMenu to true. Menu items are appended.
		 */
		Logger.d(this, "Creating fragment options menu");
		for (Integer menuResId : mMenuResIds) {
			inflater.inflate(menuResId, menu);
		}
		configureStandardMenuItems(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Patchr.router.route(getActivity(), Patchr.router.routeForMenuId(item.getItemId()), null, null);
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

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

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public List<Integer> getMenuResIds() {
		return mMenuResIds;
	}
}