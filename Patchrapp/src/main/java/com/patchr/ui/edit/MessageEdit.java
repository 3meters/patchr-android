package com.patchr.ui.edit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.model.Query;
import com.patchr.model.RealmEntity;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.QueryName;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.DeleteEntityJob;
import com.patchr.service.PostEntityJob;
import com.patchr.service.RestClient;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class MessageEdit extends BaseEdit {

	public String inputParentId;
	public String inputParentName;

	private TextView     patchNameView;
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

	@Override public void onPhotoSelected(Photo photo) {
		this.photoEditAnimator.setDisplayedChild(1);
		super.onPhotoSelected(photo);
	}

	@Override public void submitAction() {

		if (!isValid()) return;
		if (!isDirty()) return;
		if (!processing) {
			processing = true;
			post();
		}
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

		entitySchema = Constants.SCHEMA_ENTITY_MESSAGE;
		dirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		dirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		dirtyExitPositiveResId = R.string.alert_dirty_send;
		insertProgressResId = R.string.progress_sending;
		insertedResId = R.string.alert_message_sent;

		patchNameView = (TextView) findViewById(R.id.patch_name);
		userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		photoEditAnimator = (ViewAnimator) findViewById(R.id.photo_edit_animator);

		if (descriptionField != null) {
			descriptionField.setCompoundDrawables(null, null, null, null);  // Remove the clear button
		}

		actionBarTitle.setText(inputState.equals(State.Editing) ? "Edit message" : "Message");
	}

	@Override public void bind() { /* This method is only called when the activity is created.*/
		super.bind();

		if (State.Inserting.equals(inputState)) {
			UI.setTextView(this.patchNameView, this.inputParentName + " Patch");
		}
		else if (entity.patch != null) {
			UI.setTextView(this.patchNameView, this.entity.patch.name + " Patch");
		}
		UI.setImageWithEntity(this.userPhoto, UserManager.currentUser);
	}

	@Override protected void bindPhoto(Photo photo) {
		this.photoEditAnimator.setDisplayedChild(photo != null ? 1 : 0);
		super.bindPhoto(photo);
	}

	@Override protected void gather(SimpleMap parameters) {
		super.gather(parameters);   // Name, photo, description

		if (inputState.equals(State.Inserting)) {
			SimpleMap link = new SimpleMap();
			List<SimpleMap> links = new ArrayList<SimpleMap>();
			link.put("type", "content");
			link.put("_to", inputParentId);
			links.add(link);
			parameters.put("links", links);
		}
	}

	protected void post() {

		String path = entity == null ? "data/messages" : String.format("data/messages/%1$s", entityId);
		String entityId = this.entityId;
		SimpleMap data = new SimpleMap();

		if (State.Editing.equals(inputState) && entity != null) {
			realm.executeTransaction(whocares -> {
				entity.pending = true;
				entity.description = descriptionField.getText().toString().trim();
				entity.setPhoto(photoEditWidget.photo);
			});
		}
		else if (State.Inserting.equals(inputState)) {

			RealmEntity entity = RealmEntity.createBaseEntity(Constants.SCHEMA_ENTITY_MESSAGE);

			entityId = entity.id;
			entity.pending = true;
			entity.description = descriptionField.getText().toString().trim();
			entity.setPhoto(photoEditWidget.photo);

			realm.executeTransaction(whocares -> {
				RealmEntity realmEntity = realm.copyToRealmOrUpdate(entity);

				/* Fixup message lists */
				Query query = RestClient.getQuery(QueryName.MessagesForPatch, inputParentId);
				if (query != null) {
					query.entities.add(0, realmEntity);
				}

				/* Fixup patch link and counts */
				RealmEntity patch = realm.where(RealmEntity.class).equalTo("id", inputParentId).findFirst();
				if (patch != null) {
					patch.countMessages++;
					realmEntity.patch = patch;
				}
			});

			/* Link to patch */
			SimpleMap link = new SimpleMap();
			List<SimpleMap> links = new ArrayList<SimpleMap>();
			link.put("type", "content");
			link.put("_to", inputParentId);
			links.add(link);
			data.put("links", links);
		}

		Patchr.jobManager.addJobInBackground(new PostEntityJob(path, data, entityId, inputParentId));
		finish();
		AnimationManager.doOverridePendingTransition(MessageEdit.this, TransitionType.FORM_BACK);
	}

	@Override protected void delete() {

		String collection = RealmEntity.getCollectionForSchema(entity.schema);
		String schema = entity.schema;
		String path = String.format("data/%1$s/%2$s", collection, entityId);
		boolean share = Constants.TYPE_LINK_SHARE.equals(entity.type);
		String parentId = inputParentId;

		realm.executeTransaction(whocares -> {
			entity.deleteFromRealm();

			/* Fixup patch link and counts */
			RealmEntity patch = realm.where(RealmEntity.class).equalTo("id", parentId).findFirst();
			if (patch != null) {
				patch.countMessages--;
			}
		});

		if (share) {
			Patchr.jobManager.addJobInBackground(new DeleteEntityJob(path));
		}
		else {
			Patchr.jobManager.addJobInBackground(new DeleteEntityJob(path, parentId));
		}

		Reporting.track(AnalyticsCategory.EDIT, "Deleted " + Utils.capitalize(schema));
		Logger.i(this, "Deleted entity: " + entityId);
		UI.toast(StringManager.getString(R.string.alert_deleted));
		setResult(Constants.RESULT_ENTITY_DELETED);
		finish();
		AnimationManager.doOverridePendingTransition(MessageEdit.this, TransitionType.FORM_BACK);
	}

	@Override protected boolean isValid() {

		if (photoEditWidget.photo == null && TextUtils.isEmpty(descriptionField.getText().toString().trim())) {
			Dialogs.alert(R.string.error_missing_message_content, this);
			return false;
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_message;
	}
}