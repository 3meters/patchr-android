package com.patchr.ui.edit;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.patchr.R;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class PasswordEdit extends BaseEdit {

	private EditText passwordOldField;
	private EditText passwordNewField;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getId() == R.id.signup_button) {
			submitAction();
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_save, menu);
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
		if (isValid()) {
			update();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		passwordOldField = (EditText) findViewById(R.id.password_old);
		passwordNewField = (EditText) findViewById(R.id.password);
	}

	protected void update() {

		if (!processing) {

			processing = true;
			final String passwordOld = this.passwordOldField.getText().toString();
			final String passwordNew = this.passwordNewField.getText().toString();
			busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_changing_password, PasswordEdit.this);

			subscription = RestClient.getInstance().updatePassword(UserManager.userId, passwordOld, passwordNew)
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);
						Reporting.track(AnalyticsCategory.EDIT, "Changed Password");
						Logger.i(this, String.format("User changed password: %1$s (%2$s)", UserManager.currentUser.name, UserManager.currentUser.id));
						UI.toast(StringManager.getString(R.string.alert_password_changed));
						finish();
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(this, error);
					});
		}
	}

	@Override protected boolean isValid() {

		if (passwordOldField.getText().length() == 0) {
			Dialogs.alert(R.string.error_missing_password_old, this);
			return false;
		}

		if (passwordNewField.getText().length() == 0) {
			Dialogs.alert(R.string.error_missing_password_new, this);
			return false;
		}

		if (passwordNewField.getText().length() < 6) {
			Dialogs.alert(R.string.error_missing_password_weak, this);
			return false;
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_password;
	}
}