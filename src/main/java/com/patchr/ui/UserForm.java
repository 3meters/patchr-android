package com.patchr.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.ui.base.BaseEntityForm;
import com.patchr.ui.base.BaseFragment;
import com.patchr.utilities.Integers;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
@SuppressWarnings("ucd")
public class UserForm extends BaseEntityForm {

	protected EntityFormFragment mHeaderFragment;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		Boolean currentUser = UserManager.getInstance().authenticated() && UserManager.getInstance().getCurrentUser().id.equals(mEntityId);
		mLinkProfile = currentUser ? LinkSpecType.LINKS_FOR_USER_CURRENT : LinkSpecType.LINKS_FOR_USER;

		mCurrentFragment = new EntityListFragment();
		mHeaderFragment = new UserFormFragment();

		mHeaderFragment
				.setEntityId(mEntityId)
				.setListLinkType(mListLinkType)
				.setTransitionType(mTransitionType)
				.setNotificationId(mNotificationId)
				.setLayoutResId(R.layout.widget_list_header_user);

		((EntityListFragment) mCurrentFragment)
				.setScopingEntityId(mEntityId)
				.setLinkSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setLinkType(Constants.TYPE_LINK_CREATE)
				.setLinkDirection(Link.Direction.out.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_messages))
				.setHeaderFragment(mHeaderFragment)
				.setHeaderViewResId(R.layout.entity_form)
				.setListViewType(EntityListFragment.ViewType.LIST)
				.setListLayoutResId(R.layout.entity_list_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListItemResId(R.layout.temp_listitem_message);

		if (!currentUser) {
			((EntityListFragment) mCurrentFragment).setSelfBind(false);
		}

		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_edit_user);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_sign_out);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report);

		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(DataResultEvent event) {
		/*
		 * Cherry pick the entity so we can add some wrapper functionality.
		 */
		if (event.entity == null || event.entity.id.equals(mEntityId)) {
			mEntity = event.entity;

			/* Update menu items */
			if (mOptionMenu != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						configureStandardMenuItems(mOptionMenu);
					}
				});
			}
		}
	}

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		/*
		 * Gets called direct at the activity level and receives
		 * events from fragments.
		 */
		mProcessing = false;
		mUiController.getBusyController().hide(false);
	}

	@Subscribe
	public void onViewClick(ActionEvent event) {
		super.onViewClick(event);
	}

	@Override
	public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		if (mHeaderFragment != null) {
			mHeaderFragment.onRefresh();
		}
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			((EntityListFragment) mCurrentFragment).onRefresh();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.user_form;
	}
}