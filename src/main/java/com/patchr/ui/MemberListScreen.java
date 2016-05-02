package com.patchr.ui;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Entity;
import com.patchr.ui.components.RecyclePresenter;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;

import static com.patchr.objects.FetchMode.AUTO;

@SuppressWarnings("ucd")
public class MemberListScreen extends ListScreen {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getId() == R.id.remove_button) {
			final Entity entity = (Entity) view.getTag();
			removeRequestAction(entity);
		}
		else {
			super.onClick(view);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {

		this.listPresenter = new RecyclePresenter(this);
		this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
				, null
				, this.listLinkDirection
				, this.listLinkType
				, this.listLinkSchema
				, this.entityId);

		super.initialize(savedInstanceState);
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
					Reporting.track(AnalyticsCategory.EDIT, "Removed Member Request");
					fetch(AUTO);
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != Constants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(MemberListScreen.this, result.serviceResponse);
					}
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}