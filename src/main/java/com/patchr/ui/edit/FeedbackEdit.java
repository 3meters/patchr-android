package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.TextView;
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
import com.patchr.objects.User;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.views.ImageLayout;
import com.patchr.ui.widgets.AirEditText;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import java.util.HashMap;

public class FeedbackEdit extends BaseEdit {

	private Document    feedback;
	private ImageLayout userPhoto;
	private TextView    userName;
	private TextView    message;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		description = (AirEditText) findViewById(R.id.description);

		if (description != null) {
			description.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					dirty = !TextUtils.isEmpty(s);
				}
			});
		}

		this.feedback = new Document();
		this.feedback.type = "feedback";
		this.feedback.name = "patchr";
		this.feedback.data = new HashMap<String, Object>();
	}

	@Override public void bind() {
		User user = UserManager.currentUser;
		this.userPhoto.setImageWithEntity(user);
		this.userName.setText(user.name);
	}

	@Override protected void gather() {
		feedback.data.put("message", description.getText().toString().trim());
	}

	@Override protected boolean validate() {
		if (!super.validate()) return false;
		if (description.getText().length() == 0) {
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

	@Override protected void insert() {

		Logger.i(this, "Insert feedback");

		new AsyncTask() {

			@Override protected void onPreExecute() {
				uiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_sending, FeedbackEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertFeedback");
				feedback.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = DataController.getInstance().insertDocument(feedback, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				uiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_feedback_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(FeedbackEdit.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected int getLayoutId() {
		return R.layout.feedback_edit;
	}
}