package com.aircandi.ui.user;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class PasswordEdit extends BaseEdit {

	private EditText mPasswordOld;
	private EditText mPassword;
	private EditText mPasswordConfirm;

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
		mPasswordConfirm = (EditText) findViewById(R.id.password_confirm);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (validate()) {
			update();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

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
				final ModelResult result = Aircandi.getInstance().getEntityManager().updatePassword(
						Aircandi.getInstance().getCurrentUser().id,
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
							+ Aircandi.getInstance().getCurrentUser().name
							+ " (" + Aircandi.getInstance().getCurrentUser().id
							+ ")");

					UI.showToastNotification(StringManager.getString(R.string.alert_password_changed), Toast.LENGTH_SHORT);

					finish();
				}
				else {
					Errors.handleError(PasswordEdit.this, result.serviceResponse);
				}
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
		if (mPasswordConfirm.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_confirmation)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mPassword.getText().length() < 6 || mPasswordConfirm.getText().length() < 6) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_weak)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (!mPassword.getText().toString().equals(mPasswordConfirm.getText().toString())) {

			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, StringManager.getString(R.string.error_signup_missmatched_passwords_title)
					, StringManager.getString(R.string.error_signup_missmatched_passwords_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			mPasswordConfirm.setText("");
			mPassword.setText("");
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.password_edit;
	}

}