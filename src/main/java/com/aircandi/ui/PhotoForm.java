package com.aircandi.ui;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.MediaManager;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBind;
import com.aircandi.objects.Photo;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import java.io.File;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

public class PhotoForm extends BaseActivity implements IBind {

	protected static int DEFAULT_ANIMATION_DURATION = 200;

	private Photo          mPhoto;
	private ImageViewTouch mImageViewTouch;
	private AirImageView   mPhotoView;
	private MenuItem       mShareMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			mPhoto = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mTransitionType = TransitionType.DRILL_TO;
		bind(BindingMode.AUTO);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onZoomIn() {
//		mImageViewTouch.setDoubleTapDirection(1);
//		float scale = mImageViewTouch.getScale();
//		float targetScale;
//		targetScale = mImageViewTouch.onDoubleTapPost(scale, mImageViewTouch.getMaxScale());
//		targetScale = Math.min(mImageViewTouch.getMaxScale(), Math.max(targetScale, mImageViewTouch.getMinScale()));
//		mImageViewTouch.zoomTo(targetScale, DEFAULT_ANIMATION_DURATION);
	}

	public void onZoomOut() {
//		mImageViewTouch.setDoubleTapDirection(-1);
//		float scale = mImageViewTouch.getScale();
//		float targetScale;
//		targetScale = mImageViewTouch.onDoubleTapPost(scale, mImageViewTouch.getMaxScale());
//		targetScale = Math.min(mImageViewTouch.getMaxScale(), Math.max(targetScale, mImageViewTouch.getMinScale()));
//		mImageViewTouch.zoomTo(targetScale, DEFAULT_ANIMATION_DURATION);
	}

	protected void bindImageViewTouch(ViewGroup layout) {
		if (layout != null) {
			AirImageView image = (AirImageView) layout.findViewById(R.id.photo);
			mImageViewTouch = (ImageViewTouch) image.getImageView();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final ImageViewTouch imageView = (ImageViewTouch) photoView.getImageView();
		imageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
		super.onConfigurationChanged(newConfig);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setSubtitle("double-tap to zoom");
		}
	}

	@Override
	public void bind(BindingMode mode) {

		final ViewGroup layout = (ViewGroup) findViewById(R.id.holder_body);
		buildPictureDetail(mPhoto, layout);
		bindImageViewTouch(layout);
	}

	@Override
	public void draw(View view) {}

	public ViewGroup buildPictureDetail(Photo photo, ViewGroup layout) {

		mPhotoView = (AirImageView) layout.findViewById(R.id.photo);
		layout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				UI.showToastNotification("touched", Toast.LENGTH_SHORT);
				return false;
			}
		});

		final TextView name = (TextView) layout.findViewById(R.id.name);
		final UserView user = (UserView) layout.findViewById(R.id.author);
		final ImageView imageView = (ImageView) mPhotoView.getImageView();

		((ImageViewTouch) imageView).setDisplayType(DisplayType.FIT_TO_SCREEN);
		((ImageViewTouch) imageView).setScrollEnabled(true);

		/* Title */
		UI.setVisibility(name, View.GONE);
		if (!TextUtils.isEmpty(photo.getName())) {
			name.setText(photo.getName());
			UI.setVisibility(name, View.VISIBLE);
		}

		/* Author block */
		UI.setVisibility(user, View.GONE);
		if (photo.getUser() != null) {
			user.databind(photo.getUser(), photo.getCreatedAt().longValue());
			UI.setVisibility(user, View.VISIBLE);
		}

		/* Photo */
		mPhotoView.setTag(photo);
		mPhotoView.setCenterCrop(false);
		UI.drawPhoto(mPhotoView, photo);

		return layout;
	}

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
			final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
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