package com.patchr.ui.edit;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.patchr.components.Dispatcher;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.ProximityController;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Beacon;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkOld;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.ResponseCode;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.PhotoSwitchboardScreen;
import com.patchr.ui.widgets.PhotoEditWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.segment.analytics.Properties;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Subscription;

public abstract class BaseEdit extends BaseScreen {

	protected PhotoEditWidget photoEditWidget;
	protected TextView        name;
	protected TextView        description;
	protected String          photoSource;

	protected AsyncTask    taskService;
	public    Subscription subscription;

	protected Boolean brokenLink        = false;
	protected Boolean proximityDisabled = false;        // Patch is only using location

	protected Integer insertProgressResId = R.string.progress_saving;
	protected Integer updateProgressResId = R.string.progress_updating;
	protected Integer insertedResId       = R.string.alert_inserted;
	protected Integer updatedResId        = R.string.alert_updated;

	protected Integer dirtyExitTitleResId    = R.string.alert_dirty_exit_title;
	protected Integer dirtyExitMessageResId  = R.string.alert_dirty_exit_message;
	protected Integer dirtyExitPositiveResId = R.string.alert_dirty_save;

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
						Reporting.track(AnalyticsCategory.ACTION, "Set Photo", new Properties().putValue("target", Utils.capitalize(entitySchema)));
						onPhotoSelected(photo);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_EDIT) {

				if (intent != null && intent.getExtras() != null) {
					Boolean changed = intent.getExtras().getBoolean(AdobeImageIntent.EXTRA_OUT_BITMAP_CHANGED, false);
					if (changed) {
						Reporting.track(AnalyticsCategory.ACTION, "Edited Photo");
					}
					final Uri photoUri = Uri.parse("file://" + intent.getData().toString());
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
		if (entity.getPhoto() != null) {
			final String url = entity.getPhoto().uriNative();
			Uri imageUri = Uri.parse(url);

			Intent intent = new AdobeImageIntent.Builder(this)
				.setData(imageUri)
				.withOutputFormat(Bitmap.CompressFormat.JPEG)
				.withOutputQuality(90)
				.saveWithNoChanges(false)
				.withOutputSize(MegaPixels.Mp5)
				.withPreviewSize((int) UI.getScreenWidthRawPixels(this) * 2)
				.withVibrationEnabled(true)
				.withAutoColorEnabled(true)
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

	@Subscribe public void onCancelEvent(ProcessingCanceledEvent event) {
		if (taskService != null) {
			taskService.cancel(true);
		}
	}

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

		name = (TextView) findViewById(R.id.name);
		description = (TextView) findViewById(R.id.description);
		photoEditWidget = (PhotoEditWidget) findViewById(R.id.photo_edit);
	}

	protected void bind() {

		if (entityId != null && inputState.equals(State.Editing)) {
			entity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();
			if (entity != null) {
				UI.setTextView(name, entity.name);
				UI.setTextView(description, entity.description);
				bindPhoto(entity.getPhoto());
				firstDraw = false;
			}
		}
	}

	protected void bindPhoto(Photo photo) {
		photoEditWidget.bind(photo);
	}

	protected void gather(SimpleMap parameters) {

		if (inputState.equals(State.Creating)) {
			if (name != null) {
				parameters.put("name", Type.emptyAsNull(name.getText().toString().trim()));
			}
			if (description != null) {
				parameters.put("description", Type.emptyAsNull(description.getText().toString().trim()));
			}
			if (photoEditWidget != null && photoEditWidget.photo != null) {
				parameters.put("photo", photoEditWidget.photo); // Could be null
			}
		}
		else if (inputState.equals(State.Editing)) {
			if (name != null && !name.getText().toString().equals(entity.name)) {
				parameters.put("name", Type.emptyAsNull(name.getText().toString().trim()));
			}
			if (description != null && !description.getText().toString().equals(entity.description)) {
				parameters.put("description", Type.emptyAsNull(description.getText().toString().trim()));
			}
			if (photoEditWidget != null && photoEditWidget.dirty) {
				parameters.put("photo", photoEditWidget.photo); // Could be null
			}
		}
	}

	protected boolean afterInsert(Entity entity) {
		return true;
	}

	protected boolean afterUpdate() {
		return true;
	}

	protected boolean isDirty() {
		if (inputState.equals(State.Creating)) {
			if (name != null && TextUtils.isEmpty(name.getText().toString())) {
				return true;
			}
			if (description != null && TextUtils.isEmpty(description.getText().toString())) {
				return true;
			}
			if (photoEditWidget != null && photoEditWidget.photo != null) {
				return true;
			}
		}
		else if (inputState.equals(State.Editing)) {
			if (name != null && !entity.name.equals(name.getText().toString())) {
				return true;
			}
			if (description != null && !entity.description.equals(description.getText().toString())) {
				return true;
			}
			if (photoEditWidget != null && photoEditWidget.dirty) {
				return true;
			}
		}

		return false;
	}

	protected boolean isValid() {
		return true;
	}

	protected void insert() {

		taskService = new AsyncTask() {

			@Override protected void onPreExecute() {
//				if (entity.getPhoto() != null && Type.isTrue(entity.getPhoto().store)) {
//					busyController.showHorizontalProgressBar(BaseEdit.this);
//				}
//				else {
//					busyController.show(BusyController.BusyAction.ActionWithMessage, insertProgressResId, BaseEdit.this);
//				}
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");

				List<Beacon> beacons = null;

				/* We only send beacons if a patch is being inserted */
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH) && !proximityDisabled) {
					beacons = ProximityController.getInstance().getStrongestBeacons(Constants.PROXIMITY_BEACON_COVERAGE);
				}

				/*
				 * Entity has a photo that needs to be stored in s3. Usually either a user
				 * photo from anywhere or a local photo from the device camera or gallery.
				 */
				Bitmap bitmap = null;
				if (entity.getPhoto() != null) {

					try {
						bitmap = Picasso.with(Patchr.applicationContext)
							.load(entity.getPhoto().uriNative())
							.centerInside()
							.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
							.get();

						if (isCancelled()) return null;
					}
					catch (OutOfMemoryError error) {
						/*
						 * We make attempt to recover by giving the vm another chance to
						 * garbage collect plus reduce the image size in memory by 75%.
						 */
						System.gc();
						try {
							bitmap = Picasso.with(Patchr.applicationContext)
								.load(entity.getPhoto().uriNative())
								.centerInside()
								.resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
								.get();

							if (isCancelled()) return null;
						}
						catch (OutOfMemoryError err) {
							/* Give up and log it */
							Reporting.breadcrumb("OutOfMemoryError: uri: " + entity.getPhoto().uriNative());
							throw err;
						}
						catch (IOException ignore) { }
					}
					catch (IOException ignore) {
						/*
						 * This is where we are ignoring exceptions like our reset problem with picasso. This
						 * can happen pulling an image from the network or from a local file.
						 */
						Reporting.breadcrumb("Picasso failed to load bitmap");
						if (isCancelled()) return null;
					}

					if (bitmap == null) {
						ModelResult result = new ModelResult();
						result.serviceResponse.responseCode = ResponseCode.FAILED;
						result.serviceResponse.errorResponse = new Errors.ErrorResponse(Errors.ErrorActionType.TOAST, StringManager.getString(R.string.error_image_unusable));
						result.serviceResponse.errorResponse.clearPhoto = true;
						busyController.hide(true);
						return result;
					}
				}

				/* In case a derived class needs to augment the entity or add links before insert */
				List<LinkOld> links = new ArrayList<>();
				//beforeInsert(entity, links);
				if (isCancelled()) return null;

				//ModelResult result = DataController.getInstance().insertEntity(entity, links, beacons, bitmap, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				busyController.hide(true);

				return null;
			}

			@Override protected void onCancelled(Object response) {
				/*
				 * Triggered by call to task.cancel() and guarantess that onPostExecute will not
				 * be called. If using task.cancel(true) and the task is running then AsyncTask
				 * will call interrupt on the thread which in turn will be picked up
				 * by okhttp before it begins the next blocking operation.
				 *
				 * Stopping Points (interrupt was triggered on background thread)
				 * - When task is pulled from queue (if waiting)
				 * - Between service and s3 calls.
				 * - During service calls assuming okhttp catches the interrupt.
				 * - During image upload to s3 if CancelEvent is sent via bus.
				 */
				busyController.hide(true);
				UI.toast(StringManager.getString(R.string.alert_cancelled));
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Entity insertedEntity = (Entity) result.data;
					entity.id = insertedEntity.id;

					if (entity.type == null || !entity.type.equals("share")) { // Shares covered in afterInsert()
						Reporting.track(AnalyticsCategory.EDIT, "Created " + Utils.capitalize(entity.schema));
					}
					/*
					 * In case a derived class needs to do something after a successful insert
					 */
					if (afterInsert(insertedEntity)) { // Returns true if OK to finish
						if (insertedResId != null && insertedResId != 0) {
							UI.toast(StringManager.getString(insertedResId));
						}
						final Intent intent = new Intent();
						intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, insertedEntity.schema);
						finish();
						AnimationManager.doOverridePendingTransition(BaseEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEdit.this, result.serviceResponse);
					if (result.serviceResponse.errorResponse != null) {
						if (result.serviceResponse.errorResponse.clearPhoto) {
							entity.setPhoto(null);
							bindPhoto(null);
						}
					}
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void update() {

		taskService = new AsyncTask() {

			@Override protected void onPreExecute() {
//				if (entity.getPhoto() != null && Type.isTrue(entity.getPhoto().store)) {
//					busyController.showHorizontalProgressBar(BaseEdit.this);
//				}
//				else {
//					busyController.show(BusyController.BusyAction.ActionWithMessage, R.string.progress_updating, BaseEdit.this);
//				}
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");

				/*
				 * Entity has a photo that needs to be stored in s3. Usually either a user
				 * photo from anywhere or a local photo from the device camera or gallery.
				 */
				Bitmap bitmap = null;
				if (entity.getPhoto() != null) {

					try {
						bitmap = Picasso.with(Patchr.applicationContext)
							.load(entity.getPhoto().uriNative())
							.centerInside()
							.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
							.get();

						if (isCancelled()) return null;
					}
					catch (OutOfMemoryError error) {
						/*
						 * We make attempt to recover by giving the vm another chance to
						 * garbage collect plus reduce the image size in memory by 75%.
						 */
						System.gc();
						try {
							bitmap = Picasso.with(Patchr.applicationContext)
								.load(entity.getPhoto().uriNative())
								.centerInside()
								.resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
								.get();

							if (isCancelled()) return null;
						}
						catch (IOException ignore) {}
					}
					catch (IOException ignore) {
						/*
						 * This is where we are ignoring exceptions like our reset problem with picasso. This
						 * can happen pulling an image from the network or from a local file.
						 */
						Reporting.breadcrumb("Picasso failed to load bitmap");
						if (isCancelled()) return null;
					}

					if (bitmap == null) {
						ModelResult result = new ModelResult();
						result.serviceResponse.responseCode = ResponseCode.FAILED;
						result.serviceResponse.errorResponse = new Errors.ErrorResponse(Errors.ErrorActionType.TOAST, StringManager.getString(R.string.error_image_unusable));
						result.serviceResponse.errorResponse.clearPhoto = true;
						busyController.hide(true);
						return result;
					}
				}

				//ModelResult result = DataController.getInstance().updateEntity(entity, bitmap, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				ModelResult result = new ModelResult();
				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				busyController.hide(true);

				return result;
			}

			@Override protected void onCancelled(Object response) {
				/*
				 * Stopping Points (interrupt was triggered on background thread)
				 * - When task is pulled from queue (if waiting)
				 * - Between service and s3 calls.
				 * - During service calls assuming okhttp catches the interrupt.
				 * - During image upload to s3 if CancelEvent is sent via bus.
				 */
				busyController.hide(true);
				UI.toast(StringManager.getString(R.string.alert_cancelled));
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (afterUpdate()) {  // Primary current use is for patch to cleanup proximity links if needed
						Reporting.track(AnalyticsCategory.EDIT, "Updated " + Utils.capitalize(entity.schema));
						UI.toast(StringManager.getString(updatedResId));
						finish();
						AnimationManager.doOverridePendingTransition(BaseEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEdit.this, result.serviceResponse);
					if (result.serviceResponse.errorResponse != null) {
						if (result.serviceResponse.errorResponse.clearPhoto) {
							entity.setPhoto(null);
							bindPhoto(null);
						}
					}
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
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
			, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						submitAction();
					}
					else if (which == DialogInterface.BUTTON_NEUTRAL) {
						cancelAction(true);
					}
				}
			}
			, null);
		dialog.setCanceledOnTouchOutside(false);
	}
}