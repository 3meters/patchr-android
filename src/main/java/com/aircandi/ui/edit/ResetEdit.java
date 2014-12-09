package com.aircandi.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

import org.apache.http.HttpStatus;

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
			if (!Utilities.validEmail(mEmail.getText().toString())) {
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

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.show(BusyAction.ActionWithMessage, R.string.progress_reset_verify);
				UI.hideSoftInput(mEmail);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRequestPasswordReset");

				String email = mEmail.getText().toString().trim().toLowerCase(Locale.US);
				ModelResult result = Patchr.getInstance().getEntityManager().requestPasswordReset(email);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hide(false);
				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					mEmailConfirmed = false;
					if (result.serviceResponse.statusCode == HttpStatus.SC_NOT_FOUND) {

						/* No such email */
						Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
								, null
								, StringManager.getString(R.string.error_email_not_found)
								, null
								, ResetEdit.this
								, android.R.string.ok
								, null, null, null, null);

					}
					else if (result.serviceResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {

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
		}.execute();
	}

	protected void resetAndSignin() {

		Logger.d(this, "Resetting password for: " + mUser.email);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.show(BusyAction.ActionWithMessage, R.string.progress_signing_in);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncResetPassword");

				String password = mPassword.getText().toString();
				ModelResult result = Patchr.getInstance().getEntityManager().resetPassword(password, mUser);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in)
							+ " " + Patchr.getInstance().getCurrentUser().name, Toast.LENGTH_SHORT);

					setResultCode(Constants.RESULT_USER_SIGNED_IN);
					finish();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(ResetEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(ResetEdit.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.reset_edit;
	}
}