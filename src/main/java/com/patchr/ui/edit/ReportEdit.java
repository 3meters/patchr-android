package com.patchr.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.patchr.objects.Document;
import com.patchr.objects.User;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.views.ImageLayout;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import java.util.HashMap;

public class ReportEdit extends BaseEdit {

	private Document    report;
	private String      reportType;
	private ImageLayout userPhoto;
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
		return true;
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

	public void onRadioButtonClicked(View view) {
		reportType = (String) view.getTag();
		dirty = true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		if (description != null) {
			description.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					dirty = !TextUtils.isEmpty(s);
				}
			});
		}

		this.userPhoto = (ImageLayout) findViewById(R.id.user_photo);
		this.userName = (TextView) findViewById(R.id.user_name);
		this.message = (TextView) findViewById(R.id.content_message);

		this.report = new Document();
		this.report.type = "report";
		this.report.name = "patchr";
		this.report.data = new HashMap<String, Object>();

		editing = false;
	}

	@Override public void bind() {
		this.message.setText(StringManager.getString(R.string.label_report_message) + " " + entitySchema + "?");
		User user = UserManager.currentUser;
		this.userPhoto.setImageWithEntity(user);
		this.userName.setText(user.name);
	}

	@Override protected void gather() {
		if (!TextUtils.isEmpty(description.getText().toString())) {
			report.data.put("message", description.getText().toString().trim());
		}
		report.data.put("type", reportType);
		report.data.put("target", entityId);
	}

	@Override protected boolean validate() {

		gather();
		if (TextUtils.isEmpty(reportType)) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_report_option)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override protected void insert() {

		Logger.i(this, "Insert report");

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_sending, ReportEdit.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertReport");
				report.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = DataController.getInstance().insertDocument(report, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_report_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(ReportEdit.this, result.serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_report;
	}
}