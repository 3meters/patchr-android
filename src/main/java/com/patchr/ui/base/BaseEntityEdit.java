package com.patchr.ui.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.DownloadManager;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.PermissionUtil;
import com.patchr.components.ProximityController;
import com.patchr.components.StringManager;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Beacon;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.components.SimpleTextWatcher;
import com.patchr.ui.widgets.AirPhotoView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEntityEdit extends BaseEdit implements ImageChooserListener {

	protected AirPhotoView        mPhotoView;
	protected TextView            mName;
	protected TextView            mDescription;
	protected View                mButtonPhotoDelete;
	protected View                mButtonPhotoEdit;
	protected View                mButtonPhotoSet;
	protected String              mPhotoSource;
	protected ImageChooserManager mImageChooserManager;
	protected AsyncTask           mTaskService;

	protected Boolean mBrokenLink        = false;
	protected Boolean mProximityDisabled = false;

	protected Integer mInsertProgressResId = R.string.progress_saving;
	protected Integer mUpdateProgressResId = R.string.progress_updating;
	protected Integer mInsertedResId       = R.string.alert_inserted;
	protected Integer mUpdatedResId        = R.string.alert_updated;

	/* Inputs */
	public String mParentId;
	public String mEntitySchema;

	public void unpackIntent() {
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
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}

			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
		}
		mEditing = (mEntity != null);
	}

	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (TextView) findViewById(R.id.name);
		mDescription = (TextView) findViewById(R.id.description);
		mPhotoView = (AirPhotoView) findViewById(R.id.photo);
		mButtonPhotoSet = findViewById(R.id.button_photo_set);
		mButtonPhotoEdit = findViewById(R.id.button_photo_edit);
		mButtonPhotoDelete = findViewById(R.id.button_photo_delete);

		if (mName != null) {
			mName.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntity.name == null || !s.toString().equals(mEntity.name)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntity != null && (mEntity.description == null || !s.toString().equals(mEntity.description))) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}

		ensurePermissions();
	}

	public void bind(BindingMode mode) {
		if (!mEditing && mEntity == null && mEntitySchema != null) {
			IEntityController controller = Patchr.getInstance().getControllerForSchema(mEntitySchema);
			mEntity = controller.makeNew();
			if (Patchr.getInstance().getCurrentUser() != null) {
				mEntity.creator = Patchr.getInstance().getCurrentUser();
				mEntity.creatorId = Patchr.getInstance().getCurrentUser().id;
			}
		}
		draw(null);
	}

	public void draw(View view) {

		if (mEntity != null) {

			final Entity entity = mEntity;

			/* Content */

			drawPhoto();

			if (mName != null && !TextUtils.isEmpty(entity.name)) {
				mName.setText(entity.name);
			}
			if (mDescription != null && !TextUtils.isEmpty(entity.description)) {
				mDescription.setText(entity.description);
			}

			mFirstDraw = false;
		}
	}

	protected void drawPhoto() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mPhotoView != null) {
					if (mPhotoView.getPhoto() == null
							|| mEntity.photo == null
							|| !mPhotoView.getPhoto().sameAs(mEntity.getPhoto())) {
						UI.drawPhoto(mPhotoView, mEntity.getPhoto());
					}

					/* Photo adornments */
					UI.setVisibility(mButtonPhotoSet, View.GONE);
					UI.setVisibility(mButtonPhotoEdit, View.GONE);
					UI.setVisibility(mButtonPhotoDelete, View.GONE);

					if (mEntity.photo == null) {
						UI.setVisibility(mButtonPhotoSet, View.VISIBLE);
					}
					else {
						UI.setVisibility(mButtonPhotoEdit, View.VISIBLE);
						UI.setVisibility(mButtonPhotoDelete, View.VISIBLE);
					}
				}
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAccept() {
		if (mProcessing) return;
		mProcessing = true;
		/*
		 * We assume that by accepting while creating a patch, the users intention is
		 * to commit even if nothing is dirty.
		 */
		if (!mEditing || isDirty()) {
			if (validate()) {
			    /*
			     * Pull all the control values back into the entity object. Validate
				 * does that too but we don't know if validate is always being performed.
				 */
				gather();
				accept();
			}
			else {
				mProcessing = false;
			}
		}
		else {
			mProcessing = false;
			onCancel(false);
		}
	}

	public void onEditPhotoButtonClick(View view) {

		/* Ensure photo logic has the latest property values */
		gather();

		/* Route it - editor loads image directly from s3 skipping imgix service  */
		if (mEntity.photo != null) {
			final String jsonPhoto = Json.objectToJson(mEntity.photo);
			Bundle bundle = new Bundle();
			bundle.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Patchr.router.route(this, Route.PHOTO_EDIT, null, bundle);  // Checks for aviary and offers install option
		}
	}

	public void onChangePhotoButtonClick(View view) {

		/* Ensure photo logic has the latest property values */
		gather();

		/* Route it */
		Patchr.router.route(this, Route.PHOTO_SOURCE, mEntity, null);
	}

	public void onDeletePhotoButtonClick(View view) {
		mDirty = (mEditing);
		mEntity.photo = null;
		mPhotoView.setPhoto(null);
		drawPhoto();
	}

	public void onPhotoSelected(Photo photo) {
		/*
		 * All photo selection sources and types end up here
		 */
		mDirty = !Photo.same(mEntity.photo, photo);
		if (mDirty) {

			mEntity.photo = photo;
			if (mEntity.photo != null) {
				mEntity.photo.setStore(true);
			}

			drawPhoto();
		}
	}

	public void onImageChosen(final ChosenImage image) {
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

	public void onError(final String reason) {
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

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
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
						mPhotoSource = photoSource;
						if (photoSource.equals(Constants.PHOTO_ACTION_SEARCH)) {

							String defaultSearch = null;
							if (mEntity.name != null) {
								defaultSearch = mEntity.name.trim();
							}
							photoSearch(defaultSearch);
						}
						else if (photoSource.equals(Constants.PHOTO_ACTION_GALLERY)) {

							photoFromGallery();
						}
						else if (photoSource.equals(Constants.PHOTO_ACTION_CAMERA)) {

							photoFromCamera();
						}
						else if (photoSource.equals(Constants.PHOTO_ACTION_DEFAULT)
								|| photoSource.equals(Constants.PHOTO_ACTION_WEBSITE_THUMBNAIL)) {

							usePhotoDefault();
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

				mImageChooserManager.submit(requestCode, intent);
			}
			else if (requestCode == ChooserType.REQUEST_CAPTURE_PICTURE) {

				mImageChooserManager.submit(requestCode, intent);
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void accept() {
		if (mEditing) {
			update();
		}
		else {
			insert();
		}
	}

	protected String getLinkType() {
		return null;
	}

	protected void gather() {
		if (mName != null) {
			mEntity.name = Type.emptyAsNull(mName.getText().toString().trim());
		}
		if (mDescription != null) {
			mEntity.description = Type.emptyAsNull(mDescription.getText().toString().trim());
		}
	}

	protected void setEntityType(String type) {
		mEntity.type = type;
	}

	protected void usePhotoDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		Reporting.sendEvent(Reporting.TrackerCategory.UX, "photo_set_to_default", null, 0);
		onPhotoSelected(null);
	}

	private void ensurePermissions() {

		if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final AlertDialog dialog = Dialogs.alertDialog(null
								, StringManager.getString(R.string.alert_permission_storage_title)
								, StringManager.getString(R.string.alert_permission_storage_message)
								, null
								, BaseEntityEdit.this
								, R.string.alert_permission_storage_positive
								, R.string.alert_permission_storage_negative
								, null
								, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_POSITIVE) {
									ActivityCompat.requestPermissions(BaseEntityEdit.this
											, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
											, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
								}
							}
						}, null);
						dialog.setCanceledOnTouchOutside(false);
					}
				});
			}
			else {
				/*
				 * No explanation needed, we can request the permission.
				 * Parent activity will broadcast an event when permission request is complete.
				 */
				ActivityCompat.requestPermissions(this
						, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
						, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
			}
		}
	}


	/*--------------------------------------------------------------------------------------------
	 * Pickers
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("InlinedApi")
	protected void photoFromGallery() {

		try {
			String directory = MediaManager.getTempDirectory(MediaManager.tempDirectoryName);
			if (directory != null) {
				//noinspection deprecation
				mImageChooserManager = new ImageChooserManager(this
						, ChooserType.REQUEST_PICK_PICTURE
						, false);
				mImageChooserManager.setImageChooserListener((BaseEntityEdit) this);
				mImageChooserManager.choose();
			}
			else {
				UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
			}
		}
		catch (IllegalArgumentException e) {
			Reporting.logMessage("Image chooser failed to handle photo from device");
			Reporting.logException(e);
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
				mImageChooserManager = new ImageChooserManager(this
						, ChooserType.REQUEST_CAPTURE_PICTURE
						, directory
						, false);

				mImageChooserManager.setImageChooserListener((BaseEntityEdit) this);
				mImageChooserManager.choose();
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
		Patchr.router.route(this, Route.PHOTO_SEARCH, null, extras);
	}

	protected void photoFromPlace(Entity entity) {
		Patchr.router.route(this, Route.PHOTO_PLACE_SEARCH, entity, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void beforeInsert(Entity entity, List<Link> links) {
		if (mParentId != null) {
			if (links == null) {
				links = new ArrayList<Link>();
			}
			links.add(new Link(mParentId, getLinkType(), mEntity.schema));
		}
	}

	@Override
	protected void insert() {

		mTaskService = new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {
					mUiController.getBusyController().showProgressDialog(BaseEntityEdit.this);
				}
				else {
					mUiController.getBusyController().show(BusyAction.ActionWithMessage, mInsertProgressResId, BaseEntityEdit.this);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");

				List<Beacon> beacons = null;
				Beacon primaryBeacon = null;

				/* If parent id then this is a child */
				if (mEntity.linksIn != null) {
					mEntity.linksIn.clear();
				}

				if (mEntity.linksOut != null) {
					mEntity.linksOut.clear();
				}

				mEntity.toId = mParentId;

				/* We only send beacons if a patch is being inserted */
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH) && !mProximityDisabled) {
					beacons = ProximityController.getInstance().getStrongestBeacons(Constants.PROXIMITY_BEACON_COVERAGE);
					primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;
				}

				/*
				 * Entity has a photo that needs to be stored in s3. Usually either a user
				 * photo from anywhere or a local photo from the device camera or gallery.
				 */
				Bitmap bitmap = null;
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {

					try {
						bitmap = DownloadManager.with(Patchr.applicationContext)
						                        .load(mEntity.getPhoto().getDirectUri())
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
							bitmap = DownloadManager.with(Patchr.applicationContext)
							                        .load(mEntity.getPhoto().getDirectUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
							                        .get();

							if (isCancelled()) return null;
						}
						catch (OutOfMemoryError err) {
							/* Give up and log it */
							Reporting.logMessage("OutOfMemoryError: uri: " + mEntity.getPhoto().getDirectUri());
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
				List<Link> links = new ArrayList<Link>();
				beforeInsert(mEntity, links);
				if (isCancelled()) return null;

				ModelResult result = DataController.getInstance().insertEntity(mEntity, links, beacons, primaryBeacon, bitmap, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				mUiController.getBusyController().hide(true);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Entity insertedEntity = (Entity) result.data;
					mEntity.id = insertedEntity.id;
				}

				return result.serviceResponse;
			}

			@Override
			protected void onCancelled(Object response) {
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
				mUiController.getBusyController().hide(true);
				UI.showToastNotification(StringManager.getString(R.string.alert_cancelled), Toast.LENGTH_SHORT);
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					/*
					 * In case a derived class needs to do something after a successful insert
					 */
					if (afterInsert()) { // Returns true if OK to finish
						if (mInsertedResId != null && mInsertedResId != 0) {
							UI.showToastNotification(StringManager.getString(mInsertedResId), Toast.LENGTH_SHORT);
						}
						setResultCode(Activity.RESULT_OK);
						finish();
						AnimationManager.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override
	protected void update() {

		mTaskService = new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {
					mUiController.getBusyController().showProgressDialog(BaseEntityEdit.this);
				}
				else {
					mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_updating, BaseEntityEdit.this);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");
				ModelResult result = new ModelResult();

				/* Update entity */
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Bitmap bitmap = null;
					if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {

						try {
							bitmap = DownloadManager.with(Patchr.applicationContext)
							                        .load(mEntity.getPhoto().getDirectUri())
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
								bitmap = DownloadManager.with(Patchr.applicationContext)
								                        .load(mEntity.getPhoto().getDirectUri())
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

					beforeUpdate(mEntity);
					result = DataController.getInstance().updateEntity(mEntity, bitmap, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
					if (isCancelled()) return null;

					/* Don't allow cancel if we made it this far */
					mUiController.getBusyController().hide(true);
				}
				return result.serviceResponse;
			}

			@Override
			protected void onCancelled(Object response) {
				/*
				 * Stopping Points (interrupt was triggered on background thread)
				 * - When task is pulled from queue (if waiting)
				 * - Between service and s3 calls.
				 * - During service calls assuming okhttp catches the interrupt.
				 * - During image upload to s3 if CancelEvent is sent via bus.
				 */
				mUiController.getBusyController().hide(true);
				UI.showToastNotification(StringManager.getString(R.string.alert_cancelled), Toast.LENGTH_SHORT);
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (afterUpdate()) {  // Primary current use is for patch to cleanup proximity links if needed
						UI.showToastNotification(StringManager.getString(mUpdatedResId), Toast.LENGTH_SHORT);
						setResultCode(Activity.RESULT_OK);
						finish();
						AnimationManager.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_BACK);
					}
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
				mProcessing = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}