package com.patchr.ui;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.Photo;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.ListPresenter;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;

import static com.patchr.objects.FetchMode.AUTO;

@SuppressWarnings("ucd")
public class ListScreen extends BaseScreen implements SwipeRefreshLayout.OnRefreshListener {
	/*
	 * Thin wrapper around a list fragment.
	 */
	protected String        listLinkType;
	protected String        listLinkSchema;
	protected Integer       listTitleResId;
	protected Integer       listEmptyMessageResId;
	protected Integer       listItemResId;
	protected String        listLinkDirection;
	protected Entity        entity;
	protected String        entityId;
	protected ListPresenter listPresenter;

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override public void onResume() {
		super.onResume();

		if (!isFinishing()) {
			fetch(FetchMode.AUTO);
			if (this.listPresenter != null) {
				this.listPresenter.onResume();
			}
		}
	}

	@Override public void onPause() {
		super.onPause();
		if (this.listPresenter != null) {
			this.listPresenter.onPause();
		}
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getId() == R.id.remove_button) {
			final Entity entity = (Entity) view.getTag();
			removeRequestAction(entity);
		}
		else if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				navigateToPhoto(photo);
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				navigateToEntity(entity);
			}
		}
	}

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			this.entity = DataController.getStoreEntity(this.entityId);

			this.listItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_user);
			this.listLinkDirection = extras.getString(Constants.EXTRA_LIST_LINK_DIRECTION, Link.Direction.in.name());
			this.listLinkSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_USER);
			this.listLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			this.listEmptyMessageResId = extras.getInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.label_empty);
			this.listTitleResId = extras.getInt(Constants.EXTRA_LIST_TITLE_RESID);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		assert this.rootView != null;

		this.listPresenter = new ListPresenter(this);
		this.listPresenter.listView = (AbsListView) ((ViewGroup) this.rootView.findViewById(R.id.swipe)).getChildAt(1);
		this.listPresenter.listItemResId = this.listItemResId;
		this.listPresenter.busyPresenter = new BusyPresenter();
		this.listPresenter.busyPresenter.setProgressBar(this.rootView.findViewById(R.id.list_progress));
		this.listPresenter.emptyPresenter = new EmptyPresenter(this.rootView.findViewById(R.id.list_message));
		this.listPresenter.emptyPresenter.setLabel(StringManager.getString(this.listEmptyMessageResId));

		this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
				, Maps.asMap("enabled", true)
				, this.listLinkDirection
				, this.listLinkType
				, this.listLinkSchema
				, this.entityId);

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) this.rootView.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyPresenter.setSwipeRefresh(swipeRefresh);
		}

		this.listPresenter.initialize(this, this.rootView);        // We init after everything is setup
	}

	@Override protected int getLayoutId() {
		return R.layout.list_screen;
	}

	public void fetch(final FetchMode fetchMode) {
		listPresenter.fetch(fetchMode);
	}

	public void removeRequestAction(final Entity entity) {

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
							removeRequest(entity.id);
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

	public void removeRequest(final String fromId) {

		final String actionEvent = "declined_watch_entity";

		new AsyncTask() {

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncWatchEntity");
				return DataController.getInstance().deleteLink(fromId
						, entity.id
						, Constants.TYPE_LINK_MEMBER
						, false
						, entity.schema
						, actionEvent, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					((EntityListFragment) currentFragment).listPresenter.fetch(AUTO);
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != Constants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(ListScreen.this, result.serviceResponse);
					}
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}