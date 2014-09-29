package com.aircandi.ui.user;

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

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

import java.util.Locale;

public class SignInEdit extends BaseEdit {

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
		final String email = Patch.settings.getString(StringManager.getString(R.string.setting_last_email), null);
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
		Patch.dispatch.route(this, Route.PASSWORD_RESET, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onSignInButtonClick(View view) {
		signin();
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		Patch.dispatch.route(this, Route.REGISTER, null, null, null);
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
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_signing_in);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSignIn");
				ModelResult result = Patch.getInstance().getEntityManager().signin(email, password, SignInEdit.class.getSimpleName());
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					result = MessagingManager.getInstance().registerInstallWithAircandi();
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in)
							+ " " + Patch.getInstance().getCurrentUser().name, Toast.LENGTH_SHORT);

					setResultCode(Constants.RESULT_USER_SIGNED_IN);
					finish();
					Patch.getInstance().getAnimationManager().doOverridePendingTransition(SignInEdit.this, TransitionType.FORM_TO_PAGE);
				}
				else {
					Errors.handleError(SignInEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected boolean validate() {
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
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN || requestCode == Constants.ACTIVITY_RESET_AND_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				setResultCode(Constants.RESULT_USER_SIGNED_IN);
				finish();
				Patch.getInstance().getAnimationManager().doOverridePendingTransition(SignInEdit.this, TransitionType.FORM_TO_PAGE);
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.signin_edit;
	}

}