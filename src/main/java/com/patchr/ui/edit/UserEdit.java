package com.patchr.ui.edit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.objects.Route;
import com.patchr.objects.User;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Type;
import com.patchr.utilities.Utils;

import org.greenrobot.eventbus.Subscribe;

public class UserEdit extends BaseEdit {

	private EditText area;
	private EditText email;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onChangePasswordButtonClick(View view) {
		Patchr.router.route(this, Route.PASSWORD_CHANGE, null, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onCancelEvent(ProcessingCanceledEvent event) {
		if (taskService != null) {
			taskService.cancel(true);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		area = (EditText) findViewById(R.id.area);
		email = (EditText) findViewById(R.id.email);

		if (area != null) {
			area.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) entity).area)) {
						if (!firstDraw) {
							dirty = true;
						}
					}
				}
			});
		}

		if (email != null) {
			email.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) entity).email)) {
						if (!firstDraw) {
							dirty = true;
						}
					}
				}
			});
		}
	}

	@Override public void bind() {
		super.bind();

		User user = (User) entity;
		if (area != null && !TextUtils.isEmpty(user.area)) {
			area.setText(user.area);
		}
		if (email != null && !TextUtils.isEmpty(user.email)) {
			email.setText(user.email);
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

		if (!Utils.validEmail(email.getText().toString())) {
			Dialogs.alertDialogSimple(this, null, StringManager.getString(R.string.error_invalid_email));
			return false;
		}
		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.user_edit;
	}
}