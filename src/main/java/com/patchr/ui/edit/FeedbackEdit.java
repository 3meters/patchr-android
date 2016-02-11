package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.Document;
import com.patchr.ui.base.BaseEntityEdit;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.widgets.AirEditText;
import com.patchr.ui.widgets.UserView;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import java.util.HashMap;

public class FeedbackEdit extends BaseEntityEdit {

	private Document mFeedback;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		/*
		 * Feedback are not really an entity type so we handle
		 * all the expected initialization.
		 */
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		mDescription = (AirEditText) findViewById(R.id.description);

		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = !TextUtils.isEmpty(s);
				}
			});
		}
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Not a real entity so we completely override databind.
		 */
		mFeedback = new Document();
		mFeedback.type = "feedback";
		mFeedback.name = "patchr";
		mFeedback.data = new HashMap<String, Object>();
		draw(null);
	}

	@Override
	public void draw(View view) {
		((UserView) findViewById(R.id.created_by)).databind(UserManager.getInstance().getCurrentUser(), null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected String getLinkType() {
		return null;
	}

	@Override
	protected void gather() {
		mFeedback.data.put("message", mDescription.getText().toString().trim());
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
		if (mDescription.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected void insert() {

		Logger.i(this, "Insert feedback");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_sending, FeedbackEdit.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertFeedback");
				mFeedback.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = DataController.getInstance().insertDocument(mFeedback, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_feedback_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(FeedbackEdit.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.feedback_edit;
	}
}