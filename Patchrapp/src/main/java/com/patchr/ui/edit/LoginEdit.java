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
import com.patchr.objects.Command;
import com.patchr.objects.TransitionType;
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

	private ClearableEditText email;
	private PasswordEditText  password;
	private TextView          title;
	private View              forgotPasswordButton;
	private View              loginButton;
	private String onboardMode = OnboardMode.Login;
	protected Subscription subscription;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (onboardMode.equals(OnboardMode.Signup)) {
			getMenuInflater().inflate(R.menu.menu_next, menu);
		}
		else {
			getMenuInflater().inflate(R.menu.menu_login, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else if (item.getItemId() == R.id.login) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onClick(View view) {
		if (view.getId() == R.id.submit_button) {
			submitAction();
		}
		else if (view.getId() == R.id.forgot_password_button) {
			Patchr.router.route(this, Command.PASSWORD_RESET, null, null);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.onboardMode = extras.getString(Constants.EXTRA_ONBOARD_MODE, OnboardMode.Login);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		title = (TextView) findViewById(R.id.title);
		loginButton = findViewById(R.id.submit_button);
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

		if (onboardMode.equals(OnboardMode.Signup)) {
			title.setText(R.string.form_title_login_signup);
			loginButton.setVisibility(View.GONE);
			forgotPasswordButton.setVisibility(View.GONE);
		}
	}

	@Override public void bind() {
		if (UserManager.authIdentifierHint != null) {
			this.email.setText((String) UserManager.authIdentifierHint);
			password.requestFocus();
		}
	}

	@Override protected boolean validate() {

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
		if (validate()) {
			if (onboardMode.equals(OnboardMode.Login)) {
				login();
			}
			else if (onboardMode.equals(OnboardMode.Signup)) {
				validateEmail();
			}
		}
	}

	private void login() {

		final String email = this.email.getText().toString().toLowerCase(Locale.US);
		final String password = this.password.getText().toString();

		this.subscription = UserManager.shared().login(email, password)
			.doOnSubscribe(() -> busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_logging_in, LoginEdit.this))
			.doOnTerminate(() -> busyController.hide(true))  // Before either onCompleted or onError
			.doOnError(throwable -> {
				Logger.w(this, "Service call failed");      // onCompleted will not be called
			})
			.subscribe(
				response -> {
					if (response.isSuccessful()) {
						Logger.i(this, "User signed in: " + UserManager.currentUser.name);
						UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
						setResult(Constants.RESULT_USER_LOGGED_IN);
						finish();
						AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
					}
					else {
						/* Cherry pick validation errors */
						if (response.error != null
							&& (response.error.code.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_FOUND
							|| response.error.code.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS)) {
							Dialogs.alert(R.string.error_signin_failed, LoginEdit.this);
						}
						else {
							Errors.handleError(LoginEdit.this, response.error);
						}
					}
				});
	}

	private void validateEmail() {

		final String email = this.email.getText().toString().toLowerCase(Locale.US);

		this.subscription = RestClient.getInstance().findByEmail(email)
			.doOnSubscribe(() -> busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_reset_verify, LoginEdit.this))
			.doOnTerminate(() -> busyController.hide(true))  // Before either onCompleted or onError
			.doOnError(throwable -> {
				Logger.w(this, "Service call failed");      // onCompleted will not be called
			})
			.subscribe(
				response -> {
					busyController.hide(true);
					if (response.isSuccessful()) {
						if (response.count.intValue() == 0) {
							Bundle extras = new Bundle();
							extras.putString(Constants.EXTRA_STATE, State.Onboarding);
							extras.putString(Constants.EXTRA_EMAIL, email);
							extras.putString(Constants.EXTRA_PASSWORD, password.getText().toString());
							Patchr.router.route(this, Command.SIGNUP, null, extras);
						}
						else {
							UI.toast("Email has already been used.");
						}
					}
				});
	}

	public static class OnboardMode {
		public static String Login  = "login";
		public static String Signup = "signup";
	}
}