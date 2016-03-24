package com.patchr.ui.edit;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.objects.Command;
import com.patchr.objects.User;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Type;
import com.patchr.utilities.Utils;

import org.greenrobot.eventbus.Subscribe;

public class UserEdit extends BaseEdit {

	private EditText area;
	private EditText email;
	private Button   submitDelete;

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

	public void onClick(View view) {
		if (view.getId() == R.id.change_password_button) {
			Patchr.router.route(this, Command.PASSWORD_CHANGE, null, null);
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;

		if (editing) {
			getMenuInflater().inflate(R.menu.menu_save, menu);
			getMenuInflater().inflate(R.menu.menu_delete, menu);
		}

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.delete) {
			super.confirmDelete();
		}
		else if (item.getItemId() == R.id.submit) {
			super.submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
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

	@Override public void confirmDelete() {
		final EditText textConfirm = new EditText(this);
		textConfirm.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				submitDelete.setEnabled(s.toString().equals("YES"));
			}
		});
		final AlertDialog dialog = Dialogs.alertDialog(null
				, StringManager.getString(R.string.alert_delete_account_title)
				, StringManager.getString(R.string.alert_delete_account_message)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							delete();
						}
					}
				}
				, null);
		dialog.setView(textConfirm);
		submitDelete = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		submitDelete.setEnabled(false);
		dialog.setCanceledOnTouchOutside(false);
	}
}