package com.patchr.ui.edit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.exceptions.ServiceException;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.widgets.ClearableEditText;
import com.patchr.ui.widgets.PasswordEditText;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.Locale;

import rx.Subscription;

public class LoginEdit extends BaseEdit {

	private   ClearableEditText email;
	private   PasswordEditText  password;
	private   TextView          title;
	private   View              forgotPasswordButton;
	private   View              loginButton;
	protected Subscription      subscription;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (inputState.equals(State.Signup)) {
			getMenuInflater().inflate(R.menu.menu_next, menu);
		}
		else {
			getMenuInflater().inflate(R.menu.menu_login, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.next || item.getItemId() == R.id.login) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onClick(View view) {
		if (view.getId() == R.id.signup_button) {
			submitAction();
		}
		else if (view.getId() == R.id.forgot_password_button) {
			Intent intent = new Intent(this, ResetEdit.class);
			startActivityForResult(intent, Constants.ACTIVITY_RESET_AND_SIGNIN);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		title = (TextView) findViewById(R.id.title);
		loginButton = findViewById(R.id.signup_button);
		forgotPasswordButton = findViewById(R.id.forgot_password_button);
		email = (ClearableEditText) findViewById(R.id.email);
		password = (PasswordEditText) findViewById(R.id.password);
		if (password != null) {
			password.setOnEditorActionListener((textView, actionId, event) -> {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					login();
					return true;
				}
				return false;
			});
		}

		if (inputState.equals(State.Signup)) {
			title.setText(R.string.form_title_login_signup);
			loginButton.setVisibility(View.GONE);
			forgotPasswordButton.setVisibility(View.GONE);
		}
	}

	@Override public void bind() {
		final String email = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
		if (email != null) {
			this.email.setText(email);
			password.requestFocus();
		}
	}

	@Override protected boolean isValid() {

		if (password.getText().length() == 0) {
			Dialogs.alert(R.string.error_missing_password, this);
			return false;
		}
		if (password.getText().length() < 6) {
			Dialogs.alert(R.string.error_missing_password_weak, this);
			return false;
		}
		if (!Utils.validEmail(email.getText().toString())) {
			Dialogs.alert(R.string.error_invalid_email, this);
			return false;
		}
		return true;
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == Constants.ACTIVITY_SIGNUP) {
			if (resultCode == Constants.RESULT_USER_LOGGED_IN) {
				setResult(Constants.RESULT_USER_LOGGED_IN);
				finish();
				AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
			}
		}
		else if (requestCode == Constants.ACTIVITY_LOGIN
			|| requestCode == Constants.ACTIVITY_RESET_AND_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_LOGGED_IN) {
				setResult(Constants.RESULT_USER_LOGGED_IN);
				finish();
				AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_login;
	}

	@Override public void submitAction() {
		if (isValid()) {
			if (inputState.equals(State.Login)) {
				login();
			}
			else if (inputState.equals(State.Signup)) {
				validateEmail();
			}
		}
	}

	private void login() {

		busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_logging_in, LoginEdit.this);
		final String email = this.email.getText().toString().toLowerCase(Locale.US);
		final String password = this.password.getText().toString();

		this.subscription = UserManager.shared().login(email, password)
			.subscribe(
				response -> {
					busyController.hide(true);
					if (response.isSuccessful()) {
						Logger.i(this, "User signed in: " + UserManager.currentUser.name);
						UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
						setResult(Constants.RESULT_USER_LOGGED_IN);
						finish();
						AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
					}
					else {
						/* Cherry pick validation errors */
						if (response.serviceCode.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_FOUND
							|| response.serviceCode.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
							Dialogs.alert(R.string.error_signin_failed, LoginEdit.this);
						}
						else {
							Errors.handleError(LoginEdit.this, response.error);
						}
					}
				},
				error -> {
					busyController.hide(true);
					String message = error.getLocalizedMessage();
					if (error instanceof ServiceException) {
						message = ((ServiceException)error).message;
					}
					Logger.w(this, message);
					UI.toast(message);
				});
	}

	private void validateEmail() {

		final String email = this.email.getText().toString().toLowerCase(Locale.US);
		busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_reset_verify, LoginEdit.this);

		subscription = RestClient.getInstance().validEmail(email)
			.subscribe(
				response -> {
					busyController.hide(true);
					if (response.isSuccessful()) {
						if (response.count.intValue() == 0) {
							Intent intent = new Intent(this, ProfileEdit.class);
							intent.putExtra(Constants.EXTRA_STATE, State.Signup);
							intent.putExtra(Constants.EXTRA_EMAIL, email);
							intent.putExtra(Constants.EXTRA_PASSWORD, password.getText().toString());
							startActivityForResult(intent, Constants.ACTIVITY_SIGNUP);
							AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
						}
						else {
							UI.toast("Email has already been used.");
						}
					}
				},
				error -> {
					busyController.hide(true);
					Logger.w(this, error.getLocalizedMessage());
				});
	}
}