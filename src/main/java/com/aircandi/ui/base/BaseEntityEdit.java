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
import com.aircandi.ServiceConstants;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.MediaManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.StringManager;
import com.aircandi.components.TrackerBase.TrackerCategory;
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
	protected String              mPhotoSource;
	protected ImageChooserManager mImageChooserManager;
	protected AsyncTask           mTaskService;

	protected Boolean mBrokenLink       = false;
	protected Boolean mProximityInvalid = false;

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
					if (mEntity.description == null || !s.toString().equals(mEntity.description)) {
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
			if (mEditing) {
				String title = !TextUtils.isEmpty(mEntity.name) ? mEntity.name : mEntity.schema;
				setActivityTitle(title);
			}
			else {
				setActivityTitle(StringManager.getString(R.string.label_edit_new_title) + " " + mEntity.getLabelForSchema());
			}

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

			/* Configure UI */
			UI.setVisibility(findViewById(R.id.button_delete), View.GONE);
			if (entity.ownerId != null
					&& (entity.ownerId.equals(Patchr.getInstance().getCurrentUser().id)
					|| (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
					&& Patchr.getInstance().getCurrentUser().developer != null
					&& Patchr.getInstance().getCurrentUser().developer))) {
				UI.setVisibility(findViewById(R.id.button_delete), View.VISIBLE);
			}
			mFirstDraw = false;
		}
	}

	protected void drawPhoto() {
		if (mPhotoView != null) {
			if (mPhotoView.getPhoto() == null
					|| mEntity.photo == null
					|| !mPhotoView.getPhoto().sameAs(mEntity.getPhoto())) {
				UI.drawPhoto(mPhotoView, mEntity.getPhoto());
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onError(final String reason) {
	    /*
	     * Error trying to pick or take a photo
		 */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				UI.showToastNotification(reason, Toast.LENGTH_SHORT);
				onCanceledPhoto();
			}
		});
	}

	public void onAccept() {

		if (mProcessing) return;
		mProcessing = true;

		if (isDirty()) {
			if (validate()) {
			    /*
			     * Pull all the control values back into the entity object. Validate
				 * does that too but we don't know if validate is always being performed.
				 */
				gather();

				if (mSkipSave) {
                    /*
					 * Using the intent just to pass data.
					 */
					mProcessing = false;
					final IntentBuilder intentBuilder = new IntentBuilder();
					intentBuilder.setEntity(mEntity);
					setResultCode(Constants.RESULT_ENTITY_EDITED, intentBuilder.create());
					finish();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.FORM_TO_PAGE);
				}
				else {
					if (mEditing) {
						update();
					}
					else {
						insert();
					}
				}
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

	@SuppressWarnings("ucd")
	public void onChangePhotoButtonClick(View view) {

		/* Ensure photo logic has the latest property values */
		gather();

		/* Route it */
		Patchr.dispatch.route(this, Route.PHOTO_SOURCE, mEntity, null, null);
		onChangingPhoto();
	}

	public void onPhotoSelected(Photo photo) {
		/*
		 * All photo selection sources and types end up here
		 */
		mDirty = !Photo.same(mEntity.photo, photo);
		if (mDirty) {

			mEntity.photo = photo;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					UI.drawPhoto(mPhotoView, mEntity.getPhoto());
					onChangedPhoto();
				}
			});
		}
	}

	protected void onChangedPhoto() {}

	protected void onChangingPhoto() {}

	protected void onCanceledPhoto() {}

	public void onImageChosen(final ChosenImage image) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (image != null) {
					Patchr.tracker.sendEvent(TrackerCategory.UX, "photo_used_from_device", null, 0);
					final Uri photoUri = Uri.parse("file:" + image.getFilePathOriginal());
					MediaManager.scanMedia(Uri.parse("file:" + image.getFilePathOriginal()));
					Photo photo = new Photo()
							.setPrefix(photoUri.toString())
							.setStore(true);
					onPhotoSelected(photo);
				}
			}
		});
	}

	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		Patchr.dispatch.route(this, Route.BROWSE, entity, null, null);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode == Activity.RESULT_CANCELED) {
			onCanceledPhoto();
		}
		else {

			if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String photoSource = extras.getString(Constants.EXTRA_PHOTO_SOURCE);

					if (!TextUtils.isEmpty(photoSource)) {
						mPhotoSource = photoSource;
						if (photoSource.equals(Constants.PHOTO_SOURCE_SEARCH)) {

							String defaultSearch = null;
							if (mEntity.name != null) {
								defaultSearch = mEntity.name.trim();
							}
							photoSearch(defaultSearch);
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_GALLERY)) {

							photoFromGallery();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_CAMERA)) {

							photoFromCamera();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_PLACE)) {

							photoFromPlace(mEntity);
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_DEFAULT)
								|| photoSource.equals(Constants.PHOTO_SOURCE_WEBSITE_THUMBNAIL)) {

							usePhotoDefault();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_SEARCH) {

				Patchr.tracker.sendEvent(TrackerCategory.UX, "photo_select_using_search", null, 0);
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
					onPhotoSelected(photo);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_PICK_PLACE) {

				Patchr.tracker.sendEvent(TrackerCategory.UX, "photo_select_from_place", null, 0);
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
					onPhotoSelected(photo);
				}
			}
			else if (requestCode == ChooserType.REQUEST_PICK_PICTURE) {

				mImageChooserManager.submit(requestCode, intent);
			}
			else if (requestCode == ChooserType.REQUEST_CAPTURE_PICTURE) {

				mImageChooserManager.submit(requestCode, intent);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

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
		Patchr.tracker.sendEvent(TrackerCategory.UX, "photo_set_to_default", null, 0);
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
		Patchr.dispatch.route(this, Route.PHOTO_SEARCH, null, null, extras);
	}

	protected void photoFromPlace(Entity entity) {
		Patchr.dispatch.route(this, Route.PHOTO_PLACE_SEARCH, entity, null, null);
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
					mBusy.showProgress();
				}
				else {
					mBusy.showBusy(BusyAction.Update);
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

				/* We only send beacons if a place is being inserted */
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && !mProximityInvalid) {
					beacons = ProximityManager.getInstance().getStrongestBeacons(ServiceConstants.PROXIMITY_BEACON_COVERAGE);
					primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;
				}

				Bitmap bitmap = null;
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {

					try {
						bitmap = DownloadManager.with(Patchr.applicationContext)
						                        .load(mEntity.getPhoto().getUri())
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
							                        .load(mEntity.getPhoto().getUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
							                        .get();

							if (isCancelled()) return null;
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

				ModelResult result = Patchr.getInstance().getEntityManager().insertEntity(mEntity, links, beacons, primaryBeacon, bitmap, true);
				if (isCancelled()) return null;

				/* Don't allow cancel if we made it this far */
				mBusy.hideBusy(true);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Entity insertedEntity = (Entity) result.data;
					mEntity.id = insertedEntity.id;
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
				mBusy.hideBusy(true);
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
						Patchr.getInstance().getAnimationManager().doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_TO_PAGE);
					}
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
				mProcessing = false;
			}
		}.execute();
	}

	@Override
	protected void update() {

		mTaskService = new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {
					mBusy.showProgress();
				}
				else {
					mBusy.showBusy(BusyAction.Update);
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
								if (isCancelled()) return null;
							}
							catch (IOException ignore) {}
						}
						catch (IOException ignore) {
							if (isCancelled()) return null;
						}
					}

					beforeUpdate(mEntity);
					result = Patchr.getInstance().getEntityManager().updateEntity(mEntity, bitmap);
					if (isCancelled()) return null;

					/* Don't allow cancel if we made it this far */
					mBusy.hideBusy(true);

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
							Patchr.getInstance().setCurrentUser((User) mEntity, true);
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
				mBusy.hideBusy(true);
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
						Patchr.getInstance().getAnimationManager().doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_TO_PAGE);
					}
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
				mProcessing = false;
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/
}