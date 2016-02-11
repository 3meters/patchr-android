package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.ui.base.BaseEdit;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

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

		final String passwordOld = mPasswordOld.getText().toString();
		final String password = mPassword.getText(). toString();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_changing_password, PasswordEdit.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncUpdatePassword");
				final ModelResult result = DataController.getInstance().updatePassword(
						UserManager.getInstance().getCurrentUser().id,
						passwordOld,
						password,
						PasswordEdit.class.getSimpleName(),
						NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Logger.i(this, "User changed password: "
							+ UserManager.getInstance().getCurrentUser().name
							+ " (" + UserManager.getInstance().getCurrentUser().id
							+ ")");

					UI.showToastNotification(StringManager.getString(R.string.alert_password_changed), Toast.LENGTH_SHORT);

					finish();
				}
				else {
					Errors.handleError(PasswordEdit.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
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