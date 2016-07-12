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
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.State;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;

import java.util.ArrayList;
import java.util.List;

public class MessageEdit extends BaseEdit {

	public String inputParentId;
	public String inputParentName;

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

		if (inputState.equals(State.Editing)) {
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
			confirmDelete();
		}
		else if (item.getItemId() == R.id.submit) {
			submitAction();
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
			inputParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			inputParentName = extras.getString(Constants.EXTRA_ENTITY_PARENT_NAME);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		dirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		dirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		dirtyExitPositiveResId = R.string.alert_dirty_send;
		insertProgressResId = R.string.progress_sending;
		insertedResId = R.string.alert_message_sent;

		patchName = (TextView) findViewById(R.id.patch_name);
		userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		photoEditAnimator = (ViewAnimator) findViewById(R.id.photo_edit_animator);

		if (description != null) {
			description.setCompoundDrawables(null, null, null, null);  // Remove the clear button
		}

		actionBarTitle.setText(inputState.equals(State.Editing) ? "Edit message" : "Message");
	}

	@Override public void bind() { /* This method is only called when the activity is created.*/
		super.bind();

		if (!inputState.equals(State.Editing)) {
			UI.setTextView(this.patchName, this.inputParentName + " Patch");
		}
		else {
			UI.setTextView(this.patchName, this.entity.patch.name + " Patch");
		}
		UI.setImageWithEntity(this.userPhoto, UserManager.currentUser);
	}

	@Override protected void bindPhoto() {
		if (entity.getPhoto() != null) {
			this.photoEditAnimator.setDisplayedChild(1);
		}
		super.bindPhoto();
	}

	@Override protected boolean isValid() {

		if (photoEditWidget.photo == null && TextUtils.isEmpty(description.getText().toString().trim())) {
			Dialogs.alert(R.string.error_missing_message_content, this);
			return false;
		}

		return true;
	}

	@Override protected void gather(SimpleMap parameters) {
		super.gather(parameters);   // Name, photo, description

		if (inputState.equals(State.Creating)) {
			SimpleMap link = new SimpleMap();
			List<SimpleMap> links = new ArrayList<SimpleMap>();
			link.put("type", "content");
			link.put("_to", inputParentId);
			links.add(link);
			parameters.put("links", links);
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_post;
	}
}