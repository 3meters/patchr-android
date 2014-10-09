package com.aircandi.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Document;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

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
					mDirty = s.toString() != null || !s.toString().equals("");
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
		mFeedback.name = "aircandi";
		mFeedback.data = new HashMap<String, Object>();
		draw(null);
	}

	@Override
	public void draw(View view) {
		((UserView) findViewById(R.id.created_by)).databind(Patchr.getInstance().getCurrentUser(), null);
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

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/
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
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_sending);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertFeedback");
				mFeedback.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = Patchr.getInstance().getEntityManager().insertDocument(mFeedback);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_feedback_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(FeedbackEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected int getLayoutId() {
		return R.layout.feedback_edit;
	}
}