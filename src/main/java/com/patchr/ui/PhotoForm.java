package com.patchr.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.BindingMode;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.views.UserView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoForm extends BaseActivity {

	private Photo             mPhoto;
	private PhotoView         mPhotoView;
	private PhotoViewAttacher mAttacher;
	private MenuItem          mShareMenuItem;

	@Override public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			if (jsonPhoto != null) {
				mPhoto = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
			}
		}
	}

	@Override public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTransitionType = TransitionType.DRILL_TO;
		mPhotoView = (PhotoView) findViewById(R.id.image_layout);
		mPhotoView.setBackgroundColor(Colors.getColor(R.color.background_picture_detail));

		bind(BindingMode.AUTO);
	}

	public void bind(BindingMode mode) {

		final TextView name = (TextView) findViewById(R.id.name);
		final UserView user = (UserView) findViewById(R.id.author);

		/* Title */
		UI.setVisibility(name, View.GONE);
		if (!TextUtils.isEmpty(mPhoto.getName())) {
			//name.setText(mPhoto.getName());
			UI.setVisibility(name, View.VISIBLE);
		}

		/* Author block */
		UI.setVisibility(user, View.GONE);
		if (mPhoto.user != null) {
			Long createdAt = mPhoto.getCreatedAt() != null ? mPhoto.getCreatedAt().longValue() : null;
			user.databind(mPhoto.user);
			UI.setVisibility(user, View.VISIBLE);
		}

		/* Photo */

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncPhotoForm");

				final String url = mPhoto.uri(PhotoCategory.STANDARD);
				Bitmap bitmap = null;

				try {
					bitmap = Picasso.with(Patchr.applicationContext).load(url).get();
				}
				catch (IOException e) {
					Reporting.logMessage("Picasso failed to load bitmap");
					Reporting.logException(new IOException("Picasso failed to load bitmap", e));
				}

				return bitmap;
			}

			@Override
			protected void onPostExecute(Object response) {
				final Bitmap bitmap = (Bitmap) response;
				final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
				mPhotoView.setImageDrawable(bitmapDrawable);
				mPhotoView.setTag(mPhoto);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public void share() {

		if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			UI.showToastNotification("Sharing a photo requires permission to read and write to storage", Toast.LENGTH_SHORT);
			return;
		}

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);
		builder.setType("image/jpeg").setStream(MediaManager.getSharePathUri());
		if (UserManager.getInstance().authenticated()) {
			builder.setSubject(String.format(StringManager.getString(R.string.label_photo_share_subject), UserManager.getInstance().getCurrentUser().name));
		}
		else {
			builder.setSubject(StringManager.getString(R.string.label_photo_share_subject_guest));
		}

		builder.getIntent()
				.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName())
				.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PICTURE);

		builder.startChooser();
	}

	protected void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setSubtitle("double-tap to zoom");
		}
	}

	private void ensurePermissions() {
		/*
		 * Sharing a photo requires external storage permission on api 16+
		 */
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

				if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final AlertDialog dialog = Dialogs.alertDialog(null
									, StringManager.getString(R.string.alert_permission_storage_title)
									, StringManager.getString(R.string.alert_permission_storage_message)
									, null
									, PhotoForm.this
									, R.string.alert_permission_storage_positive
									, R.string.alert_permission_storage_negative
									, null
									, new DialogInterface.OnClickListener() {

								@SuppressLint("InlinedApi") @Override public void onClick(DialogInterface dialog, int which) {
									if (which == DialogInterface.BUTTON_POSITIVE) {
										ActivityCompat.requestPermissions(PhotoForm.this
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
	}

	@Override protected int getLayoutId() {
		return R.layout.photo_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onDestroy() {
		super.onDestroy();
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * Time to put the photo where the share handler can find it.
		 */
		if (item.getItemId() == R.id.share_photo) {

			ensurePermissions();

			final PhotoView photoView = (PhotoView) findViewById(R.id.image_layout);
			Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
			File file = MediaManager.copyBitmapToSharePath(bitmap);

			if (file == null) {
				UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
				return true;
			}
		}

		super.onOptionsItemSelected(item);
		return true;
	}
}