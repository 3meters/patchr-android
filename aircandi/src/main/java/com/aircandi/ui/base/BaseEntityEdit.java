package com.aircandi.ui.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.MediaManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.User;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;

public abstract class BaseEntityEdit extends BaseEdit implements ImageChooserListener {

	protected AirImageView mPhotoView;
	protected List<Entity> mApplinks;

	protected TextView            mName;
	protected TextView            mDescription;
	protected CheckBox            mLocked;
	protected ViewGroup           mPrivateHolder;
	protected String              mPhotoSource;
	protected ImageChooserManager mImageChooserManager;

	protected Boolean mBrokenLink = false;

	protected Integer mInsertProgressResId = R.string.progress_saving;
	protected Integer mUpdateProgressResId = R.string.progress_updating;
	protected Integer mInsertedResId       = R.string.alert_inserted;
	protected Integer mUpdatedResId        = R.string.alert_updated;

	/* Inputs */
	public String mParentId;
	public String mEntitySchema;

	@Override
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

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (TextView) findViewById(R.id.name);
		mDescription = (TextView) findViewById(R.id.description);
		mPhotoView = (AirImageView) findViewById(R.id.entity_photo);
		mLocked = (CheckBox) findViewById(R.id.chk_locked);
		mPrivateHolder = (ViewGroup) findViewById(R.id.holder_private);

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
		if (mLocked != null) {
			mLocked.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mEntity.locked != isChecked) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		//		if (mPrivate != null) {
		//			mPrivate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		//
		//				@Override
		//				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		//					if (mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE) != isChecked) {
		//						if (!mFirstDraw) {
		//							if (mEditing && isChecked && mEntity.visibility.equals(Constants.VISIBILITY_PUBLIC)) {
		//								Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
		//										, null
		//										, StringManager.getString(R.string.alert_switch_public_to_private)
		//										, null
		//										, BaseEntityEdit.this
		//										, android.R.string.ok
		//										, null, null, null, null);
		//							}
		//							mDirty = true;
		//						}
		//					}
		//				}
		//			});
		//		}
	}

	@Override
	public void bind(BindingMode mode) {
		if (!mEditing && mEntity == null && mEntitySchema != null) {
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(mEntitySchema);
			mEntity = controller.makeNew();
			setActivityTitle(StringManager.getString(R.string.label_edit_new_title) + " " + mEntity.getLabelForSchema());
			if (Aircandi.getInstance().getCurrentUser() != null) {
				mEntity.creator = Aircandi.getInstance().getCurrentUser();
				mEntity.creatorId = Aircandi.getInstance().getCurrentUser().id;
			}
		}
		draw();
	}

	@Override
	public void draw() {

		if (mEntity != null) {

			final Entity entity = mEntity;
			if (mEditing) {
				String title = !TextUtils.isEmpty(mEntity.name) ? mEntity.name : mEntity.getSchemaMapped();
				setActivityTitle(title);
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

			if (mLocked != null) {
				mLocked.setVisibility(View.VISIBLE);
				mLocked.setChecked(entity.locked);
			}

			UI.setVisibility(mPrivateHolder, View.GONE);
			//			if (mPrivate != null) {
			//				if (!mEditing || mEntity.isOwnedByCurrentUser()) {
			//					mPrivate.setChecked(mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE));
			//					UI.setVisibility(mPrivateHolder, View.VISIBLE);
			//				}
			//			}

			/* Shortcuts */

			if (findViewById(R.id.applinks) != null) {
				drawShortcuts(entity);
				UI.setVisibility(findViewById(R.id.alert), View.GONE);
				if (mBrokenLink) {
					((TextView) findViewById(R.id.alert)).setText(StringManager.getString(R.string.alert_applinks_broken));
					UI.setVisibility(findViewById(R.id.alert), View.VISIBLE);
				}
			}

			/* Creator block */
			final UserView creator = (UserView) findViewById(R.id.created_by);
			final UserView editor = (UserView) findViewById(R.id.edited_by);

			UI.setVisibility(creator, View.GONE);
			UI.setVisibility(editor, View.GONE);

			if (mEditing) {
				if (creator != null
						&& entity.creator != null
						&& !entity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
						&& !mEntity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

					creator.setLabel(R.string.label_added_by);
					creator.databind(entity.creator, (entity.createdDate != null) ? entity.createdDate.longValue() : null);
					UI.setVisibility(creator, View.VISIBLE);
				}

				/* Editor block */

				if (editor != null && entity.modifier != null
						&& !entity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)
						&& !entity.modifier.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
					if (entity.createdDate.longValue() != entity.modifiedDate.longValue()) {
						editor.setLabel(R.string.label_edited_by);
						editor.databind(entity.modifier, entity.modifiedDate.longValue());
						UI.setVisibility(editor, View.VISIBLE);
					}
				}
			}

			/* Configure UI */
			UI.setVisibility(findViewById(R.id.button_delete), View.GONE);
			if (entity.ownerId != null
					&& (entity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
					|| (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
					&& Aircandi.getInstance().getCurrentUser().developer != null
					&& Aircandi.getInstance().getCurrentUser().developer))) {
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

	protected void drawShortcuts(Entity entity) {

		/*
         * We are expecting a builder button with a viewgroup to
		 * hold a set of images.
		 */
		final BuilderButton button = (BuilderButton) findViewById(R.id.applinks);

		List<Shortcut> shortcuts;

		if (mApplinks != null) {
			shortcuts = new ArrayList<Shortcut>();
			for (Entity applink : mApplinks) {
				Shortcut shortcut = ((Applink) applink).getShortcut();
				shortcuts.add(shortcut);
			}
		}
		else {
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, false);
			settings.linkBroken = true;
			shortcuts = entity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
		}

		Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());

		if (shortcuts.size() == 0) {
			button.getTextView().setVisibility(View.VISIBLE);
			button.getViewGroup().setVisibility(View.GONE);
		}
		else {
			button.getTextView().setVisibility(View.GONE);
			button.getViewGroup().setVisibility(View.VISIBLE);
			button.getViewGroup().removeAllViews();
			final LayoutInflater inflater = LayoutInflater.from(this);
			final int sizePixels = UI.getRawPixelsForDisplayPixels(30f);
			final int marginPixels = UI.getRawPixelsForDisplayPixels(5f);

			/* We only show the first five */
			int shortcutCount = 0;
			mBrokenLink = false;
			for (Shortcut shortcut : shortcuts) {
				if (shortcutCount < 5) {
					View view = inflater.inflate(R.layout.temp_entity_edit_link_item, null);
					AirImageView photoView = (AirImageView) view.findViewById(R.id.entity_photo);
					photoView.setSizeHint(sizePixels);

					UI.drawPhoto(photoView, shortcut.getPhoto());

					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePixels, sizePixels);
					params.setMargins(marginPixels
							, marginPixels
							, marginPixels
							, marginPixels);
					view.setLayoutParams(params);
					button.getViewGroup().addView(view);
				}
				if (shortcut.validatedDate != null && shortcut.validatedDate.longValue() == -1) {
					mBrokenLink = true;
				}
				shortcutCount++;
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
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

	@Override
	public void onAccept() {
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
					final IntentBuilder intentBuilder = new IntentBuilder();
					intentBuilder.setEntity(mEntity);
					setResultCode(Constants.RESULT_ENTITY_EDITED, intentBuilder.create());
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.FORM_TO_PAGE);
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
		}
		else {
			onCancel(false);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePhotoButtonClick(View view) {

		/* Ensure photo logic has the latest property values */
		gather();

		/* Route it */
		Aircandi.dispatch.route(this, Route.PHOTO_SOURCE, mEntity, null, null);
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

	protected void onChangedPhoto() {
	}

	protected void onChangingPhoto() {
	}

	protected void onCanceledPhoto() {
	}

	@Override
	public void onImageChosen(final ChosenImage image) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (image != null) {
					Aircandi.tracker.sendEvent(TrackerCategory.UX, "photo_used_from_device", null, 0);
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

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		Aircandi.dispatch.route(this, Route.BROWSE, entity, null, null);
	}

	@Override
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
						else if (photoSource.equals(Constants.PHOTO_SOURCE_FACEBOOK)) {

							Photo photo = new Photo()
									.setPrefix("https://graph.facebook.com/" + ((Applink) mEntity).appId + "/picture?type=large")
									.setSource(PhotoSource.facebook);
							onPhotoSelected(photo);
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {

							Photo photo = new Photo()
									.setPrefix("https://api.twitter.com/1/users/profile_image?screen_name=" + ((Applink) mEntity).appId + "&size=bigger")
									.setSource(PhotoSource.facebook);
							onPhotoSelected(photo);
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_SEARCH) {

				Aircandi.tracker.sendEvent(TrackerCategory.UX, "photo_select_using_search", null, 0);
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
					onPhotoSelected(photo);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PHOTO_PICK_PLACE) {

				Aircandi.tracker.sendEvent(TrackerCategory.UX, "photo_select_from_place", null, 0);
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

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void buildPhoto() {
		if (mPhotoSource != null && mEntity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			if (mPhotoSource.equals(Constants.PHOTO_SOURCE_FACEBOOK)) {
				mEntity.photo = new Photo("https://graph.facebook.com/" + ((Applink) mEntity).appId + "/picture?type=large", null, null, null,
						PhotoSource.facebook);
			}
			else if (mPhotoSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {
				mEntity.photo = new Photo(
						"https://api.twitter.com/1/users/profile_image?screen_name=" + ((Applink) mEntity).appId + "&size=bigger",
						null,
						null, null, PhotoSource.twitter);
			}
		}
	}

	protected String getLinkType() {
		return null;
	}

	;

	protected void gather() {
		if (mName != null) {
			mEntity.name = Type.emptyAsNull(mName.getText().toString().trim());
		}
		if (mDescription != null) {
			mEntity.description = Type.emptyAsNull(mDescription.getText().toString().trim());
		}
		if (mLocked != null) {
			mEntity.locked = mLocked.isChecked();
		}
		//		if (mPrivate != null) {
		//			mEntity.visibility = mPrivate.isChecked() ? Constants.VISIBILITY_PRIVATE : Constants.VISIBILITY_PUBLIC;
		//		}

		/* Might need to rebuild photo because it requires current property values */
		buildPhoto();
	}

	protected void setEntityType(String type) {
		mEntity.type = type;
	}

	protected void usePhotoDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		Aircandi.tracker.sendEvent(TrackerCategory.UX, "photo_set_to_default", null, 0);
		onPhotoSelected(null);
	}

	// --------------------------------------------------------------------------------------------
	// Pickers
	// --------------------------------------------------------------------------------------------

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
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
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
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void photoSearch(String defaultSearch) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_SEARCH_PHRASE, defaultSearch);
		Aircandi.dispatch.route(this, Route.PHOTO_SEARCH, null, null, extras);
	}

	protected void photoFromPlace(Entity entity) {
		Aircandi.dispatch.route(this, Route.PHOTO_PLACE_SEARCH, entity, null, null);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

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

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, mInsertProgressResId);
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
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					beacons = ProximityManager.getInstance().getStrongestBeacons(ServiceConstants.PROXIMITY_BEACON_COVERAGE);
					primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;
				}

				Bitmap bitmap = null;
				if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {

					try {
						bitmap = DownloadManager.with(Aircandi.applicationContext)
						                        .load(mEntity.getPhoto().getUri())
						                        .centerInside()
						                        .resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
						                        .get();
					}
					catch (OutOfMemoryError error) {
						/*
						 * We make attempt to recover by giving the vm another chance to
						 * garbage collect plus reduce the image size in memory by 75%.
						 */
						System.gc();
						try {
							bitmap = DownloadManager.with(Aircandi.applicationContext)
							                        .load(mEntity.getPhoto().getUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_REDUCED, Constants.IMAGE_DIMENSION_REDUCED)
							                        .get();

						}
						catch (IOException ignore) {}
					}
					catch (IOException ignore) {}
				}

				/* In case a derived class needs to augment the entity or add links before insert */
				List<Link> links = new ArrayList<Link>();
				beforeInsert(mEntity, links);

				ModelResult result = Aircandi.getInstance().getEntityManager().insertEntity(mEntity, links, beacons, primaryBeacon, bitmap, true);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Entity insertedEntity = (Entity) result.data;
					mEntity.id = insertedEntity.id;

					if (mApplinks != null) {
						result = Aircandi.getInstance().getEntityManager()
						                 .replaceEntitiesForEntity(insertedEntity.id, mApplinks, Constants.SCHEMA_ENTITY_APPLINK);
                        /*
                         * Need to update the linkIn for the entity or these won't show
                         * without a service refresh.
                         */
					}
				}

				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				mBusy.hideBusy(true);
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
						Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_TO_PAGE);
					}
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
			}

		}.execute();
	}

	@Override
	protected void update() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, mUpdateProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertUpdateEntity");
				ModelResult result = new ModelResult();

				/* Save applinks first */
				if (mApplinks != null) {
					result = Aircandi.getInstance().getEntityManager().replaceEntitiesForEntity(mEntity.id, mApplinks, Constants.SCHEMA_ENTITY_APPLINK);
				}

				/* Update entity */
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Bitmap bitmap = null;
					if (mEntity.photo != null && Type.isTrue(mEntity.photo.store)) {

						try {
							bitmap = DownloadManager.with(Aircandi.applicationContext)
							                        .load(mEntity.getPhoto().getUri())
							                        .centerInside()
							                        .resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
							                        .get();
						}
						catch (IOException ignore) {}
					}

					result = Aircandi.getInstance().getEntityManager().updateEntity(mEntity, bitmap);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
							if (Aircandi.getInstance().getCurrentUser().id.equals(mEntity.id)) {

								/* We also need to update the user that has been persisted for AUTO sign in. */
								final String jsonUser = Json.objectToJson(mEntity);
								Aircandi.settingsEditor.putString(StringManager.getString(R.string.setting_user), jsonUser);
								Aircandi.settingsEditor.commit();

								/*
								 * Update the global user but retain the session info. We don't need
								 * to call activateCurrentUser because we don't need to refetch link data
								 * or change notification registration.
								 */
								((User) mEntity).session = Aircandi.getInstance().getCurrentUser().session;
								Aircandi.getInstance().setCurrentUser((User) mEntity);
							}
						}
					}
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				mBusy.hideBusy(true);
				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(StringManager.getString(mUpdatedResId), Toast.LENGTH_SHORT);
					setResultCode(Activity.RESULT_OK);
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_TO_PAGE);
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

}