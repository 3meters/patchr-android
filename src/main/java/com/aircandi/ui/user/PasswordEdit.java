package com.aircandi.ui.user;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class PasswordEdit extends BaseEdit {

	private EditText mPasswordOld;
	private EditText mPassword;

	/* Inputs */
	protected String mEntityId;

	@Override
	public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mPasswordOld = (EditText) findViewById(R.id.password_old);
		mPassword = (EditText) findViewById(R.id.password);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAccept() {

		if (mProcessing) return;
		mProcessing = true;

		if (validate()) {
			update();
		}
		else {
			mProcessing = false;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void update() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_changing_password);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncUpdatePassword");
				final ModelResult result = Patchr.getInstance().getEntityManager().updatePassword(
						Patchr.getInstance().getCurrentUser().id,
						mPasswordOld.getText().toString(),
						mPassword.getText().toString(),
						PasswordEdit.class.getSimpleName());
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Logger.i(this, "User changed password: "
							+ Patchr.getInstance().getCurrentUser().name
							+ " (" + Patchr.getInstance().getCurrentUser().id
							+ ")");

					UI.showToastNotification(StringManager.getString(R.string.alert_password_changed), Toast.LENGTH_SHORT);

					finish();
				}
				else {
					Errors.handleError(PasswordEdit.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.execute();
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
		if (mPasswordOld.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mPassword.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mPassword.getText().length() < 6) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_weak)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.password_edit;
	}
}