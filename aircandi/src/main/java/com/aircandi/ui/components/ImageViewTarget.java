package com.aircandi.ui.components;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

@SuppressWarnings("ucd")
public class ImageViewTarget implements Target {

	@Override
	public void onBitmapFailed(Drawable arg0) {}

	@Override
	public void onBitmapLoaded(Bitmap arg0, LoadedFrom arg1) {}

	@Override
	public void onPrepareLoad(Drawable arg0) {}

}
