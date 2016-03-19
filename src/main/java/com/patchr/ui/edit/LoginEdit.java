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
import com.patchr.components.UserManager;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.Locale;

public class LoginEdit extends BaseEdit {

	private EditText email;
	private EditText password;
	private CheckBox passwordUnmask;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onForgotPasswordButtonClick(View view) {
		Patchr.router.route(this, Route.PASSWORD_RESET, null, null);
	}

	public void onLoginButtonClick(View view) {
		if (validate()) {
			signin();
		}
	}

	public void onSignupButtonClick(View view) {
		Patchr.router.route(this, Route.SIGNUP, null, null);
	}

	@Override public void onSubmit() {
		if (validate()) {
			signin();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		email = (EditText) findViewById(R.id.email);
		password = (EditText) findViewById(R.id.password);
		passwordUnmask = (CheckBox) findViewById(R.id.chk_unmask);

		passwordUnmask.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					password.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					FontManager.getInstance().setTypefaceDefault(password);
				}
				else {
					password.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					FontManager.getInstance().setTypefaceDefault(password);
				}
			}
		});

		password.setImeOptions(EditorInfo.IME_ACTION_GO);
		password.setOnEditorActionListener(new OnEditorActionListener() {

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

	@Override public void bind() {
		final String email = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
		if (email != null) {
			this.email.setText(email);
			password.requestFocus();
		}
	}

	@Override protected boolean validate() {
		if (password.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password)
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
		return true;
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == Constants.ACTIVITY_SIGNIN
				|| requestCode == Constants.ACTIVITY_RESET_AND_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				setResult(Constants.RESULT_USER_SIGNED_IN);
				finish();
				AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override protected int getLayoutId() {
		return R.layout.login_edit;
	}

	private void signin() {

		final String email = this.email.getText().toString().toLowerCase(Locale.US);
		final String password = this.password.getText().toString();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				uiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_signing_in, LoginEdit.this);
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

				uiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in) + " " + UserManager.currentUser.name, Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_USER_SIGNED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(LoginEdit.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}