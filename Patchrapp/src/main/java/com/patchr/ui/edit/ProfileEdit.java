package com.patchr.ui.edit;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
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
import com.patchr.model.RealmEntity;
import com.patchr.objects.Session;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;

public class ProfileEdit extends BaseEdit {

	private EditText emailField;
	private EditText areaField;
	public  TextView authIdentifierLabel;
	public  TextView authIdentifier;
	private Button   deleteButton;

	private String inputEmail;
	private String inputPassword;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (inputState.equals(State.Signup)) {
			getMenuInflater().inflate(R.menu.menu_join, menu);
		}
		else if (inputState.equals(State.Editing)) {
			getMenuInflater().inflate(R.menu.menu_save, menu);
			getMenuInflater().inflate(R.menu.menu_delete, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.delete) {
			confirmDelete();
		}
		else if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onClick(View view) {
		if (view.getId() == R.id.change_password_button) {
			Intent intent = new Intent(this, PasswordEdit.class);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
		else if (view.getId() == R.id.terms_button) {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
		else if (view.getId() == R.id.signup_button) {
			submitAction();
		}
		else {
			super.onClick(view);
		}
	}

	@Override public void submitAction() {

		if (!isValid()) return;
		if (!processing) {
			processing = true;
			SimpleMap parameters = new SimpleMap();
			gather(parameters);
			post(parameters);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			inputEmail = extras.getString(Constants.EXTRA_EMAIL);
			inputPassword = extras.getString(Constants.EXTRA_PASSWORD);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // handles name/photo

		entitySchema = Constants.SCHEMA_ENTITY_USER;
		TextView titleView = (TextView) findViewById(R.id.title);
		areaField = (EditText) findViewById(R.id.area);
		emailField = (EditText) findViewById(R.id.email);
		authIdentifierLabel = (TextView) findViewById(R.id.auth_identifier_label);
		authIdentifier = (TextView) findViewById(R.id.auth_identifier);
		Button submitButton = (Button) findViewById(R.id.signup_button);
		Button termsButton = (Button) findViewById(R.id.terms_button);
		Button changePasswordButton = (Button) findViewById(R.id.change_password_button);

		if (inputState != null && inputState.equals(State.Signup)) {
			entity = new RealmEntity();
			entity.email = inputEmail;
			titleView.setText(R.string.form_title_profile_signup);
			areaField.setVisibility(View.GONE);
			submitButton.setVisibility(View.VISIBLE);
			termsButton.setVisibility(View.VISIBLE);
			changePasswordButton.setVisibility(View.GONE);
		}

		nameField.setImeOptions(EditorInfo.IME_ACTION_GO);
		nameField.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_GO) {
				submitAction();
				return true;
			}
			return false;
		});
	}

	@Override public void bind() {
		super.bind();

		if (this.inputState != null && this.inputState.equals(State.Signup)) {
			UI.setTextView(this.emailField, this.inputEmail);
			this.emailField.setEnabled(false);
			this.photoEditWidget.bind(null);
		}
		else {
			if (this.areaField != null && !TextUtils.isEmpty(entity.area)) {
				this.areaField.setText(entity.area);
			}
			if (this.emailField != null && !TextUtils.isEmpty(entity.email)) {
				this.emailField.setText(entity.email);
			}
		}
	}

	@Override protected void gather(SimpleMap parameters) {
		super.gather(parameters); // Handles name, description, photo

		if (inputState.equals(State.Signup)) {
			if (emailField != null) {
				parameters.put("email", Type.emptyAsNull(emailField.getText().toString().trim()));
			}
			if (areaField != null) {
				parameters.put("area", Type.emptyAsNull(areaField.getText().toString().trim()));
			}
			parameters.put("password", inputPassword);
		}
		else {
			if (emailField != null && !emailField.getText().toString().equals(entity.email)) {
				parameters.put("email", Type.emptyAsNull(emailField.getText().toString().trim()));
			}
			if (areaField != null && !areaField.getText().toString().equals(entity.area)) {
				parameters.put("area", Type.emptyAsNull(areaField.getText().toString().trim()));
			}
		}
	}

	protected void post(SimpleMap data) {

		String path = entity == null ? "data/users" : String.format("data/users/%1$s", entity.id);
		busyController.show(BusyController.BusyAction.ActionWithMessage, insertProgressResId, ProfileEdit.this);

		if (data.containsKey("photo")) {
			busyController.showHorizontalProgressBar(ProfileEdit.this);
		}
		else {
			busyController.show(BusyController.BusyAction.Update);
		}

		AsyncTask.execute(() -> {

			if (data.containsKey("photo")) {
				Photo photo = Photo.setPropertiesFromMap(new Photo(), (SimpleMap) data.get("photo"));
				if (photo != null) {
					Photo photoFinal = postPhotoToS3(photo);
					data.put("photo", photoFinal);
				}
			}

			if (inputState.equals(State.Signup)) {
				subscription = RestClient.getInstance().signup(data)
					.subscribe(
						response -> {
							processing = false;
							busyController.hide(true);

							/* We automatically consider the user signed in. */
							final RealmEntity user = response.user;
							final Session session = response.session;
							UserManager.shared().setCurrentUser(user, session);

							Reporting.track(AnalyticsCategory.EDIT, "Created User and Logged In");
							Logger.i(ProfileEdit.this, "Inserted new user: " + entity.name + " (" + entity.id + ")");
							UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + user.name);

							setResult(Constants.RESULT_USER_LOGGED_IN);
							finish();
							AnimationManager.doOverridePendingTransition(ProfileEdit.this, TransitionType.FORM_BACK);
						},
						error -> {
							processing = false;
							busyController.hide(true);
							Errors.handleError(this, error);
						});
			}
			else {
				subscription = RestClient.getInstance().postEntity(path, data)
					.subscribe(
						response -> {
							processing = false;
							busyController.hide(true);
							final RealmEntity user = response.data.get(0);
							UserManager.shared().setCurrentUser(user, UserManager.currentSession);  // Retain current session
							finish();
							AnimationManager.doOverridePendingTransition(ProfileEdit.this, TransitionType.FORM_BACK);
						},
						error -> {
							processing = false;
							busyController.hide(true);
							Errors.handleError(this, error);
						});
			}
		});
	}

