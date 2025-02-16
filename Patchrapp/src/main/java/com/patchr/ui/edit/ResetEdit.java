package com.patchr.ui.edit;

import android.content.Intent;
import android.os.Bundle;
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
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.components.ReportingManager;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.Locale;

public class ResetEdit extends BaseEdit {

	private String  inputToken;
	private String  inputUserName;
	private String  inputUserPhoto;
	private boolean resetActive;

	private boolean emailValidated;
	private boolean resetRequested;

	private TextView    titleView;
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

		titleView = (TextView) findViewById(R.id.title);
		emailField = (EditText) findViewById(R.id.email);
		passwordField = (EditText) findViewById(R.id.password);
		userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		userName = (TextView) findViewById(R.id.user_name);
		submitButton = (Button) findViewById(R.id.signup_button);

		passwordField.setVisibility(View.GONE);
		emailField.setVisibility(View.GONE);
		userName.setVisibility(View.GONE);
		userPhoto.setVisibility(View.GONE);

		if (!resetActive) {
			titleView.setText(R.string.form_title_reset_validate_email);
			titleView.setMinLines(3);
			emailField.setVisibility(View.VISIBLE);
			emailField.setImeOptions(EditorInfo.IME_ACTION_GO);
			emailField.setOnEditorActionListener((v, actionId, event) -> {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					submitAction();
					return true;
				}
				return false;
			});
			submitButton.setText("Verify");
		}
		else {
			titleView.setText(R.string.form_title_reset_reset_password);
			titleView.setMinLines(1);
			userName.setVisibility(View.VISIBLE);
			userPhoto.setVisibility(View.VISIBLE);
			passwordField.setVisibility(View.VISIBLE);
			passwordField.setImeOptions(EditorInfo.IME_ACTION_GO);
			passwordField.setOnEditorActionListener((v, actionId, event) -> {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					submitAction();
					return true;
				}
				return false;
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
				userPhoto.setImageWithPhoto(photo, null, null);
			}
			else if (inputUserName != null) {
				userPhoto.setImageWithText(inputUserName, true);
			}
			userName.setText(inputUserName);
		}
		else {
			final String email = Patchr.settings.getString(StringManager.getString(R.string.setting_last_email), null);
			if (email != null) {
				emailField.setText(email);
			}
		}
	}

	@Override protected boolean isValid() {

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

		if (isValid()) {
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

		if (!this.processing) {
			this.processing = true;

			Logger.d(this, "Verifying email for password reset");
			final String email = this.emailField.getText().toString().trim().toLowerCase(Locale.US);
			busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_reset_verify, ResetEdit.this);
			UI.hideSoftInput(ResetEdit.this.emailField);

			subscription = RestClient.getInstance().validEmail(email)
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);

						if (response.data != null && response.count.intValue() != 0) {
							titleView.setText(R.string.form_title_reset_request_reset);
							submitButton.setText("Send password reset email");
							emailField.setEnabled(false);
							emailValidated = true;
						}
						else {
							Dialogs.alert("Email address not found.", ResetEdit.this);
						}
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(this, error);
					});
		}
	}

	protected void resetEmail() {

		if (!this.processing) {
			this.processing = true;

			Logger.d(this, "Requesting password reset email");
			final String email = this.emailField.getText().toString().trim().toLowerCase(Locale.US);
			busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_reset_verify, ResetEdit.this);
			UI.hideSoftInput(ResetEdit.this.emailField);

			subscription = RestClient.getInstance().requestPasswordReset(email)
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);
						resetRequested = true;
						titleView.setText("An email has been sent to your account\'s email address. Please check your email to continue.");
						submitButton.setText("Finished");
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(this, error);
					});
		}
	}

	protected void resetPassword() {

		if (!this.processing) {
			this.processing = true;

			Logger.d(this, "Resetting password for: " + inputUserName);
			final String password = this.passwordField.getText().toString();
			busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_logging_in, ResetEdit.this);
			UI.hideSoftInput(ResetEdit.this.emailField);

			subscription = RestClient.getInstance().resetPassword(password, inputToken)
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);

						if (response.isSuccessful()) {
							ReportingManager.getInstance().track(AnalyticsCategory.EDIT, "Reset Password and Logged In");
							Logger.i(this, "Password reset and user signed in: " + UserManager.currentUser.name);
							UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name);
							UI.routeHome(this);
							finish();
						}
						else {
							if (response.serviceCode.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
								Dialogs.alert(R.string.alert_reset_expired, ResetEdit.this, (dlg, which) -> {
									dlg.dismiss();
									Intent intent = new Intent(this, ResetEdit.class);
									startActivityForResult(intent, Constants.ACTIVITY_RESET_AND_SIGNIN);
									AnimationManager.doOverridePendingTransition(ResetEdit.this, TransitionType.FORM_TO);
									finish();
								});
							}
							else {
								Logger.w(this, response.serviceMessage);
							}
						}
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(this, error);
					});
		}
	}
}