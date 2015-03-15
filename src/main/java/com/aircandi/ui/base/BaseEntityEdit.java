package com.aircandi.ui.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.MediaManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityController;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.components.SimpleTextWatcher;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEntityEdit extends BaseEdit implements ImageChooserListener {

	protected AirImageView        mPhotoView;
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
		mPhotoView = (AirImageView) findViewById(R.id.photo);
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

			if (!TextUtils.isEmpty(entity.name)) {
				if (mName != null) {
					mName.setText(entity.name);
				}
			}

			if (!TextUtils.isEmpty(entity.description)) {
				if (mDescription != null) {
					mDescription.setText(entity.description);
				}
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

		/* Route it */
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
					final Uri photoUri = Uri.parse("file:" + image.getFilePathOriginal());
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

	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		Patchr.router.route(this, Route.BROWSE, entity, null);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode == Activity.RESULT_OK) {

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
						else if (photoSource.equals(Constants.PHOTO_ACTION_EDIT)) {

							if (mEntity.photo != null) {
								final String jsonPhoto = Json.objectToJson(mEntity.photo);
								Bundle bundle = new Bundle();
								bundle.putString(Constants.EXTRA_PHOTO, jsonPhoto);
								Patchr.router.route(this, Route.PHOTO_EDIT, null, bundle);  // Checks for aviary and offers install option
							}
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
					final Photo photo = (Photo) Json.jsonToObject(json, Json.ObjectType.PHOTO);
					onPhotoSelected(photo);
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
					final Uri photoUri = Uri.parse("file:" + intent.getData().toString());
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

	/*--------------------------------------------------------------------------------------------
	 * Pickers
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("InlinedApi")
	protected void photoFromGallery() {

		try {
			String directory = MediaManager.getTempDirectory(MediaManager.tempDirectoryName);
			if (directory != null) {
				mImageChooserManager = new ImageChooserManager(this
						, ChooserType.REQUEST_PICK_PICTURE
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
			Reporting.logException(e);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
	}

	protected void photoFromCamera() {
		try {
			String directory = MediaManager.getPhotoDirectory();
			if (directory != null) {
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
			Reporting.logException(e);
		}
		catch (Exception e) {
			Reporting.logException(e);
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
					mUiController.getBusyController().show(BusyAction.Update);
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
						                        .load(mEntity.getPhoto().getUri())
						                        .centerInside()
						                        .resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
						                        .get();
						DownloadManager.logBitmap(BaseEntityEdit.this, bitmap);
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
							                        .load(mEntity.getPhoto().getUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
							                        .get();
							DownloadManager.logBitmap(BaseEntityEdit.this, bitmap);

							if (isCancelled()) return null;
						}
						catch (OutOfMemoryError err) {
							/* Give up and log it */
							Reporting.logMessage("OutOfMemoryError: uri: " + mEntity.getPhoto().getUri());
							throw err;
						}
						catch (IOException ignore) {}
					}
					catch (IOException ignore) {
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
					mUiController.getBusyController().show(BusyAction.Update);
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
							                        .load(mEntity.getPhoto().getUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
							                        .get();
							DownloadManager.logBitmap(BaseEntityEdit.this, bitmap);
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
								                        .load(mEntity.getPhoto().getUri())
								                        .centerInside()
								                        .resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
								                        .get();
								DownloadManager.logBitmap(BaseEntityEdit.this, bitmap);
								if (isCancelled()) return null;
							}
							catch (IOException ignore) {}
						}
						catch (IOException ignore) {
							if (isCancelled()) return null;
						}
					}

					beforeUpdate(mEntity);
					result = DataController.getInstance().updateEntity(mEntity, bitmap, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
					if (isCancelled()) return null;

					/* Don't allow cancel if we made it this far */
					mUiController.getBusyController().hide(true);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

						/* We are editing the current user */
						if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_USER) &&
								Patchr.getInstance().getCurrentUser().id.equals(mEntity.id)) {

							/* We also need to update the user that has been persisted for AUTO sign in. */
							final String jsonUser = Json.objectToJson(mEntity);
							Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), jsonUser);
							Patchr.settingsEditor.commit();

							/*
							 * Update the global user but retain the session info. We don't need
							 * to call activateCurrentUser because we don't need to refetch link data
							 * or change notification registration.
							 */
							((User) mEntity).session = Patchr.getInstance().getCurrentUser().session;
							Patchr.getInstance().setCurrentUser((User) mEntity, false);
						}
					}
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
					if (afterUpdate()) {
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