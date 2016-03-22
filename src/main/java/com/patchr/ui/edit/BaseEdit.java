package com.patchr.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.ProximityController;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Beacon;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Message;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.objects.User;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.views.ImageLayout;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEdit extends BaseScreen implements ImageChooserListener, Target {

	protected ImageLayout         photoView;
	protected TextView            name;
	protected TextView            description;
	protected View                buttonPhotoDelete;
	protected View                buttonPhotoEdit;
	protected View                buttonPhotoSet;
	protected String              photoSource;
	protected ImageChooserManager imageChooserManager;
	protected AsyncTask           taskService;

	protected Boolean brokenLink        = false;
	protected Boolean proximityDisabled = false;

	protected Integer insertProgressResId = R.string.progress_saving;
	protected Integer updateProgressResId = R.string.progress_updating;
	protected Integer insertedResId       = R.string.alert_inserted;
	protected Integer updatedResId        = R.string.alert_updated;

	/* Inputs */
	public String parentId;
	public String entitySchema;

	public Boolean editing = false;
	public Boolean dirty   = false;

	protected Integer dirtyExitTitleResId    = R.string.alert_dirty_exit_title;
	protected Integer dirtyExitMessageResId  = R.string.alert_dirty_exit_message;
	protected Integer dirtyExitPositiveResId = R.string.alert_dirty_save;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {

		final BitmapDrawable bitmapDrawable = new BitmapDrawable(Patchr.applicationContext.getResources(), bitmap);
		UI.showDrawableInImageView(bitmapDrawable, photoView.imageView, true);

		this.processing = false;

		UI.setVisibility(buttonPhotoEdit, View.VISIBLE);
		UI.setVisibility(buttonPhotoDelete, View.VISIBLE);

		photoView.showLoading(false);
	}

	@Override public void onBitmapFailed(Drawable arg0) {
		UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
		bindPhoto();
		this.processing = false;
	}

	@Override public void onPrepareLoad(Drawable drawable) { }

	@Override public void onImageChosen(final ChosenImage image) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (image != null) {
					Reporting.sendEvent(Reporting.TrackerCategory.UX, "photo_used_from_device", null, 0);
					final Uri photoUri = Uri.parse("file://" + image.getFilePathOriginal());
					MediaManager.scanMedia(photoUri);
					Photo photo = new Photo()
							.setPrefix(photoUri.toString())
							.setSource(Photo.PhotoSource.file);
					onPhotoSelected(photo);
				}
			}
		});
	}

	@Override public void onError(final String reason) {
	    /*
	     * Error trying to pick or take a photo
		 */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				UI.showToastNotification(reason, Toast.LENGTH_SHORT);
			}
		});
	}

	@Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode != Activity.RESULT_CANCELED) {

			if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String photoSource = extras.getString(Constants.EXTRA_PHOTO_SOURCE);

					if (!TextUtils.isEmpty(photoSource)) {
						this.photoSource = photoSource;
						switch (photoSource) {
							case Constants.PHOTO_ACTION_SEARCH:
								String defaultSearch = null;
								if (entity.name != null) {
									defaultSearch = entity.name.trim();
								}
								photoSearch(defaultSearch);
								break;
							case Constants.PHOTO_ACTION_GALLERY:
								photoFromGallery();
								break;
							case Constants.PHOTO_ACTION_CAMERA:
								photoFromCamera();
								break;
							case Constants.PHOTO_ACTION_DEFAULT:
							case Constants.PHOTO_ACTION_WEBSITE_THUMBNAIL:
								usePhotoDefault();
								break;
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_SEARCH) {

				Reporting.sendEvent(Reporting.TrackerCategory.UX, "photo_select_using_search", null, 0);
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_PHOTO);
					if (json != null) {
						final Photo photo = (Photo) Json.jsonToObject(json, Json.ObjectType.PHOTO);
						onPhotoSelected(photo);
					}
				}
			}
			else if (requestCode == ChooserType.REQUEST_PICK_PICTURE) {

				imageChooserManager.submit(requestCode, intent);
			}
			else if (requestCode == ChooserType.REQUEST_CAPTURE_PICTURE) {

				imageChooserManager.submit(requestCode, intent);
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
		super.onActivityResult(requestCode, resultCode, intent);
	}

	public void onEditPhotoButtonClick(View view) {

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

	public void onChangePhotoButtonClick(View view) {

		/* Ensure photo logic has the latest property values */
		gather();

		/* Route it */
		Patchr.router.route(this, Command.PHOTO_SOURCE, entity, null);
	}

	public void onDeletePhotoButtonClick(View view) {
		dirty = (editing);
		entity.photo = null;
		bindPhoto();
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
			this.parentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			this.entitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
		}
		this.editing = (this.entity != null);
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		name = (TextView) findViewById(R.id.name);
		description = (TextView) findViewById(R.id.description);
		photoView = (ImageLayout) findViewById(R.id.photo);
		buttonPhotoSet = findViewById(R.id.photo_set_button);
		buttonPhotoEdit = findViewById(R.id.photo_edit_button);
		buttonPhotoDelete = findViewById(R.id.photo_delete_button);

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

		if (photoView != null) {
			photoView.target = this;
		}

		/* Make new entity if we are not editing */
		if (!editing && this.entity == null && entitySchema != null) {

			if (entitySchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				this.entity = Message.build();
			}
			else if (entitySchema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				this.entity = Patch.build();
			}
			else if (entitySchema.equals(Constants.SCHEMA_ENTITY_USER)) {
				this.entity = User.build();
			}

			if (UserManager.shared().authenticated()) {
				entity.creator = UserManager.currentUser;
				entity.creatorId = UserManager.currentUser.id;
			}
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

	public void bind() {

		if (this.entity != null) {
			if (name != null && !TextUtils.isEmpty(entity.name)) {
				name.setText(entity.name);
			}
			if (description != null && !TextUtils.isEmpty(entity.description)) {
				description.setText(entity.description);
			}
			bindPhoto();
			firstDraw = false;
		}
	}

	protected void bindPhoto() {

		/* Can be called from main or background thread. */
		runOnUiThread(
				new Runnable() {
					@Override public void run() {
						if (photoView != null) {

							/* Photo adornments */
							UI.setVisibility(buttonPhotoSet, View.GONE);
							UI.setVisibility(buttonPhotoEdit, View.GONE);
							UI.setVisibility(buttonPhotoDelete, View.GONE);

							if (entity.photo == null) {
								UI.setVisibility(buttonPhotoSet, View.VISIBLE);
							}
							else {
								UI.setVisibility(buttonPhotoEdit, View.VISIBLE);
								UI.setVisibility(buttonPhotoDelete, View.VISIBLE);
							}

							if (entity.photo == null) {
								photoView.imageView.setImageDrawable(null);
							}
							else {
								processing = true;                             // So user can't post while we a trying to fetch the photo
								photoView.setImageWithPhoto(entity.photo);    // Only place we try to load a photo
							}
						}
					}
				});
	}

	protected void beforeInsert(Entity entity, List<Link> links) {
		if (parentId != null) {
			if (links == null) {
				links = new ArrayList<>();
			}
			links.add(new Link(parentId, getLinkType(), this.entity.schema));
		}
	}

	protected boolean afterInsert() {
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
								.load(entity.photo.uriDirect())
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
									.load(entity.photo.uriDirect())
									.centerInside()
									.resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
									.get();

							if (isCancelled()) return null;
						}
						catch (OutOfMemoryError err) {
							/* Give up and log it */
							Reporting.logMessage("OutOfMemoryError: uri: " + entity.photo.uriDirect());
							throw err;
						}
						catch (IOException ignore) { }
					}
					catch (IOException ignore) {
						/*
						 * This is where we are ignoring exceptions like our reset problem with picasso. This
						 * can happen pulling an image from the network or from a local file.
						 */
						Reporting.logException(new IOException("Picasso failed to load bitmap", ignore));
						if (isCancelled()) return null;
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
				UI.showToastNotification(StringManager.getString(R.string.alert_cancelled), Toast.LENGTH_SHORT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					Entity insertedEntity = (Entity) result.data;
					entity.id = insertedEntity.id;


					/*
					 * In case a derived class needs to do something after a successful insert
					 */
					if (afterInsert()) { // Returns true if OK to finish
						if (insertedResId != null && insertedResId != 0) {
							UI.showToastNotification(StringManager.getString(insertedResId), Toast.LENGTH_SHORT);
						}
						final Intent intent = new Intent();
						intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, insertedEntity.schema);
						finish();
						AnimationManager.doOverridePendingTransition(BaseEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEdit.this, result.serviceResponse);
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
				ModelResult result = new ModelResult();

				/* Update entity */
				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {

					Bitmap bitmap = null;
					if (entity.photo != null && Type.isTrue(entity.photo.store)) {

						try {
							bitmap = Picasso.with(Patchr.applicationContext)
									.load(entity.photo.uriDirect())
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
										.load(entity.photo.uriDirect())
										.centerInside()
										.resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
										.get();

								if (isCancelled()) return null;
							}
							catch (IOException ignore) {}
						}
						catch (IOException ignore) {
							Reporting.logException(new IOException("Picasso failed to load bitmap", ignore));
							if (isCancelled()) return null;
						}
					}

					result = DataController.getInstance().updateEntity(entity, bitmap, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
					if (isCancelled()) return null;

					/* Don't allow cancel if we made it this far */
					busyPresenter.hide(true);
				}
				return result.serviceResponse;
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
				UI.showToastNotification(StringManager.getString(R.string.alert_cancelled), Toast.LENGTH_SHORT);
			}

			@Override protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					if (afterUpdate()) {  // Primary current use is for patch to cleanup proximity links if needed
						UI.showToastNotification(StringManager.getString(updatedResId), Toast.LENGTH_SHORT);
						finish();
						AnimationManager.doOverridePendingTransition(BaseEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEdit.this, serviceResponse);
				}
				processing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void gather() {
		if (name != null) {
			entity.name = Type.emptyAsNull(name.getText().toString().trim());
		}
		if (description != null) {
			entity.description = Type.emptyAsNull(description.getText().toString().trim());
		}
	}

	protected String getLinkType() {
		return null;
	}

	protected void setEntityType(String type) {
		entity.type = type;
	}

	protected void usePhotoDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		Reporting.sendEvent(Reporting.TrackerCategory.UX, "photo_set_to_default", null, 0);
		onPhotoSelected(null);
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

	@SuppressLint("InlinedApi") protected void photoFromGallery() {

		try {
			String directory = MediaManager.getTempDirectory(MediaManager.tempDirectoryName);
			if (directory != null) {
				//noinspection deprecation
				imageChooserManager = new ImageChooserManager(this
						, ChooserType.REQUEST_PICK_PICTURE
						, false);
				imageChooserManager.setImageChooserListener((BaseEdit) this);
				imageChooserManager.choose();
			}
			else {
				UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
			}
		}
		catch (Exception e) {
			Reporting.logMessage("Image chooser failed to handle photo from device");
			Reporting.logException(e);
		}
	}

	protected void photoFromCamera() {
		try {
			String directory = MediaManager.getPhotoDirectory();
			if (directory != null) {
				//noinspection deprecation
				imageChooserManager = new ImageChooserManager(this
						, ChooserType.REQUEST_CAPTURE_PICTURE
						, directory
						, false);

				imageChooserManager.setImageChooserListener((BaseEdit) this);
				imageChooserManager.choose();
			}
			else {
				UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
			}
		}
		catch (IllegalArgumentException e) {
			Reporting.logException(new IllegalArgumentException("Image chooser failed to handle photo from camera", e));
		}
		catch (Exception e) {
			Reporting.logException(new Exception("Image chooser failed to handle photo from camera", e));
		}
	}

	protected void photoSearch(String defaultSearch) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_SEARCH_PHRASE, defaultSearch);
		Patchr.router.route(this, Command.PHOTO_SEARCH, null, extras);
	}

	protected void photoFromPlace(Entity entity) {
		Patchr.router.route(this, Command.PHOTO_PLACE_SEARCH, entity, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/
}