	@Override protected void delete() {

		final String userName = entity.name;

		busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_deleting_user, ProfileEdit.this);
		String path = String.format("user/%1$s", entityId);
		SimpleMap parameters = new SimpleMap();
		parameters.put("erase", true);

		subscription = RestClient.getInstance().deleteEntity(path, parameters, entityId)
			.subscribe(
				response -> {
					processing = false;
					busyController.hide(true);
					Reporting.track(AnalyticsCategory.EDIT, "Deleted User");
					Logger.i(this, "Deleted user: " + entityId);
					UserManager.shared().setCurrentUser(null, null);

					/* Clear cached email */
					SharedPreferences.Editor editor = Patchr.settings.edit();
					editor.putString(StringManager.getString(R.string.setting_last_email), null);
					editor.apply();

					UI.routeLobby(Patchr.applicationContext);
					UI.toast(String.format(StringManager.getString(R.string.alert_user_deleted), userName));
					finish();
				},
				error -> {
					processing = false;
					busyController.hide(true);
					Errors.handleError(this, error);
				});
	}

	@Override public void confirmDelete() {

		final EditText textConfirm = new EditText(this);

		textConfirm.addTextChangedListener(new SimpleTextWatcher() {
			@Override public void afterTextChanged(Editable s) {
				deleteButton.setEnabled(s.toString().equals("YES"));
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder((this));

		int padding = UI.getRawPixelsForDisplayPixels(20f);

		//noinspection deprecation
		builder.setView(textConfirm, padding, 0, padding, 0);
		builder.setTitle(R.string.alert_delete_account_title);
		builder.setMessage(R.string.alert_delete_account_message);
		builder.setPositiveButton("Delete", (dialog, which) -> {
			if (!processing) {
				processing = true;
				delete();
			}
		});

		builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
			/* do nothing */
		});

		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		deleteButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		deleteButton.setEnabled(false);
	}

	@Override protected boolean isValid() {

		if (nameField == null) {
			Dialogs.alert(R.string.error_missing_fullname, this);
			return false;
		}

		if (inputState.equals(State.Editing)) {
			if (emailField == null) {
				Dialogs.alert(R.string.error_missing_email, this);
				return false;
			}
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_profile;
	}
}