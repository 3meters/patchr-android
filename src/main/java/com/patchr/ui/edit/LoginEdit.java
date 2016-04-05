package com.patchr.ui.edit;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Command;
import com.patchr.objects.ServiceData;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.widgets.ClearableEditText;
import com.patchr.ui.widgets.PasswordEditText;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Locale;

public class LoginEdit extends BaseEdit {

	private ClearableEditText email;
	private PasswordEditText  password;
	private TextView          title;
	private View              forgotPasswordButton;
	private View              loginButton;
	private String onboardMode = OnboardMode.Login;

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
		password.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					login();
					return true;
				}
				return false;
			}
		});

		if (onboardMode.equals(OnboardMode.Signup)) {
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

		if (requestCode == Constants.ACTIVITY_LOGIN
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

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_logging_in, LoginEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLogin");
				ModelResult result = DataController.getInstance().signin(email, password, LoginEdit.class.getSimpleName(), NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
					didLogin();
				}
				else {
					Errors.handleError(LoginEdit.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void validateEmail() {

		final String email = this.email.getText().toString().toLowerCase(Locale.US);

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_reset_verify, LoginEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncEmailVerify");
				ModelResult result = new ModelResult();
				Response response = NetworkManager.getInstance().get(Constants.URL_PROXIBASE_SERVICE_FIND + "/users", String.format("q[email]=%1$s", email));
				if (!response.isSuccessful()) {
					result.serviceResponse.responseCode = ResponseCode.FAILED;
				}
				try {
					String jsonResponse = response.body().string();
					final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
					result.data = serviceData;
				}
				catch (IOException e) {
					UI.toast(e.toString());
				}
				return result;
			}

			@Override protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Reporting.sendEvent(Reporting.TrackerCategory.USER, "email_validate", null, 0);
					ServiceData serviceData = (ServiceData) result.data;
					if (serviceData.count == 0) {
						didValidate();
					}
					else {
						UI.toast("Email has already been used.");
					}
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void didLogin() {
		setResult(Constants.RESULT_USER_LOGGED_IN);
		finish();
		AnimationManager.doOverridePendingTransition(LoginEdit.this, TransitionType.FORM_BACK);
	}

	private void didValidate() {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_STATE, State.Onboarding);
		extras.putString(Constants.EXTRA_EMAIL, email.getText().toString());
		extras.putString(Constants.EXTRA_PASSWORD, password.getText().toString());
		Patchr.router.route(this, Command.SIGNUP, null, extras);
	}

	public static class OnboardMode {
		public static String Login  = "login";
		public static String Signup = "signup";
	}
}