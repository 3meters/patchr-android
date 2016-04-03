package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Document;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import java.util.HashMap;

public class FeedbackEdit extends BaseEdit {

	private Document    feedback;
	private ImageWidget userPhoto;
	private TextView    userName;
	private TextView    message;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_send, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			super.submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		this.actionBarTitle.setText(R.string.screen_title_feedback_edit);

		if (description != null) {
			description.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					dirty = !TextUtils.isEmpty(s);
				}
			});
		}

		userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		userName = (TextView) findViewById(R.id.user_name);

		this.feedback = new Document();
		this.feedback.type = "feedback";
		this.feedback.name = "patchr";
		this.feedback.data = new HashMap<String, Object>();
	}

	@Override public void bind() {
		this.userPhoto.setImageWithEntity(UserManager.currentUser);
		this.userName.setText(UserManager.currentUser.name);
	}

	@Override protected void gather() {
		feedback.data.put("message", description.getText().toString().trim());
	}

	@Override protected boolean validate() {

		gather();
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
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_sending, FeedbackEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertFeedback");
				feedback.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = DataController.getInstance().insertDocument(feedback, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.toast(StringManager.getString(R.string.alert_feedback_sent));
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
		return R.layout.edit_feedback;
	}
}