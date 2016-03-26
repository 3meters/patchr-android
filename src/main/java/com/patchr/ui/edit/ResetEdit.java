package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.net.HttpURLConnection;
import java.util.Locale;

public class ResetEdit extends BaseEdit {

	private EditText email;
	private EditText password;
	private boolean  emailConfirmed;
	private TextView message;
	private User     user;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_submit, menu);
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			resetAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		message = (TextView) findViewById(R.id.content_message);
		email = (EditText) findViewById(R.id.email);
		password = (EditText) findViewById(R.id.password);

		if (password != null) {
			password.setImeOptions(EditorInfo.IME_ACTION_GO);
			password.setOnEditorActionListener(new TextView.OnEditorActionListener() {

				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_GO) {
						resetAction();
						return true;
					}
					return false;
				}
			});
		}
	}

	@Override public void bind() {
		final String email = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
		if (email != null) {
			this.email.setText(email);
		}
	}

	@Override protected boolean validate() {

		gather();
		if (!emailConfirmed) {

			if (email.getText().length() == 0) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, StringManager.getString(R.string.error_missing_email)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}

			if (!Utils.validEmail(email.getText().toString())) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, StringManager.getString(R.string.error_invalid_email)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}
		else {

			if (password.getText().length() < 6) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, StringManager.getString(R.string.error_missing_password)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}
		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_reset;
	}

	public void resetAction() {

		if (this.processing) return;
		this.processing = true;

		if (!emailConfirmed) {
			if (validate()) {
				requestReset();
			}
			else {
				this.processing = false;
			}
		}
		else {
			if (validate()) {
				resetAndSignin();
			}
			else {
				this.processing = false;
			}
		}
	}

	protected void requestReset() {

		Logger.d(this, "Verifying email and install for password reset");

		final String email = this.email.getText().toString().trim().toLowerCase(Locale.US);

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_reset_verify, ResetEdit.this);
				UI.hideSoftInput(ResetEdit.this.email);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRequestPasswordReset");

				ModelResult result = DataController.getInstance().requestPasswordReset(email, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(false);
				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					emailConfirmed = false;
					if (result.serviceResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {

						/* No such email */
						Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
								, null
								, StringManager.getString(R.string.error_email_not_found)
								, null
								, ResetEdit.this
								, android.R.string.ok
								, null, null, null, null);
					}
					else if (result.serviceResponse.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {

						/* No successful signin on this install */
						Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
								, null
								, StringManager.getString(R.string.error_install_not_found)
								, null
								, ResetEdit.this
								, android.R.string.ok
								, null, null, null, null);
					}
					else {
						Errors.handleError(ResetEdit.this, result.serviceResponse);
					}
				}
				else {
					user = (User) result.data;
					emailConfirmed = true;
					ResetEdit.this.email.setVisibility(View.GONE);
					password.setVisibility(View.VISIBLE);
					message.setText(StringManager.getString(R.string.label_reset_message_password));
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void resetAndSignin() {

		Logger.d(this, "Resetting password for: " + user.email);

		final String password = this.password.getText().toString();

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_signing_in, ResetEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncResetPassword");
				ModelResult result = DataController.getInstance().resetPassword(password, user, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in)
							+ " " + UserManager.currentUser.name, Toast.LENGTH_SHORT);

					setResult(Constants.RESULT_USER_SIGNED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(ResetEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(ResetEdit.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}