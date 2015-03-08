package com.aircandi.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.Document;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.components.SimpleTextWatcher;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

import java.util.HashMap;

public class ReportEdit extends BaseEntityEdit {

	private Document mReport;
	private String   mReportType;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
	/*
		 * Feedback are not really an entity type so we handle
		 * all the expected initialization.
		 */
		mDescription = (AirEditText) findViewById(R.id.description);

		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = !TextUtils.isEmpty(s);
				}
			});
		}

		((TextView) findViewById(R.id.content_message)).setText(StringManager.getString(R.string.label_report_message) + " " + mEntitySchema + "?");
		mEditing = false;
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Not a real entity so we completely override databind.
		 */
		mReport = new Document();
		mReport.type = "report";
		mReport.name = "aircandi";
		mReport.data = new HashMap<String, Object>();
		draw(null);
	}

	@Override
	public void draw(View view) {
		((UserView) findViewById(R.id.created_by)).databind(Patchr.getInstance().getCurrentUser(), null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	
	@Override
	public void onAccept() {
		if (validate()) {
			super.onAccept();
		}
	}

	@SuppressWarnings("ucd")
	public void onRadioButtonClicked(View view) {
		mReportType = (String) view.getTag();
		mDirty = true;
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
		if (!TextUtils.isEmpty(mDescription.getText().toString())) {
			mReport.data.put("message", mDescription.getText().toString().trim());
		}
		mReport.data.put("type", mReportType);
		mReport.data.put("target", mEntityId);
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
		if (TextUtils.isEmpty(mReportType)) {
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

	@Override
	protected void insert() {

		Logger.i(this, "Insert report");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_sending, ReportEdit.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertReport");
				mReport.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = Patchr.getInstance().getDataController().insertDocument(mReport, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(R.string.alert_report_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(ReportEdit.this, result.serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.report_edit;
	}
}