package com.patchr.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.objects.enums.PhotoCategory;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoScreen extends BaseScreen {

	private Photo             photo;
	private PhotoView         photoView;
	private PhotoViewAttacher attacher;
	private MenuItem          shareMenuItem;

	@Override public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_share_photo, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * Time to put the photo where the share handler can find it.
		 */
		if (item.getItemId() == R.id.share_photo) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
					requestPermissions();
					return true;
				}
			}
			shareAction();
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
				if (PermissionUtil.verifyPermissions(grantResults)) {
					shareAction();
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			if (jsonPhoto != null) {
				photo = Patchr.gson.fromJson(jsonPhoto, Photo.class);
			}
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		photoView = (PhotoView) findViewById(R.id.photo);
		photoView.setBackgroundColor(Colors.getColor(R.color.background_picture_detail));
		actionBar.setSubtitle("double-tap to zoom");
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_photo;
	}

	public void bind() {

		/* Photo */

		new AsyncTask() {

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncPhotoForm");

				final String url = photo.uri(PhotoCategory.STANDARD);
				Bitmap bitmap = null;

				try {
					bitmap = Picasso.with(Patchr.applicationContext).load(url).get();
				}
				catch (ConnectException e) {
					Reporting.breadcrumb("Picasso failed to load bitmap: connect");
				}
				catch (IOException e) {
					Reporting.breadcrumb("Picasso failed to load bitmap: io");
				}

				return bitmap;
			}

			@Override protected void onPostExecute(Object response) {
				final Bitmap bitmap = (Bitmap) response;
				final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
				photoView.setImageDrawable(bitmapDrawable);
				photoView.setTag(photo);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public void shareAction() {

		Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
		File file = MediaManager.copyBitmapToSharePath(bitmap);

		if (file == null) {
			UI.toast(StringManager.getString(R.string.error_storage_unmounted));
			return;
		}

		share();
	}

	public void share() {

		if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			UI.toast("Sharing a photo requires permission to read and write to storage");
			return;
		}

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);
		builder.setType("image/jpeg").setStream(MediaManager.getSharePathUri());
		builder.setSubject(String.format(StringManager.getString(R.string.label_photo_share_subject), UserManager.currentUser.name));
		builder.getIntent()
			.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName())
			.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PICTURE);

		builder.startChooser();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void requestPermissions() {
		/* Sharing a photo requires external storage permission on api 16+ */
		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

			runOnUiThread(new Runnable() {

				@Override public void run() {

					final AlertDialog dialog = Dialogs.alertDialog(null
						, StringManager.getString(R.string.alert_permission_storage_title)
						, StringManager.getString(R.string.alert_permission_storage_message)
						, null
						, PhotoScreen.this
						, R.string.alert_permission_storage_positive
						, R.string.alert_permission_storage_negative
						, null
						, new DialogInterface.OnClickListener() {

							@SuppressLint("InlinedApi") @Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_POSITIVE) {
									ActivityCompat.requestPermissions(PhotoScreen.this
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
			 * We get a callback when permission request is complete.
			 */
			ActivityCompat.requestPermissions(this
				, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
				, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
		}
	}
}