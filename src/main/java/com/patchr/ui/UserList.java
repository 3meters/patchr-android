package com.patchr.ui;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.Entity;
import com.patchr.objects.Route;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;

import org.greenrobot.eventbus.Subscribe;

import static com.patchr.objects.FetchMode.AUTO;

@SuppressWarnings("ucd")
public class UserList extends BaseActivity {
	/*
	 * Thin wrapper around a list fragment.
	 */
	protected String  mListLinkType;
	protected Integer mListTitleResId;
	protected Integer mListItemResId;
	protected Integer mListEmptyMessageResId;

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			entity = DataController.getStoreEntity(entityId);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mListTitleResId = extras.getInt(Constants.EXTRA_LIST_TITLE_RESID);
			mListItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID);
			mListEmptyMessageResId = extras.getInt(Constants.EXTRA_LIST_EMPTY_RESID);
		}
	}

	@Override protected void onResume() {
		super.onResume();
		draw(null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onProcessingComplete(ProcessingCompleteEvent event) {
		/*
		 * Gets called direct at the activity level and receives
		 * events from fragments.
		 */
		processing = false;
		uiController.getBusyController().hide(false);
	}

	public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		if (currentFragment != null && currentFragment instanceof EntityListFragment) {
			((EntityListFragment) currentFragment).listPresenter.refresh();
		}
	}

	public void onShareButtonClick(View view) {
		Patchr.router.route(this, Route.SHARE, entity, null);
	}

	public void onDeleteRequestClick(View view) {

		final Entity entity = (Entity) view.getTag();
		Integer messageResId = entity.linkEnabled
		                       ? R.string.dialog_decline_approved_private_message
		                       : R.string.dialog_decline_requested_private_message;
		Integer okResId = entity.linkEnabled
		                  ? R.string.dialog_decline_approved_private_ok
		                  : R.string.dialog_decline_requested_private_ok;
		Integer cancelResId = entity.linkEnabled
		                      ? R.string.dialog_decline_approved_private_cancel
		                      : R.string.dialog_decline_requested_private_cancel;

		/* Confirm a decline since the user won't be able to undo */
		final AlertDialog declineDialog = Dialogs.alertDialog(null
				, null
				, StringManager.getString(messageResId)
				, null
				, this
				, okResId
				, cancelResId
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							removeMember(entity.id);
							dialog.dismiss();
						}
						else if (which == DialogInterface.BUTTON_NEGATIVE) {
							dialog.dismiss();
						}
					}
				}
				, null);

		declineDialog.setCanceledOnTouchOutside(false);
		declineDialog.show();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		currentFragment = new EntityListFragment();

//		((EntityListFragment) currentFragment).listPresenter
//				.setScopingEntityId(entityId)
//				//.setLinkSchema(Constants.SCHEMA_ENTITY_USER)
//				//.setLinkType(mListLinkType)
//				//.setLinkDirection(Direction.in.name())
//				//.setLinkWhere(null)
//				//.setPageSize(Integers.getInteger(R.integer.page_size_messages))
//				.setListLoadingResId(R.layout.temp_listitem_loading)
//				.setListItemResId(mListItemResId)
//				.setTitleResId(mListTitleResId);

//		if (mListLinkType != null && mListLinkType.equals(Constants.TYPE_LINK_WATCH) && !mEntity.isOwnedByCurrentUser()
//				&& !mEntity.ownerId.equals(Constants.ADMIN_USER_ID)) {
//			((EntityListFragment) mCurrentFragment).getListController().setLinkWhere(Maps.asMap("enabled", true));
//		}

		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, currentFragment)
				.commit();
	}

	public void draw(View view) {
	}

	public void removeMember(final String fromId) {

		final String actionEvent = "declined_watch_entity";

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncWatchEntity");
				return DataController.getInstance().deleteLink(fromId
						, entity.id
						, Constants.TYPE_LINK_WATCH
						, false
						, entity.schema
						, actionEvent, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					((EntityListFragment) currentFragment).listPresenter.fetch(AUTO);
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != Constants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(UserList.this, result.serviceResponse);
					}
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected int getLayoutId() {
		return R.layout.watcher_list;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}