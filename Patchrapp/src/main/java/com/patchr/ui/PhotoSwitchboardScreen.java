package com.patchr.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ChosenImages;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.model.Photo;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.collections.PhotoSearchScreen;
import com.patchr.utilities.Dialogs;
import com.patchr.components.ReportingManager;
import com.patchr.utilities.UI;

public class PhotoSwitchboardScreen extends AppCompatActivity implements ImageChooserListener {

	private   String              pendingChoice;
	protected ImageChooserManager imageChooserManager;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.screen_photo_switchboard);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		initialize(savedInstanceState);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {

		if (view.getId() == R.id.photo_search_button) {
			pickerAction(Constants.PHOTO_ACTION_SEARCH);
		}
		else if (view.getId() == R.id.gallery_button) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
					pendingChoice = Constants.PHOTO_ACTION_GALLERY;
					requestPermissions();
					return;
				}
			}
			pickerAction(Constants.PHOTO_ACTION_GALLERY);
		}
		else if (view.getId() == R.id.camera_button) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
					pendingChoice = Constants.PHOTO_ACTION_CAMERA;
					requestPermissions();
					return;
				}
			}
			pickerAction(Constants.PHOTO_ACTION_CAMERA);
		}
		else if (view.getId() == R.id.cancel_button) {
			cancelAction(false);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
				if (PermissionUtil.verifyPermissions(grantResults)) {
					pickerAction(pendingChoice);
				}
				else {
					cancelAction(false);
				}
			}
		}
	}

	@Override public void onBackPressed() {
		cancelAction(true);
	}

	@Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_PHOTO_SEARCH) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_PHOTO);
					if (json != null) {
						final Photo photo = Patchr.gson.fromJson(json, Photo.class);
						submitAction(photo);
					}
				}
			}
			else if (requestCode == ChooserType.REQUEST_PICK_PICTURE) {
				imageChooserManager.submit(requestCode, intent);
			}
			else if (requestCode == ChooserType.REQUEST_CAPTURE_PICTURE) {
				imageChooserManager.submit(requestCode, intent);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	public void submitAction(Photo photo) {
		final Intent intent = new Intent();
		final String jsonPhoto = Patchr.gson.toJson(photo);
		intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	public void cancelAction(Boolean force) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	@Override public void onImageChosen(final ChosenImage chosenImage) {
		runOnUiThread(() -> {
			if (chosenImage != null) {
				final Uri photoUri = Uri.parse("file://" + chosenImage.getFilePathOriginal());
				MediaManager.scanMedia(photoUri);
				Photo photo = new Photo(photoUri.toString(), Photo.PhotoSource.file);
				submitAction(photo);
			}
		});
	}

	@Override public void onError(final String reason) {
	    /*
	     * Error trying to pick or take a photo
		 */
		runOnUiThread(() -> UI.toast(reason));
	}

	@Override public void onImagesChosen(final ChosenImages chosenImages) {
		runOnUiThread(() -> {
			if (chosenImages != null && chosenImages.size() > 0) {
				final Uri photoUri = Uri.parse("file://" + chosenImages.getImage(0).getFilePathOriginal());
				MediaManager.scanMedia(photoUri);
				Photo photo = new Photo(photoUri.toString(), Photo.PhotoSource.file);
				submitAction(photo);
			}
		});
	}


	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(Bundle savedInstanceState) {
		View cameraButton = findViewById(R.id.camera_button);

		/* Only show the camera choice if there is one and there is a place to store the image */
		UI.setVisibility(cameraButton, View.GONE);
		if (MediaManager.canCaptureWithCamera()) {
			UI.setVisibility(cameraButton, View.VISIBLE);
		}
	}

	public void pickerAction(String choice) {

		switch (choice) {
			case Constants.PHOTO_ACTION_SEARCH:
				photoSearch();
				break;
			case Constants.PHOTO_ACTION_GALLERY:
				photoFromGallery();
				break;
			case Constants.PHOTO_ACTION_CAMERA:
				photoFromCamera();
				break;
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void requestPermissions() {

		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

			runOnUiThread(() -> {
				final AlertDialog dialog = Dialogs.alertDialog(null
					, StringManager.getString(R.string.alert_permission_storage_title)
					, StringManager.getString(R.string.alert_permission_storage_message)
					, null
					, PhotoSwitchboardScreen.this
					, R.string.alert_permission_storage_positive
					, R.string.alert_permission_storage_negative
					, null
					, (dialog1, which) -> {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
								ActivityCompat.requestPermissions(PhotoSwitchboardScreen.this
									, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
									, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
							}
							else {
								cancelAction(false);
							}
						}
					}, null);
				dialog.setCanceledOnTouchOutside(false);
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

	protected void photoFromGallery() {

		try {
			String directory = MediaManager.getTempDirectory(MediaManager.tempDirectoryName);
			if (directory != null) {
				//noinspection deprecation
				imageChooserManager = new ImageChooserManager(this
					, ChooserType.REQUEST_PICK_PICTURE
					, false);
				imageChooserManager.setImageChooserListener(this);
				imageChooserManager.choose();
			}
			else {
				UI.toast(StringManager.getString(R.string.error_storage_unmounted));
			}
		}
		catch (Exception e) {
			ReportingManager.breadcrumb("Image chooser failed to handle photo from device");
			ReportingManager.logException(e);
		}
	}

	protected void photoFromCamera() {
		try {
			imageChooserManager = new ImageChooserManager(this
				, ChooserType.REQUEST_CAPTURE_PICTURE
				, false);

			imageChooserManager.setImageChooserListener(this);
			imageChooserManager.choose();
		}
		catch (IllegalArgumentException e) {
			ReportingManager.logException(new IllegalArgumentException("Image chooser failed to handle photo from camera", e));
		}
		catch (Exception e) {
			ReportingManager.logException(new Exception("Image chooser failed to handle photo from camera", e));
		}
	}

	protected void photoSearch() {

		final Intent intent = new Intent(this, PhotoSearchScreen.class);
		startActivityForResult(intent, Constants.ACTIVITY_PHOTO_SEARCH);
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_TO);
	}
}