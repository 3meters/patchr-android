package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
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
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.base.BaseEdit;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.net.HttpURLConnection;
import java.util.Locale;

public class ResetEdit extends BaseEdit {

	private EditText mEmail;
	private EditText mPassword;
	private Boolean mEmailConfirmed = false;
	private TextView mMessage;
	private User     mUser;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mMessage = (TextView) findViewById(R.id.content_message);
		mEmail = (EditText) findViewById(R.id.email);
		mPassword = (EditText) findViewById(R.id.password);

		mPassword.setImeOptions(EditorInfo.IME_ACTION_GO);
		mPassword.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					onResetButtonClick(null);
					return true;
				}
				return false;
			}
		});

		final String email = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
		if (email != null) {
			mEmail.setText(email);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onResetButtonClick(View view) {

		if (mProcessing) return;
		mProcessing = true;

		if (!mEmailConfirmed) {
			if (validate()) {
				requestReset();
			}
			else {
				mProcessing = false;
			}
		}
		else {
			if (validate()) {
				resetAndSignin();
			}
			else {
				mProcessing = false;
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected boolean validate() {
		if (!mEmailConfirmed) {
			if (mEmail.getText().length() == 0) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, StringManager.getString(R.string.error_missing_email)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
			if (!Utils.validEmail(mEmail.getText().toString())) {
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
			if (mPassword.getText().length() < 6) {
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

	protected void requestReset() {

		Logger.d(this, "Verifying email and install for password reset");

		final String email = mEmail.getText().toString().trim().toLowerCase(Locale.US);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_reset_verify, ResetEdit.this);
				UI.hideSoftInput(mEmail);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRequestPasswordReset");

				ModelResult result = DataController.getInstance().requestPasswordReset(email, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(false);
				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					mEmailConfirmed = false;
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
					mUser = (User) result.data;
					mEmailConfirmed = true;
					mEmail.setVisibility(View.GONE);
					mPassword.setVisibility(View.VISIBLE);
					mMessage.setText(StringManager.getString(R.string.label_reset_message_password));
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void resetAndSignin() {

		Logger.d(this, "Resetting password for: " + mUser.email);

		final String password = mPassword.getText().toString();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_signing_in, ResetEdit.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncResetPassword");
				ModelResult result = DataController.getInstance().resetPassword(password, mUser, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in)
							+ " " + UserManager.getInstance().getCurrentUser().name, Toast.LENGTH_SHORT);

					setResultCode(Constants.RESULT_USER_SIGNED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(ResetEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(ResetEdit.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.reset_edit;
	}
}