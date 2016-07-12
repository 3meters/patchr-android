package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.ResponseCode;
import com.patchr.objects.enums.State;
import com.patchr.ui.components.BusyController;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class PasswordEdit extends BaseEdit {

	private EditText passwordOld;
	private EditText password;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getId() == R.id.signup_button) {
			submitAction();
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (inputState.equals(State.Editing)) {
			getMenuInflater().inflate(R.menu.menu_save, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void submitAction() {

		if (this.processing) return;
		this.processing = true;

		if (isValid()) {
			update();
		}
		else {
			this.processing = false;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		passwordOld = (EditText) findViewById(R.id.password_old);
		password = (EditText) findViewById(R.id.password);
	}

	@Override protected void update() {

		final String passwordOld = this.passwordOld.getText().toString();
		final String password = this.password.getText().toString();

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_changing_password, PasswordEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncUpdatePassword");
				final ModelResult result = DataController.getInstance().updatePassword(
						UserManager.currentUser.id,
						passwordOld,
						password,
						PasswordEdit.class.getSimpleName(),
						NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				busyController.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Reporting.track(AnalyticsCategory.EDIT, "Changed Password");
					Logger.i(this, "User changed password: "
							+ UserManager.currentUser.name
							+ " (" + UserManager.currentUser.id
							+ ")");

					UI.toast(StringManager.getString(R.string.alert_password_changed));

					finish();
				}
				else {
					Errors.handleError(PasswordEdit.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected boolean isValid() {

		if (passwordOld.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_old)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (password.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (password.getText().length() < 6) {
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

	@Override protected int getLayoutId() {
		return R.layout.edit_password;
	}
}