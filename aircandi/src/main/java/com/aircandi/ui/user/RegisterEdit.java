package com.aircandi.ui.user;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

import java.io.IOException;
import java.util.Locale;

public class RegisterEdit extends BaseEntityEdit {

	private EditText mEmail;
	private EditText mPassword;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEntitySchema = Constants.SCHEMA_ENTITY_USER;
		mEmail = (EditText) findViewById(R.id.email);
		mPassword = (EditText) findViewById(R.id.password);

		mPassword.setImeOptions(EditorInfo.IME_ACTION_GO);
		mPassword.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					onAccept();
					return true;
				}
				return false;
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onViewTermsButtonClick(View view) {
		Aircandi.dispatch.route(this, Route.TERMS, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onRegisterButtonClick(View view) {
		onAccept();
	}

	@Override
	public void onAccept() {
		if (validate()) {
			gather();
			update();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void draw() {
		super.draw();
		setActivityTitle(StringManager.getString(R.string.label_register_title));
	}

	@Override
	protected String getLinkType() {
		return null;
	}

	@Override
	protected void gather() {
		super.gather();

		User user = (User) mEntity;
		user.email = mEmail.getText().toString().trim().toLowerCase(Locale.US);
		user.password = mPassword.getText().toString().trim();
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected boolean validate() {
		if (mName.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_fullname)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mEmail.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mPassword.getText().length() < 6) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_password)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (!Utilities.validEmail(mEmail.getText().toString())) {
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

	@Override
	protected void update() {

		Logger.d(this, "Inserting user: " + mEntity.name);

		mTaskService = new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {
					mBusy.showProgress();
				}
				else {
					mBusy.showBusy(BusyAction.Update);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUser");

				Bitmap bitmap = null;
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {
					
					/* Synchronous call to get the bitmap */
					try {
						bitmap = DownloadManager.with(Aircandi.applicationContext)
						                        .load(mEntity.getPhoto().getUri())
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
							bitmap = DownloadManager.with(Aircandi.applicationContext)
							                        .load(mEntity.getPhoto().getUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
							                        .get();

							if (isCancelled()) return null;
						}
						catch (IOException ignore) {}
					}
					catch (IOException ignore) {
						if (isCancelled()) return null;
					}
				}

				ModelResult result = Aircandi.getInstance().getEntityManager().registerUser((User) mEntity
						, (mEntity.photo != null) ? bitmap : null);

				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				mBusy.hideBusy(true);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					/*
					 * We automatically consider the user signed in.
					 */
					final User user = (User) result.data;
					Aircandi.getInstance().setCurrentUser(user);
					result = Aircandi.getInstance().getEntityManager().activateCurrentUser(true);
					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						result = MessagingManager.getInstance().registerInstallWithAircandi();
					}
				}
				return result;
			}

			@Override
			protected void onCancelled(Object response) {
				/*
				 * Stopping Points (interrupt was triggered on background thread)
				 * - When task is pulled from queue (if waiting)
				 * - Between service and s3 calls.
				 * - During service calls assuming okhttp catches the interrupt.
				 * - During image upload to s3 if CancelEvent is sent via bus.
				 */
				mBusy.hideBusy(true);
				UI.showToastNotification(StringManager.getString(R.string.alert_cancelled), Toast.LENGTH_SHORT);
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Logger.i(RegisterEdit.this, "Inserted new user: " + mEntity.name + " (" + mEntity.id + ")");
					UI.showToastNotification(StringManager.getString(R.string.alert_signed_in) + " " + Aircandi.getInstance().getCurrentUser().name,
							Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_USER_SIGNED_IN);
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(RegisterEdit.this, TransitionType.FORM_TO_PAGE);
				}
				else {
					Errors.handleError(RegisterEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected int getLayoutId() {
		return R.layout.register_edit;
	}
}