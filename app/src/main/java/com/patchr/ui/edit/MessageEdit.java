package com.patchr.ui.edit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Message;
import com.patchr.objects.Photo;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;

public class MessageEdit extends BaseEdit {

	private String       inputPatchName;
	private TextView     patchName;
	private ImageWidget  userPhoto;
	private ViewAnimator photoEditAnimator;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

    /*--------------------------------------------------------------------------------------------
     * Events
     *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (editing) {
			getMenuInflater().inflate(R.menu.menu_save, menu);
			getMenuInflater().inflate(R.menu.menu_delete, menu);
		}
		else {
			getMenuInflater().inflate(R.menu.menu_send, menu);
		}

		return super.onCreateOptionsMenu(menu);
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

	@Override public void onClick(View view) {
		if (view.getId() == R.id.photo_delete_button) {
			this.photoEditAnimator.setDisplayedChild(0);
		}
		super.onClick(view);
	}

	@Override public void onPhotoSelected(Photo photo) {
		this.photoEditAnimator.setDisplayedChild(1);
		super.onPhotoSelected(photo);
	}

    /*--------------------------------------------------------------------------------------------
     * Methods
     *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.inputPatchName = extras.getString(Constants.EXTRA_ENTITY_PARENT_NAME);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		this.dirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		this.dirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		this.dirtyExitPositiveResId = R.string.alert_dirty_send;
		this.insertProgressResId = R.string.progress_sending;
		this.insertedResId = R.string.alert_message_sent;

		this.patchName = (TextView) findViewById(R.id.patch_name);
		this.userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		this.photoEditAnimator = (ViewAnimator) findViewById(R.id.photo_edit_animator);

		if (this.description != null) {
			this.description.setCompoundDrawables(null, null, null, null);  // Remove the clear button
		}

		this.actionBarTitle.setText(editing ? "Edit message" : "Message");
	}

	@Override public void bind() {
		super.bind();
		/* This method is only called when the activity is created.*/
		if (!this.editing) {
			UI.setTextView(this.patchName, this.inputPatchName + " Patch");
		}
		else {
			UI.setTextView(this.patchName, this.entity.patch.name + " Patch");
		}
		UI.setImageWithEntity(this.userPhoto, UserManager.currentUser);
	}

	protected void bindPhoto() {
		if (entity.photo != null) {
			this.photoEditAnimator.setDisplayedChild(1);
		}
		super.bindPhoto();
	}

	@Override protected boolean validate() {

		gather();

		if (this.entity.photo == null && TextUtils.isEmpty(this.entity.description)) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_message_content)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override protected void gather() {
		super.gather();

		if (!editing) {
			Message message = (Message) entity;
			message.type = "root";
			message.patchId = this.parentId;
		}
	}

	@Override protected String getEntitySchema() {
		return Constants.SCHEMA_ENTITY_MESSAGE;
	}

	@Override protected String getLinkType() {
		return Constants.TYPE_LINK_CONTENT;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_post;
	}
}