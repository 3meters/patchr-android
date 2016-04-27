package com.patchr.ui.edit;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.ProximityController;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.objects.Beacon;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Message;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.widgets.PhotoEditWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEdit extends BaseScreen {

	protected PhotoEditWidget photoEditWidget;
	protected TextView        name;
	protected TextView        description;
	protected String          photoSource;
	protected AsyncTask       taskService;

	protected Boolean brokenLink        = false;
	protected Boolean proximityDisabled = false;        // Patch is only using location

	protected Integer insertProgressResId = R.string.progress_saving;
	protected Integer updateProgressResId = R.string.progress_updating;
	protected Integer insertedResId       = R.string.alert_inserted;
	protected Integer updatedResId        = R.string.alert_updated;

	/* Inputs */
	public String parentId;
	public String parentName;
	public String entitySchema;

	public Boolean editing = false;
	public Boolean dirty   = false;

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
						final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
						onPhotoSelected(photo);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_EDIT) {

				if (intent != null && intent.getExtras() != null) {
					Boolean changed = intent.getExtras().getBoolean("bitmap-changed", false);
					if (changed) {
						Reporting.sendEvent(Reporting.TrackerCategory.UX, "photo_edited", null, 0);
					}
					final Uri photoUri = Uri.parse("file://" + intent.getData().toString());
					MediaManager.scanMedia(photoUri);
					Photo photo = new Photo()
							.setPrefix(photoUri.toString())
							.setStore(true)
							.setSource(Photo.PhotoSource.file);
					onPhotoSelected(photo);
				}
			}
		}
	}

	public void onClick(View view) {
		if (view.getId() == R.id.photo_set_button) {
			gather();
			Patchr.router.route(this, Command.PHOTO_PICK, entity, null);
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
		dirty = !Photo.same(entity.photo, photo);
		if (dirty) {

			entity.photo = photo;
			if (entity.photo != null) {
				entity.photo.setStore(true);
			}

			bindPhoto();
		}
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
		super.unpackIntent();
		/*
		 * Intent inputs:
		 * - Both: Edit_Only
		 * - New: Schema (required), Parent_Entity_Id
		 * - Edit: Entity (required)
		 */
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				this.entity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}

			this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			this.entitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);    // Used by support like reporting
			this.parentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			this.parentName = extras.getString(Constants.EXTRA_ENTITY_PARENT_NAME);
		}
		this.editing = (this.entity != null);
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		name = (TextView) findViewById(R.id.name);
		description = (TextView) findViewById(R.id.description);
		photoEditWidget = (PhotoEditWidget) findViewById(R.id.photo_edit);

		if (name != null) {
			name.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (entity.name == null || !s.toString().equals(entity.name)) {
						if (!firstDraw) {
							dirty = true;
						}
					}
				}
			});
		}
		if (description != null) {
			description.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (entity != null && (entity.description == null || !s.toString().equals(entity.description))) {
						if (!firstDraw) {
							dirty = true;
						}
					}
				}
			});
		}

		/* Make new entity if we are not editing */
		if (!editing && this.entity == null && getEntitySchema() != null) {

			if (getEntitySchema().equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				this.entity = Message.build();
			}
			else if (getEntitySchema().equals(Constants.SCHEMA_ENTITY_PATCH)) {
				this.entity = Patch.build();
			}
			else if (getEntitySchema().equals(Constants.SCHEMA_ENTITY_USER)) {
				this.entity = User.build();
			}

			if (UserManager.shared().authenticated()) {
				entity.creator = UserManager.currentUser;
				entity.creatorId = UserManager.currentUser.id;
			}
		}
	}

	@Override public void submitAction() {
		if (this.processing) return;

		this.processing = true;
		/*
		 * We assume that by accepting while creating a patch, the users intention is
		 * to commit even if nothing is dirty.
		 */
		if (!editing || this.dirty) {
			if (validate()) {   // validate also gathers
				if (editing) {
					update();
				}
				else {
					insert();
				}
			}
			else {
				this.processing = false;
			}
		}
		else {
			this.processing = false;
			cancelAction(false);
		}
	}

	@Override public void cancelAction(Boolean force) {
		if (!force && dirty) {
			confirmDirtyExit();
		}
		else {
			super.cancelAction(force);
		}
	}

	public void bind() {
		if (this.entity != null) {
			UI.setTextView(name, entity.name);
			UI.setTextView(description, entity.description);
			bindPhoto();
			firstDraw = false;
		}
	}

	protected void bindPhoto() {
		photoEditWidget.bind(entity.photo);
	}

	public void editPhotoAction() {

		/* Ensure photo logic has the latest property values */
		gather();

		/* Route it - editor loads image directly from s3 skipping imgix service  */
		if (entity.photo != null) {
			final String jsonPhoto = Json.objectToJson(entity.photo);
			Bundle bundle = new Bundle();
			bundle.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patchr.router.route(this, Command.PHOTO_EDIT, null, bundle);  // Checks for aviary and offers install option
		}
	}

	public void deletePhotoAction() {
		dirty = (editing);
		entity.photo = null;
		bindPhoto();
	}

	protected void beforeInsert(Entity entity, List<Link> links) {
		if (parentId != null) {
			if (links == null) {
				links = new ArrayList<>();
			}
			links.add(new Link(parentId, getLinkType(), this.entity.schema));
		}
	}

	protected boolean afterInsert(Entity entity) {
		return true;
	}

	protected boolean afterUpdate() {
		return true;
	}

	protected boolean validate() {
		return true;
	}

	protected void insert() {

		taskService = new AsyncTask() {

			@Override protected void onPreExecute() {
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {
					busyPresenter.showProgressDialog(BaseEdit.this);
				}
				else {
					busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, insertProgressResId, BaseEdit.this);
				}
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");

				List<Beacon> beacons = null;
				Beacon primaryBeacon = null;

				/* If parent id then this is a child */
				if (entity.linksIn != null) {
					entity.linksIn.clear();
				}

				if (entity.linksOut != null) {
					entity.linksOut.clear();
				}

				entity.toId = parentId;

				/* We only send beacons if a patch is being inserted */
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH) && !proximityDisabled) {
					beacons = ProximityController.getInstance().getStrongestBeacons(Constants.PROXIMITY_BEACON_COVERAGE);
					primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;
				}

				/*
				 * Entity has a photo that needs to be stored in s3. Usually either a user
				 * photo from anywhere or a local photo from the device camera or gallery.
				 */
				Bitmap bitmap = null;
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {

					try {
						bitmap = Picasso.with(Patchr.applicationContext)
								.load(entity.photo.uriNative())
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
									.load(entity.photo.uriNative())
									.centerInside()
									.resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
									.get();

							if (isCancelled()) return null;
						}
						catch (OutOfMemoryError err) {
							/* Give up and log it */
							Reporting.breadcrumb("OutOfMemoryError: uri: " + entity.photo.uriNative());
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
						result.serviceResponse.responseCode = NetworkManager.ResponseCode.FAILED;
						result.serviceResponse.errorResponse = new Errors.ErrorResponse(Errors.ResponseType.TOAST, StringManager.getString(R.string.error_image_unusable));
						result.serviceResponse.errorResponse.clearPhoto = true;
						busyPresenter.hide(true);
						return result;
					}
				}

				/* In case a derived class needs to augment the entity or add links before insert */
				List<Link> links = new ArrayList<>();
				beforeInsert(entity, links);
				if (isCancelled()) return null;

				ModelResult result = DataController.getInstance().insertEntity(entity, links, beacons, primaryBeacon, bitmap, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				busyPresenter.hide(true);

				return result;
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
				busyPresenter.hide(true);
				UI.toast(StringManager.getString(R.string.alert_cancelled));
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					Entity insertedEntity = (Entity) result.data;
					entity.id = insertedEntity.id;
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
							entity.photo = null;
							bindPhoto();
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
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {
					busyPresenter.showProgressDialog(BaseEdit.this);
				}
				else {
					busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_updating, BaseEdit.this);
				}
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");

				/*
				 * Entity has a photo that needs to be stored in s3. Usually either a user
				 * photo from anywhere or a local photo from the device camera or gallery.
				 */
				Bitmap bitmap = null;
				if (entity.photo != null && Type.isTrue(entity.photo.store)) {

					try {
						bitmap = Picasso.with(Patchr.applicationContext)
								.load(entity.photo.uriNative())
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
									.load(entity.photo.uriNative())
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
						result.serviceResponse.responseCode = NetworkManager.ResponseCode.FAILED;
						result.serviceResponse.errorResponse = new Errors.ErrorResponse(Errors.ResponseType.TOAST, StringManager.getString(R.string.error_image_unusable));
						result.serviceResponse.errorResponse.clearPhoto = true;
						busyPresenter.hide(true);
						return result;
					}
				}

				ModelResult result = DataController.getInstance().updateEntity(entity, bitmap, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				busyPresenter.hide(true);

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
				busyPresenter.hide(true);
				UI.toast(StringManager.getString(R.string.alert_cancelled));
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					if (afterUpdate()) {  // Primary current use is for patch to cleanup proximity links if needed
						UI.toast(StringManager.getString(updatedResId));
						finish();
						AnimationManager.doOverridePendingTransition(BaseEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEdit.this, result.serviceResponse);
					if (result.serviceResponse.errorResponse != null) {
						if (result.serviceResponse.errorResponse.clearPhoto) {
							entity.photo = null;
							bindPhoto();
						}
					}
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void gather() {
		if (entity != null && name != null) {
			entity.name = Type.emptyAsNull(name.getText().toString().trim());
		}
		if (entity != null && description != null) {
			entity.description = Type.emptyAsNull(description.getText().toString().trim());
		}
	}

	protected String getLinkType() {
		return null;
	}

	protected String getEntitySchema() {
		return null;
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

					@Override
					public void onClick(DialogInterface dialog, int which) {
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

	/*--------------------------------------------------------------------------------------------
	 * Pickers
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/
}