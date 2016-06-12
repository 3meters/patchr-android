package com.patchr.ui.edit;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.patchr.BuildConfig;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Command;
import com.patchr.objects.PhoneNumber;
import com.patchr.objects.PhotoCategory;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.LobbyScreen;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class ProfileEdit extends BaseEdit {

	private TextView title;
	private EditText email;
	private EditText area;
	public  TextView authIdentifierLabel;
	public  TextView authIdentifier;

	private Button submitDelete;
	private Button submitButton;
	private Button changePasswordButton;
	private Button termsButton;

	private String inputState = State.Editing;
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

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			if (inputState != null && inputState.equals(State.CompleteProfile)) {
				getMenuInflater().inflate(R.menu.menu_signup, menu);
			}
			else if (editing) {
				getMenuInflater().inflate(R.menu.menu_save, menu);
				getMenuInflater().inflate(R.menu.menu_delete, menu);
			}
		}
		else if (editing) {
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
			Patchr.router.route(this, Command.PASSWORD_CHANGE, null, null);
		}
		else if (view.getId() == R.id.terms_button) {
			Patchr.router.route(this, Command.TERMS, null, null);
		}
		else if (view.getId() == R.id.submit_button) {
			submitAction();
		}
		else {
			super.onClick(view);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.inputState = extras.getString(Constants.EXTRA_STATE, State.Editing);
			this.inputEmail = extras.getString(Constants.EXTRA_EMAIL);
			this.inputPassword = extras.getString(Constants.EXTRA_PASSWORD);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // handles name/photo

		title = (TextView) findViewById(R.id.title);
		area = (EditText) findViewById(R.id.area);
		email = (EditText) findViewById(R.id.email);
		authIdentifierLabel = (TextView) findViewById(R.id.auth_identifier_label);
		authIdentifier = (TextView) findViewById(R.id.auth_identifier);
		submitButton = (Button) findViewById(R.id.submit_button);
		termsButton = (Button) findViewById(R.id.terms_button);
		changePasswordButton = (Button) findViewById(R.id.change_password_button);

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			email.setVisibility(View.GONE);
			authIdentifierLabel.setVisibility(View.VISIBLE);
			authIdentifier.setVisibility(View.VISIBLE);
			changePasswordButton.setVisibility(View.GONE);

			if (inputState != null && inputState.equals(State.CompleteProfile)) {
				title.setText("Make your account more personal");
				authIdentifierLabel.setText(R.string.label_auth_identifier_onboarding);
				area.setVisibility(View.GONE);
				submitButton.setVisibility(View.VISIBLE);
				submitButton.setText("Create account");
				termsButton.setVisibility(View.VISIBLE);
			}
			else {
				authIdentifierLabel.setText(R.string.label_auth_identifier);
			}
		}
		else {
			if (inputState != null && inputState.equals(State.Onboarding)) {
				this.entity = User.build();
				((User) this.entity).email = this.inputEmail;
				((User) this.entity).password = this.inputPassword;
				title.setText(R.string.form_title_profile_signup);
				area.setVisibility(View.GONE);
				submitButton.setVisibility(View.VISIBLE);
				termsButton.setVisibility(View.VISIBLE);
				changePasswordButton.setVisibility(View.GONE);
			}

			if (email != null) {
				email.addTextChangedListener(new SimpleTextWatcher() {

					@Override
					public void afterTextChanged(Editable s) {
						if (entity != null) {
							if (!s.toString().equals(((User) entity).email)) {
								if (!firstDraw) {
									dirty = true;
								}
							}
						}
					}
				});
			}
		}

		if (area != null) {
			area.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (entity != null) {
						if (!s.toString().equals(((User) entity).area)) {
							if (!firstDraw) {
								dirty = true;
							}
						}
					}
				}
			});
		}

		name.setImeOptions(EditorInfo.IME_ACTION_GO);
		name.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					submitAction();
					return true;
				}
				return false;
			}
		});
	}

	@Override public void bind() {
		super.bind();

		if (BuildConfig.ACCOUNT_KIT_ENABLED) {
			User user = (User) this.entity;
			if (UserManager.authTypeHint != null) {
				if (UserManager.authTypeHint.equals(LobbyScreen.AuthType.PhoneNumber)) {
					authIdentifier.setText(((PhoneNumber) UserManager.authIdentifierHint).number);
				}
				else {
					authIdentifier.setText((String) UserManager.authIdentifierHint);
				}
			}

			if (inputState == null || !inputState.equals(State.CompleteProfile)) {
				if (this.area != null && !TextUtils.isEmpty(user.area)) {
					this.area.setText(user.area);
				}
			}
		}
		else {
			if (this.inputState != null && this.inputState.equals(State.Onboarding)) {
				UI.setTextView(this.email, this.inputEmail);
				this.email.setEnabled(false);
				this.photoEditWidget.bind(null);
			}
			else {
				User user = (User) entity;
				if (this.area != null && !TextUtils.isEmpty(user.area)) {
					this.area.setText(user.area);
				}
				if (this.email != null && !TextUtils.isEmpty(user.email)) {
					this.email.setText(user.email);
				}
			}
		}
	}

	@Override public void submitAction() {
		if (!processing) {

			if (inputState != null && inputState.equals(State.CompleteProfile)) {
				if (validate()) {
					processing = true;
					((User) this.entity).role = "user";
					update();
				}
			}
			else if (inputState != null && inputState.equals(State.Onboarding)) {
				if (validate()) {
					processing = true;
					register();
				}
			}
			else {                      // Covers both editing and profile completion
				super.submitAction();   // Validates and updates the user (only if dirty)
			}
		}
	}

	@Override protected void gather() {
		super.gather(); // Handles name and description

		User user = (User) entity;
		if (email != null) {
			user.email = Type.emptyAsNull(email.getText().toString().trim());
		}
		if (area != null) {
			user.area = Type.emptyAsNull(area.getText().toString().trim());
		}
	}

	@Override protected boolean afterUpdate() {
		/* So our persisted user is up-to-date. Only called if update call was successful. */
		((User) entity).session = UserManager.currentUser.session;
		UserManager.shared().setCurrentUser((User) entity, false);  // Updates persisted user too
		return true;
	}

	@Override protected boolean validate() {

		gather();
		if (this.entity.name == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_fullname)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	protected void register() {

		Logger.d(this, "Inserting user: " + entity.name);

		taskService = new AsyncTask() {

			@Override protected void onPreExecute() {
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {
					busyPresenter.showHorizontalProgressBar(ProfileEdit.this);
				}
				else {
					busyPresenter.show(BusyPresenter.BusyAction.Update);
				}
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUser");

				Bitmap bitmap = null;
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {

					/* Synchronous call to get the bitmap */
					try {
						bitmap = Picasso.with(Patchr.applicationContext)
								.load(entity.photo.uri(PhotoCategory.STANDARD))
								.centerInside()
								.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
								.get();

						if (isCancelled()) return null;
					}
					catch (OutOfMemoryError error) {
						/*
						 * We make attempt to recover by giving the vm another chance to
						 * garbage collect plus reduce the image size in memory by 75%.
						 */
						System.gc();
						try {
							bitmap = Picasso.with(Patchr.applicationContext)
									.load(entity.photo.uri(PhotoCategory.STANDARD))
									.centerInside()
									.resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
									.get();

							if (isCancelled()) return null;
						}
						catch (IOException ignore) {}
					}
					catch (IOException ignore) {
						/*
						 * This is where we are ignoring exceptions like our reset problem with picasso. This
						 * can happen pulling an image from the network or from a local file.
						 */
						Reporting.breadcrumb("Picasso failed to load bitmap");
						if (isCancelled()) return null;
					}

					if (bitmap == null) {
						ModelResult result = new ModelResult();
						result.serviceResponse.responseCode = NetworkManager.ResponseCode.FAILED;
						result.serviceResponse.errorResponse = new Errors.ErrorResponse(Errors.ResponseType.TOAST, StringManager.getString(R.string.error_image_unusable));
						result.serviceResponse.errorResponse.clearPhoto = true;
						busyPresenter.hide(true);
						return result;
					}
				}

				ModelResult result = DataController.getInstance().registerUser((User) entity
						, (entity.photo != null) ? bitmap : null, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				return !isCancelled() ? result : null;
			}

			@Override protected void onCancelled(Object response) {
				/*
				 * Stopping Points (interrupt was triggered on background thread)
				 * - When task is pulled from queue (if waiting)
				 * - Between service and s3 calls.
				 * - During service calls assuming okhttp catches the interrupt.
				 * - During image upload to s3 if CancelEvent is sent via bus.
				 */
				busyPresenter.hide(true);
				UI.toast(StringManager.getString(R.string.alert_cancelled));
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {

					/* We automatically consider the user signed in. */
					final User user = (User) result.data;
					UserManager.shared().setCurrentUser(user, false);

					Reporting.track(AnalyticsCategory.EDIT, "Created User and Logged In");
					Logger.i(ProfileEdit.this, "Inserted new user: " + entity.name + " (" + entity.id + ")");
					UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + user.name);

					setResult(Constants.RESULT_USER_LOGGED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(ProfileEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(ProfileEdit.this, result.serviceResponse);
					if (result.serviceResponse.errorResponse != null) {
						if (result.serviceResponse.errorResponse.clearPhoto) {
							entity.photo = null;
							bindPhoto();
						}
					}
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void deleteUser() {

		final String userName = entity.name;

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_deleting_user, ProfileEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				return DataController.getInstance().deleteUser(entity.id, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				processing = false;
				busyPresenter.hide(true);

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {

					Reporting.track(AnalyticsCategory.EDIT, "Deleted User");
					Logger.i(this, "Deleted user: " + entity.id);
					UserManager.shared().setCurrentUser(null, false);
					UserManager.shared().discardAuthHints();
					Patchr.router.route(Patchr.applicationContext, Command.LOBBY, null, null);
					UI.toast(String.format(StringManager.getString(R.string.alert_user_deleted), userName));
					finish();
				}
				else {
					Errors.handleError(ProfileEdit.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected String getEntitySchema() {
		return Constants.SCHEMA_ENTITY_USER;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_profile;
	}

	@Override public void confirmDelete() {

		final EditText textConfirm = new EditText(this);
		textConfirm.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				submitDelete.setEnabled(s.toString().equals("YES"));
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder((this));

		int padding = UI.getRawPixelsForDisplayPixels(20f);

		builder.setView(textConfirm, padding, 0, padding, 0);
		builder.setTitle(R.string.alert_delete_account_title);
		builder.setMessage(R.string.alert_delete_account_message);
		builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface dialog, int which) {
				deleteUser();
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface dialog, int which) { /* do nothing */}
		});

		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		submitDelete = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		submitDelete.setEnabled(false);
	}
}