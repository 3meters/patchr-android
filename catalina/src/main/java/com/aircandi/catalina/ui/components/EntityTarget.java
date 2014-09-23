package com.aircandi.catalina.ui.components;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.aircandi.objects.Entity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class EntityTarget implements Target {

	protected Entity mEntity;

	public EntityTarget(Entity entity) {
		this.mEntity = entity;
	}

	public Entity getEntity() {
		return mEntity;
	}

	public void onBitmapLoaded(Bitmap bm, Picasso.LoadedFrom from) {}

	public void onBitmapFailed(Drawable errorDrawable) {}

	public void onPrepareLoad(Drawable placeHolderDrawable) {}

}
