package com.patchr.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.objects.Command;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

public class PhotoPicker extends AppCompatActivity implements ImageChooserListener {

	private   String              pendingChoice;
	protected ImageChooserManager imageChooserManager;
	private   View                searchButton;
	private   View                galleryButton;
	private   View                cameraButton;

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

	@Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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

				Reporting.sendEvent(Reporting.TrackerCategory.UX, "photo_select_using_search", null, 0);
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_PHOTO);
					if (json != null) {
						final Photo photo = (Photo) Json.jsonToObject(json, Json.ObjectType.PHOTO);
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
					submitAction(photo);
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(Bundle savedInstanceState) {
		searchButton = findViewById(R.id.photo_search_button);
		galleryButton = findViewById(R.id.gallery_button);
		cameraButton = findViewById(R.id.camera_button);

		/* Only show the camera choice if there is one and there is a place to store the image */
		UI.setVisibility(cameraButton, View.GONE);
		if (MediaManager.canCaptureWithCamera()) {
			UI.setVisibility(cameraButton, View.VISIBLE);
		}
	}

	public void pickerAction(String choice) {

		switch (choice) {
			case Constants.PHOTO_ACTION_SEARCH:
				photoSearch(null);
				break;
			case Constants.PHOTO_ACTION_GALLERY:
				photoFromGallery();
				break;
			case Constants.PHOTO_ACTION_CAMERA:
				photoFromCamera();
				break;
		}
	}

	public void submitAction(Photo photo) {
		final Intent intent = new Intent();
		final String jsonPhoto = Json.objectToJson(photo);
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

	private void requestPermissions() {

		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final AlertDialog dialog = Dialogs.alertDialog(null
							, StringManager.getString(R.string.alert_permission_storage_title)
							, StringManager.getString(R.string.alert_permission_storage_message)
							, null
							, PhotoPicker.this
							, R.string.alert_permission_storage_positive
							, R.string.alert_permission_storage_negative
							, null
							, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (which == DialogInterface.BUTTON_POSITIVE) {
										if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
											ActivityCompat.requestPermissions(PhotoPicker.this
													, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
													, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
										}
										else {
											cancelAction(false);
										}
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

				imageChooserManager.setImageChooserListener(this);
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
}