package com.patchr.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.MenuManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Entity;
import com.patchr.objects.Route;
import com.patchr.ui.BaseActivity;
import com.patchr.utilities.DateTime;

/**
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
public abstract class BaseFragment extends Fragment {

	protected String  groupTag;
	protected boolean isVisible;
	protected boolean processing;
	protected boolean recreated;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		recreated = (savedInstanceState != null && !savedInstanceState.isEmpty());
		groupTag = String.valueOf(DateTime.nowDate().getTime());
		if (recreated) {
			Intent intent = getActivity().getIntent();
			getActivity().finish();
			startActivity(intent);
		}
	}

	@Override public void onStart() {
		super.onStart();
		if (getActivity() != null && !getActivity().isFinishing()) {
			configureStandardMenuItems(((BaseActivity) getActivity()).optionMenu);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

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

				MenuItem item = menu.findItem(R.id.login);
				if (item != null) {
					item.setVisible(!UserManager.shared().authenticated());
				}

				/* Remove menu items per policy */
				Entity entity = ((BaseActivity) getActivity()).entity;

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