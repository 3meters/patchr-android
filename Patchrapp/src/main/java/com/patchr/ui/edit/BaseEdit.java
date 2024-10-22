package com.patchr.ui.edit;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.adobe.creativesdk.aviary.AdobeImageIntent;
import com.adobe.creativesdk.aviary.internal.headless.utils.MegaPixels;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.ReportingManager;
import com.patchr.components.S3;
import com.patchr.components.StringManager;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.ResponseCode;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.PhotoSwitchboardScreen;
import com.patchr.ui.widgets.PhotoEditWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

public abstract class BaseEdit extends BaseScreen {

	protected PhotoEditWidget photoEditWidget;
	protected TextView        nameField;
	protected TextView        descriptionField;
	protected String          photoSource;

	protected Boolean brokenLink        = false;
	protected Boolean proximityDisabled = false;        // Patch is only using location

	protected Integer insertProgressResId = R.string.progress_saving;
	protected Integer updateProgressResId = R.string.progress_updating;
	protected Integer insertedResId       = R.string.alert_inserted;
	protected Integer updatedResId        = R.string.alert_updated;

	protected Integer dirtyExitTitleResId    = R.string.alert_dirty_exit_title;
	protected Integer dirtyExitMessageResId  = R.string.alert_dirty_exit_message;
	protected Integer dirtyExitPositiveResId = R.string.alert_dirty_save;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode != Activity.RESULT_CANCELED) {

			if (requestCode == Constants.ACTIVITY_PHOTO_PICK) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					if (jsonPhoto != null) {
						final Photo photo = Patchr.gson.fromJson(jsonPhoto, Photo.class);
						ReportingManager.getInstance().track(AnalyticsCategory.ACTION, String.format("Set Photo For %1$s", Utils.capitalize(entitySchema)));
						onPhotoSelected(photo);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_EDIT) {

				if (intent != null && intent.getExtras() != null) {
					Boolean changed = intent.getExtras().getBoolean(AdobeImageIntent.EXTRA_OUT_BITMAP_CHANGED, false);
					if (changed) {
						ReportingManager.getInstance().track(AnalyticsCategory.ACTION, "Edited Photo");
					}
					//final Uri photoUri = Uri.parse("file://" + intent.getParcelableExtra(AdobeImageIntent.EXTRA_OUTPUT_URI));
					final Uri photoUri = intent.getParcelableExtra(AdobeImageIntent.EXTRA_OUTPUT_URI);
					MediaManager.scanMedia(photoUri);

					Photo photo = new Photo(photoUri.toString(), Photo.PhotoSource.file);
					onPhotoSelected(photo);
				}
			}
		}
	}

	@Override public void cancelAction(Boolean force) {
		if (!force && isDirty()) {
			confirmDirtyExit();
		}
		else {
			super.cancelAction(force);
		}
	}

	public void onClick(View view) {
		if (view.getId() == R.id.photo_set_button) {
			setPhotoAction();
		}
		else if (view.getId() == R.id.photo_edit_button) {
			editPhotoAction();
		}
		else if (view.getId() == R.id.photo_delete_button) {
			deletePhotoAction();
		}
	}

	public void onPhotoSelected(Photo photo) {
		/*
		 * All photo selection sources and types end up here
		 */
		if (!Photo.same(photoEditWidget.photo, photo)) {
			bindPhoto(photo);
			photoEditWidget.dirty = true;
		}
	}

	public void setPhotoAction() {
		Intent intent = new Intent(this, PhotoSwitchboardScreen.class);
		startActivityForResult(intent, Constants.ACTIVITY_PHOTO_PICK);
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_TO);
	}

	public void editPhotoAction() {

		/* Route it - editor loads image directly from s3 skipping imgix service  */
		if (photoEditWidget.photo != null) {
			final String url = photoEditWidget.photo.uriNative();
			Uri imageUri = Uri.parse(url);

			Intent intent = new AdobeImageIntent.Builder(this)
				.setData(imageUri)
				.withOutputFormat(Bitmap.CompressFormat.JPEG)
				.withOutputQuality(90)
				.saveWithNoChanges(false)
				.withOutputSize(MegaPixels.Mp5)
				.withPreviewSize((int) UI.getScreenWidthRawPixels(this) * 2)
				.withVibrationEnabled(true)
				.build();

			startActivityForResult(intent, Constants.ACTIVITY_PHOTO_EDIT);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	public void deletePhotoAction() {
		photoEditWidget.dirty = (inputState.equals(State.Editing));
		bindPhoto(null);
	}

    /*--------------------------------------------------------------------------------------------
     * Notifications
     *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		/*
		 * Intent inputs:
		 * - Both: Edit_Only
		 * - New: Schema (required), Parent_Entity_Id
		 * - Edit: Entity (required)
		 */
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			inputState = extras.getString(Constants.EXTRA_STATE);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		nameField = (TextView) findViewById(R.id.name);
		descriptionField = (TextView) findViewById(R.id.description);
		photoEditWidget = (PhotoEditWidget) findViewById(R.id.photo_edit);
	}

	protected void bind() {

		if (entityId != null && inputState.equals(State.Editing)) {
			entity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();
			if (entity != null) {
				UI.setTextView(nameField, entity.name);
				UI.setTextView(descriptionField, entity.description);
				bindPhoto(entity.getPhoto());
				return;
			}
		}
		bindPhoto(null);
	}

	protected void bindPhoto(Photo photo) {
		photoEditWidget.bind(photo);
	}

	protected void gather(SimpleMap parameters) {

		if (inputState.equals(State.Inserting) || inputState.equals(State.Signup)) {
			if (nameField != null) {
				parameters.put("name", Type.emptyAsNull(nameField.getText().toString().trim()));
			}
			if (descriptionField != null) {
				parameters.put("description", Type.emptyAsNull(descriptionField.getText().toString().trim()));
			}
			if (photoEditWidget != null && photoEditWidget.photo != null) {
				parameters.put("photo", photoEditWidget.photo.asMap()); // Could be null
			}
		}
		else if (inputState.equals(State.Editing)) {
			if (nameField != null && !nameField.getText().toString().equals(entity.name)) {
				parameters.put("name", Type.emptyAsNull(nameField.getText().toString().trim()));
			}
			if (descriptionField != null && !descriptionField.getText().toString().equals(entity.description)) {
				parameters.put("description", Type.emptyAsNull(descriptionField.getText().toString().trim()));
			}
			if (photoEditWidget != null && photoEditWidget.dirty) {
				parameters.put("photo", photoEditWidget.photo != null ? photoEditWidget.photo.asMap() : null); // Could be null
			}
		}
	}

	protected Photo postPhotoToS3(Photo photo) {

		Bitmap bitmap = Photo.getBitmapForPhoto(photo);
		if (bitmap == null) {
			processing = false;
			busyController.hide(true);
			Logger.w(this, "Failed to download bitmap from the network");
			return null;
		}
		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = UI.ensureBitmapScaleForS3(bitmap);

		/* Push it to S3. It is always formatted/compressed as a jpeg. */
		String imageKey = Utils.createImageKey(); // User id at root to avoid collisions
		ServiceResponse serviceResponse = S3.getInstance().putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			return new Photo(imageKey, bitmap.getWidth(), bitmap.getHeight(), Photo.PhotoSource.aircandi_images);
		}

		return null;
	}

	protected boolean isDirty() {

		if (inputState != null) {
			if (inputState.equals(State.Inserting)) {
				if (nameField != null && !TextUtils.isEmpty(nameField.getText().toString())) {
					return true;
				}
				if (descriptionField != null && !TextUtils.isEmpty(descriptionField.getText().toString())) {
					return true;
				}
				if (photoEditWidget != null && photoEditWidget.photo != null) {
					return true;
				}
			}
			else if (inputState.equals(State.Editing)) {
				if (nameField != null && !Type.equal(entity.name, nameField.getText().toString())) {
					return true;
				}
				if (descriptionField != null && !Type.equal(entity.description, descriptionField.getText().toString())) {
					return true;
				}
				if (photoEditWidget != null && photoEditWidget.dirty) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean isValid() {
		return true;
	}

	protected void confirmDirtyExit() {

		final AlertDialog dialog = Dialogs.alertDialog(null
			, StringManager.getString(dirtyExitTitleResId)
			, StringManager.getString(dirtyExitMessageResId)
			, null
			, BaseEdit.this
			, dirtyExitPositiveResId
			, android.R.string.cancel
			, R.string.alert_dirty_discard
			, (dialog1, which) -> {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					submitAction();
				}
				else if (which == DialogInterface.BUTTON_NEUTRAL) {
					cancelAction(true);
				}
			}
			, null);
		dialog.setCanceledOnTouchOutside(false);
	}
}