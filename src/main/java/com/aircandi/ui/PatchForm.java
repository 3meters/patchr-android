package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController.ActionType;
import com.aircandi.components.MenuManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.ActionEvent;
import com.aircandi.events.DataResultEvent;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Message;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.components.ListController;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Type;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
public class PatchForm extends BaseActivity {

	protected EntityFormFragment mHeaderFragment;
	protected String             mListLinkType;
	protected String             mNotificationId;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			mNotificationId = extras.getString(Constants.EXTRA_NOTIFICATION_ID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mCurrentFragment = new EntityListFragment();
		mHeaderFragment = new PatchFormFragment();

		mHeaderFragment
				.setEntityId(mEntityId)
				.setListLinkType(mListLinkType)
				.setTransitionType(mTransitionType)
				.setNotificationId(mNotificationId)
				.setParallax(true)
				.setLayoutResId(R.layout.widget_list_header_patch);

		((EntityListFragment) mCurrentFragment)
				.setScopingEntityId(mEntityId)
				.setActionType(ActionType.ACTION_GET_ENTITIES)
				.setLinkSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setLinkType(Constants.TYPE_LINK_CONTENT)
				.setLinkDirection(Direction.in.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_messages))
				.setHeaderFragment(mHeaderFragment)
				.setHeaderViewResId(R.layout.entity_form)
				.setFooterViewResId(R.layout.widget_list_footer_message)
				.setListItemResId(R.layout.temp_listitem_message)
				.setListLayoutResId(R.layout.message_list_patch_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListViewType(EntityListFragment.ViewType.LIST)
				.setBubbleButtonMessageResId(R.string.button_list_share);

		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_sign_in);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_invite);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_map);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_edit_patch);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_delete);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_qrcode);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report);

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
		/*
		 * Cherry pick the entity so we can add some wrapper functionality.
		 */
		if (event.entity != null && event.entity.id.equals(mEntityId)) {
			mEntity = event.entity;
			Patchr.getInstance().setCurrentPatch(mEntity);
		}
	}

	@Subscribe
	public void onViewClick(ActionEvent event) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. There are actions
		 * that should be handled at the activity level like add a new entity.
		 */
		if (mProcessing) return;

		if (event.view != null) {
			mProcessing = true;
			Integer id = event.view.getId();

			/* Dynamic button we need to redirect */
			if (id == R.id.button_alert) {
				id = (Integer) event.view.getTag();
			}

			if (id == R.id.add || id == R.id.list_fab) {
				onAdd(new Bundle());
			}
			else {
				mProcessing = false;
				super.onViewClick(event);
			}
			mProcessing = false;
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

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/*
				 * Non-members can't add messages to private patches.
				 */
				if (mEntity != null && mEntity instanceof Patch) {
					Patch patch = (Patch) mEntity;
					ListController controller = ((EntityListFragment) mCurrentFragment).getListController();
					if (patch.privacy != null
							&& patch.privacy.equals(Constants.PRIVACY_PRIVATE)
							&& !patch.isVisibleToCurrentUser()) {
						controller.getFloatingActionController().fadeOut();
					}
					else {
						controller.getFloatingActionController().fadeIn();
					}
				}
			}
		});
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

	@Override
	public void onAdd(Bundle extras) {

		if (mEntity == null) return;

		if (MenuManager.canUserAdd(mEntity)) {

			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(mEntity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), mEntity.name);
			}

			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
			extras.putString(Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

			Patchr.router.route(this, Route.NEW, null, extras);
		}
		else if (Type.isTrue(((Patch) mEntity).locked)) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED || Patchr.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					AnimationManager.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setSubject(String.format(StringManager.getString(R.string.label_patch_share_subject)
				, (mEntity.name != null) ? mEntity.name : "A"));

		builder.setType("text/plain");
		builder.setText(String.format(StringManager.getString(R.string.label_patch_share_body), mEntityId));
		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_patch_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

		builder.startChooser();
	}

	@Override
	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.patch_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			Patchr.getInstance().setCurrentPatch(mEntity);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}