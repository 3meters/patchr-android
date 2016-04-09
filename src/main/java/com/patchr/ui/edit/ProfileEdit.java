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
import com.patchr.objects.Command;
import com.patchr.objects.PhotoCategory;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
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

	private EditText area;
	private EditText email;
	private TextView title;
	private Button   submitDelete;
	private Button   submitButton;
	private Button   changePasswordButton;
	private Button   termsButton;

	private String inputState;
	private String inputEmail;
	private String inputPassword;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
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

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (editing) {
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
		submitButton = (Button) findViewById(R.id.submit_button);
		termsButton = (Button) findViewById(R.id.terms_button);
		changePasswordButton = (Button) findViewById(R.id.change_password_button);

		if (inputState.equals(State.Onboarding)) {
			this.entity = User.build();
			((User)this.entity).email = this.inputEmail;
			((User)this.entity).password = this.inputPassword;
			title.setText(R.string.form_title_profile_signup);
			area.setVisibility(View.GONE);
			submitButton.setVisibility(View.VISIBLE);
			termsButton.setVisibility(View.VISIBLE);
			changePasswordButton.setVisibility(View.GONE);
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

		if (inputState.equals(State.Onboarding)) {
			UI.setTextView(email, inputEmail);
			email.setEnabled(false);
			photoEditWidget.bind(null);
		}
		else {
			User user = (User) entity;
			if (area != null && !TextUtils.isEmpty(user.area)) {
				area.setText(user.area);
			}
			if (email != null && !TextUtils.isEmpty(user.email)) {
				email.setText(user.email);
			}
		}
	}

	@Override public void submitAction() {
		if (!inputState.equals(State.Onboarding)) {
			super.submitAction();
		}
		else {
			if (processing) return;
			processing = true;

			if (validate()) {
				register();
			}
			else {
				processing = false;
			}
		}
	}

	@Override protected void gather() {
		super.gather();

		User user = (User) entity;
		if (email != null) {
			user.email = Type.emptyAsNull(email.getText().toString().trim());
		}
		if (area != null) {
			user.area = Type.emptyAsNull(area.getText().toString().trim());
		}
	}

	@Override protected boolean validate() {

		gather();
		User user = (User) entity;
		if (user.name == null) {
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
					busyPresenter.showProgressDialog(ProfileEdit.this);
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
						Reporting.logException(new IOException("Picasso failed to load bitmap", ignore));
						if (isCancelled()) return null;
					}
				}

				ModelResult result = DataController.getInstance().registerUser((User) entity
						, (entity.photo != null) ? bitmap : null, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				busyPresenter.hide(true);

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					/*
					 * We automatically consider the user signed in.
					 */
					final User user = (User) result.data;
					UserManager.shared().setCurrentUser(user, true);
				}
				return result;
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

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {

					Logger.i(ProfileEdit.this, "Inserted new user: " + entity.name + " (" + entity.id + ")");
					UI.toast(StringManager.getString(R.string.alert_logged_in) + " " + UserManager.currentUser.name
					);
					setResult(Constants.RESULT_USER_LOGGED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(ProfileEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(ProfileEdit.this, result.serviceResponse);
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
					Logger.i(this, "Deleted user: " + entity.id);
					UserManager.shared().setCurrentUser(null, false);
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