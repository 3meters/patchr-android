package com.patchr.ui.edit;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Command;
import com.patchr.objects.PhotoCategory;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Locale;

public class RegisterEdit extends BaseEdit {

	private EditText email;
	private EditText password;
	private CheckBox passwordUnmask;
	
	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getId() == R.id.terms_button) {
			Patchr.router.route(this, Command.TERMS, null, null);
		}
		else if (view.getId() == R.id.signup_button) {
			submitAction();
		}
	}

	@Override public void submitAction() {

		if (processing) return;
		processing = true;

		if (validate()) {
			gather();
			register();
		}
		else {
			processing = false;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		entitySchema = Constants.SCHEMA_ENTITY_USER;
		email = (EditText) findViewById(R.id.email);
		password = (EditText) findViewById(R.id.password);
		passwordUnmask = (CheckBox) findViewById(R.id.chk_unmask);

		passwordUnmask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

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
					submitAction();
					return true;
				}
				return false;
			}
		});
	}

	@Override protected void gather() {
		super.gather();

		User user = (User) entity;
		user.email = email.getText().toString().trim().toLowerCase(Locale.US);
		user.password = password.getText().toString().trim();
	}

	@Override protected boolean validate() {

		gather();
		if (name.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_fullname)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (email.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (password.getText().length() < 6) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password)
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

	@Override protected int getLayoutId() {
		return R.layout.edit_register;
	}

	protected void register() {

		Logger.d(this, "Inserting user: " + entity.name);

		taskService = new AsyncTask() {

			@Override protected void onPreExecute() {
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {
					busyPresenter.showProgressDialog(RegisterEdit.this);
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

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
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
				UI.showToastNotification(StringManager.getString(R.string.alert_cancelled), Toast.LENGTH_SHORT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Logger.i(RegisterEdit.this, "Inserted new user: " + entity.name + " (" + entity.id + ")");
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in) + " " + UserManager.currentUser.name,
							Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_USER_SIGNED_IN);
					finish();
					AnimationManager.doOverridePendingTransition(RegisterEdit.this, TransitionType.FORM_BACK);
				}
				else {
					Errors.handleError(RegisterEdit.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}