package com.patchr.ui.edit;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.ResponseCode;
import com.patchr.objects.ServiceData;
import com.patchr.objects.User;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.Locale;

public class ResetEdit extends BaseEdit {

	private User user;

	private String  inputToken;
	private String  inputUserName;
	private String  inputUserPhoto;
	private boolean resetActive;

	private boolean emailValidated;
	private boolean resetRequested;

	private TextView    title;
	private EditText    emailField;
	private EditText    passwordField;
	private ImageWidget userPhoto;
	private TextView    userName;
	private Button      submitButton;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_submit, menu);
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

	public void onClick(View view) {
		submitAction();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			inputToken = extras.getString(Constants.EXTRA_RESET_TOKEN);
			inputUserName = extras.getString(Constants.EXTRA_RESET_USER_NAME);
			inputUserPhoto = extras.getString(Constants.EXTRA_RESET_USER_PHOTO);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		if (inputToken != null) {
			resetActive = true;
		}

		actionBarTitle.setText(R.string.screen_title_reset_edit);

		title = (TextView) findViewById(R.id.title);
		emailField = (EditText) findViewById(R.id.email);
		passwordField = (EditText) findViewById(R.id.password);
		userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		userName = (TextView) findViewById(R.id.user_name);
		submitButton = (Button) findViewById(R.id.submit_button);

		passwordField.setVisibility(View.GONE);
		emailField.setVisibility(View.GONE);
		userName.setVisibility(View.GONE);
		userPhoto.setVisibility(View.GONE);

		if (!resetActive) {
			title.setText(R.string.form_title_reset_validate_email);
			title.setMinLines(3);
			emailField.setVisibility(View.VISIBLE);
			emailField.setImeOptions(EditorInfo.IME_ACTION_GO);
			emailField.setOnEditorActionListener(new TextView.OnEditorActionListener() {

				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_GO) {
						submitAction();
						return true;
					}
					return false;
				}
			});
			submitButton.setText("Verify");
		}
		else {
			title.setText(R.string.form_title_reset_reset_password);
			title.setMinLines(1);
			userName.setVisibility(View.VISIBLE);
			userPhoto.setVisibility(View.VISIBLE);
			passwordField.setVisibility(View.VISIBLE);
			passwordField.setImeOptions(EditorInfo.IME_ACTION_GO);
			passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {

				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_GO) {
						submitAction();
						return true;
					}
					return false;
				}
			});
			submitButton.setText("Reset");
		}
	}

	@Override public void bind() {

		if (resetActive) {
			if (inputUserPhoto != null) {
				Photo photo = new Photo();
				photo.prefix = inputUserPhoto;
				photo.source = Photo.PhotoSource.aircandi_images;
				userPhoto.setImageWithPhoto(photo);
			}
			else if (inputUserName != null) {
				userPhoto.setImageWithText(inputUserName, true);
			}
			userName.setText(inputUserName);
		}
		else {
			if (UserManager.authIdentifierHint != null) {
				this.emailField.setText((String) UserManager.authIdentifierHint);
			}
		}
	}

	@Override protected boolean validate() {

		if (!resetActive) {
			if (emailField.getText().length() == 0) {
				Dialogs.alert(R.string.error_reset_missing_email, this);
				return false;
			}

			if (!Utils.validEmail(emailField.getText().toString())) {
				Dialogs.alert(R.string.error_invalid_email, this);
				return false;
			}
		}
		else {
			if (passwordField.getText().length() < 6) {
				Dialogs.alert(R.string.error_reset_missing_password, this);
				return false;
			}
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_reset;
	}

	public void submitAction() {

		if (this.processing) return;

		if (validate()) {
			if (!resetActive) {
				if (!emailValidated) {
					validateEmail();
				}
				else if (!resetRequested) {
					resetEmail();
				}
				else {
					cancelAction(true);
				}
			}
			else {
				resetPassword();
			}
		}
	}

	protected void validateEmail() {
		if (this.processing) return;
		this.processing = true;

		Logger.d(this, "Verifying email for password reset");

		final String email = this.emailField.getText().toString().trim().toLowerCase(Locale.US);

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_reset_verify, ResetEdit.this);
				UI.hideSoftInput(ResetEdit.this.emailField);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRequestPasswordReset");
				ModelResult result = DataController.getInstance().validateEmail(email, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				processing = false;
				busyController.hide(false);

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					Errors.handleError(ResetEdit.this, result.serviceResponse);
				}
				else {
					ServiceData serviceData = (ServiceData) result.serviceResponse.data;
					if (serviceData.count.intValue() != 0) {
						title.setText(R.string.form_title_reset_request_reset);
						submitButton.setText("Send password reset email");
						emailField.setEnabled(false);
						emailValidated = true;
					}
					else {
						Dialogs.alert("Email address not found.", ResetEdit.this);
					}
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void resetEmail() {

		if (this.processing) return;
		this.processing = true;

		Logger.d(this, "Requesting password reset email");

		final String email = this.emailField.getText().toString().trim().toLowerCase(Locale.US);

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_reset_verify, ResetEdit.this);
				UI.hideSoftInput(ResetEdit.this.emailField);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRequestPasswordReset");
				ModelResult result = DataController.getInstance().requestPasswordReset(email, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				processing = false;
				busyController.hide(false);

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					Errors.handleError(ResetEdit.this, result.serviceResponse);
				}
				else {
					resetRequested = true;
					title.setText("An email has been sent to your account\'s email address. Please check your email to continue.");
					submitButton.setText("Finished");
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void resetPassword() {

		if (this.processing) return;
		this.processing = true;

		Logger.d(this, "Resetting password for: " + inputUserName);

		final String password = this.passwordField.getText().toString();

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_logging_in, ResetEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncResetPassword");
				ModelResult result = DataController.getInstance().resetPassword(password, inputToken, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				processing = false;
				busyController.hide(true);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Reporting.track(AnalyticsCategory.EDIT, "Reset Password and Logged In");
					Logger.i(this, "Password reset and user signed in: " + UserManager.currentUser.name);
					navigateToMain();
				}
				else {
					if (result.serviceResponse.statusCodeService != null
						&& result.serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
						Dialogs.alert(R.string.alert_reset_expired, ResetEdit.this, new DialogInterface.OnClickListener() {
							@Override public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Patchr.router.route(ResetEdit.this, Command.PASSWORD_RESET, null, null);
								finish();
							}
						});
					}
					else {
						Errors.handleError(ResetEdit.this, result.serviceResponse);
					}
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void navigateToMain() {
		UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
		Patchr.router.route(this, Command.HOME, null, null);
		finish();
	}
}