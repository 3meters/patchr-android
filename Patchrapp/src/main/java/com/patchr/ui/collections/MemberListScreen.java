package com.patchr.ui.collections;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.patchr.R;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.model.Link;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.service.RestClient;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;

import static com.patchr.objects.enums.FetchMode.AUTO;

/*
 * Just a veneer of commands to support BaseListScreen
 */
@SuppressWarnings("ucd")
public class MemberListScreen extends BaseListScreen {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getId() == R.id.remove_button) {
			final RealmEntity entity = (RealmEntity) view.getTag();
			removeRequestAction(entity);
		}
		else {
			super.onClick(view);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void removeRequestAction(final RealmEntity entity) {

		Link link = entity.getLink();

		Integer messageResId = link.enabled
		                       ? R.string.dialog_decline_approved_private_message
		                       : R.string.dialog_decline_requested_private_message;
		Integer okResId = link.enabled
		                  ? R.string.dialog_decline_approved_private_ok
		                  : R.string.dialog_decline_requested_private_ok;
		Integer cancelResId = link.enabled
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
						removeRequest(link.id);
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

	public void removeRequest(final String linkId) {

		AsyncTask.execute(() -> {
			RestClient.getInstance().deleteLinkById(linkId)
				.subscribe(
					response -> {
						Reporting.track(AnalyticsCategory.EDIT, "Removed Member Request");
						realm.executeTransaction(realm -> {
							entity.linkJson = null;
						});
						fetch(AUTO);
					},
					error -> {
						Logger.w(this, error.getLocalizedMessage());
					});
		});
	}
}