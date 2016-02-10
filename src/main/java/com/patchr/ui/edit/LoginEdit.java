package com.patchr.ui.edit;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.FontManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseEdit;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.Locale;

public class LoginEdit extends BaseEdit {

	private EditText mEmail;
	private EditText mPassword;
	private CheckBox mPasswordUnmask;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEmail = (EditText) findViewById(R.id.email);
		mPassword = (EditText) findViewById(R.id.password);
		mPasswordUnmask = (CheckBox) findViewById(R.id.chk_unmask);

		mPasswordUnmask.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mPassword.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					FontManager.getInstance().setTypefaceDefault(mPassword);
				}
				else {
					mPassword.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					FontManager.getInstance().setTypefaceDefault(mPassword);
				}
			}
		});

		mPassword.setImeOptions(EditorInfo.IME_ACTION_GO);
		mPassword.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					signin();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void draw(View view) {
		final String email = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
		if (email != null) {
			mEmail.setText(email);
			mPassword.requestFocus();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@SuppressWarnings("ucd")
	public void onForgotPasswordButtonClick(View view) {
		Patchr.router.route(this, Route.PASSWORD_RESET, null, null);
	}

	@SuppressWarnings("ucd")
	public void onSignInButtonClick(View view) {
		if (validate()) {
			signin();
		}
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		Patchr.router.route(this, Route.REGISTER, null, null);
	}

	@Override
	public void onAccept() {
		if (validate()) {
			signin();
		}
	}

	private void signin() {

		final String email = mEmail.getText().toString().toLowerCase(Locale.US);
		final String password = mPassword.getText().toString();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_signing_in, LoginEdit.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSignIn");
				ModelResult result = DataController.getInstance().signin(email, password, LoginEdit.class.getSimpleName(), NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in)
							+ " " + Patchr.getInstance().getCurrentUser().name, Toast.LENGTH_SHORT);

					setResultCode(Constants.RESULT_USER_SIGNED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(LoginEdit.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override
	protected boolean validate() {
		if (mPassword.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password)
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
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == Constants.ACTIVITY_SIGNIN
				|| requestCode == Constants.ACTIVITY_RESET_AND_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				setResultCode(Constants.RESULT_USER_SIGNED_IN);
				finish();
				AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.login_edit;
	}
}