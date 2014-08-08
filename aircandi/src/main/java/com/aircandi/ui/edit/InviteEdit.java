package com.aircandi.ui.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

public class InviteEdit extends BaseEntityEdit {

	private AirEditText mEmail;
	private static final int CONTACT_PICKER_RESULT = 1001;

	@Override
	public void initialize(Bundle savedInstanceState) {
		/*
		 * Feedback are not really an entity type so we have to handle
		 * all the expected initialization.
		 */
		mDescription = (AirEditText) findViewById(R.id.description);
		mEmail = (AirEditText) findViewById(R.id.email);

		String lastMessage = Aircandi.settings.getString(StringManager.getString(R.string.setting_invite_message_last), null);
		if (lastMessage != null && !lastMessage.equals("")) {
			mDescription.setText(lastMessage);
		}
		else {
			mDescription.setText(StringManager.getString(R.string.invite_message_default));
		}

		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = s.toString() != null || !s.toString().equals("");
				}
			});
		}

		if (mEmail != null) {
			mEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = s.toString() != null || !s.toString().equals("");
				}
			});
		}
	}

	@Override
	public void draw() {
		((UserView) findViewById(R.id.created_by)).databind(Aircandi.getInstance().getCurrentUser(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSearchButtonClick(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK, Email.CONTENT_URI);
		startActivityForResult(intent, CONTACT_PICKER_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_CANCELED) {

			switch (requestCode) {

				case CONTACT_PICKER_RESULT:

					Cursor cursor = null;
					String email = null;
					try {
						Uri uriContact = data.getData();
						Logger.v(this, "Got a contact uri: " + uriContact.toString());

						cursor = getContentResolver().query(uriContact, null, null, null, null);

						if (cursor.moveToFirst()) {
							email = cursor.getString(cursor.getColumnIndex("data1"));  // Email.ADDRESS = "data1"
							Logger.v(this, "Got email: " + email);
						}
						else {
							Logger.w(this, "No results");
						}
					}
					catch (Exception e) {
						Logger.e(this, "Failed to get email data", e);
					}
					finally {
						if (cursor != null) {
							cursor.close();
						}
						if (TextUtils.isEmpty(email)) {
							UI.showToastNotification("No email found for contact.", Toast.LENGTH_LONG);
						}
						else {
							String currentEmail = mEmail.getEditableText().toString().trim();
							Boolean existingEmail = (currentEmail != null && !currentEmail.equals(""));
							mEmail.getEditableText().append(existingEmail ? ", " + email : email);
						}
					}
					break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (validate()) {
			super.onAccept();
		}
	}

	@Override
	protected String getLinkType() {
		return null;
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
		if (mEmail.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_invite_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected void gather() {
		/*
		 * Do nothing
		 */
	}

	@Override
	protected void insert() {
		Logger.i(this, "Send invite");

		final String email = mEmail.getEditableText().toString();
		final String invitor = Aircandi.getInstance().getCurrentUser().name;
		final String message = mDescription.getEditableText().toString();
		final List<String> emails = new ArrayList(Arrays.asList(email.split("\\s*,\\s*")));

		/* Stash message so we can restore it in the FUTURE */
		Aircandi.settingsEditor.putString(StringManager.getString(R.string.setting_invite_message_last), message);
		Aircandi.settingsEditor.commit();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, (emails.size() > 1) ? R.string.progress_sending_invite_plural : R.string.progress_sending_invite);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSendInvite");
				final ModelResult result = Aircandi.getInstance().getEntityManager().sendInvite(emails, invitor, message);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString((emails.size() > 1)
					                                                 ? R.string.alert_invite_sent_plural
					                                                 : R.string.alert_invite_sent)
							, Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(InviteEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.invite_edit;
	}
}