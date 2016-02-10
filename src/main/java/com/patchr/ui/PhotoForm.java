package com.patchr.ui;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DownloadManager;
import com.patchr.components.MediaManager;
import com.patchr.components.StringManager;
import com.patchr.interfaces.IBind;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoSizeCategory;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.widgets.AirPhotoView;
import com.patchr.ui.widgets.UserView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import java.io.File;
import java.io.IOException;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoForm extends BaseActivity implements IBind {

	private Photo             mPhoto;
	private PhotoView         mPhotoView;
	private PhotoViewAttacher mAttacher;
	private MenuItem          mShareMenuItem;

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			if (jsonPhoto != null) {
				mPhoto = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTransitionType = TransitionType.DRILL_TO;
		mPhotoView = (PhotoView) findViewById(R.id.photo);
		mPhotoView.setBackgroundColor(Colors.getColor(R.color.background_picture_detail));

		bind(BindingMode.AUTO);
	}

	@Override
	public void bind(BindingMode mode) {

		final TextView name = (TextView) findViewById(R.id.name);
		final UserView user = (UserView) findViewById(R.id.author);

		/* Title */
		UI.setVisibility(name, View.GONE);
		if (!TextUtils.isEmpty(mPhoto.getName())) {
			name.setText(mPhoto.getName());
			UI.setVisibility(name, View.VISIBLE);
		}

		/* Author block */
		UI.setVisibility(user, View.GONE);
		if (mPhoto.getUser() != null) {
			Long createdAt = mPhoto.getCreatedAt() != null ? mPhoto.getCreatedAt().longValue() : null;
			user.databind(mPhoto.getUser(), createdAt);
			UI.setVisibility(user, View.VISIBLE);
		}

		/* Photo */

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncPhotoForm");

				final String url = mPhoto.getUri(PhotoSizeCategory.STANDARD);
				Bitmap bitmap = null;

				try {
					bitmap = DownloadManager.with(Patchr.applicationContext).load(url).get();
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

	@Override
	public void draw(View view) {}

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);
		builder.setType("image/jpeg").setStream(MediaManager.getSharePathUri());
		builder.setSubject(String.format(StringManager.getString(R.string.label_photo_share_subject), Patchr.getInstance().getCurrentUser().name));

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

	@Override
	protected int getLayoutId() {
		return R.layout.photo_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * Time to put the photo where the share handler can find it.
		 */
		if (item.getItemId() == R.id.share_photo) {

			final AirPhotoView photoView = (AirPhotoView) findViewById(R.id.photo);
			Bitmap bitmap = ((BitmapDrawable) photoView.getImageView().getDrawable()).getBitmap();
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