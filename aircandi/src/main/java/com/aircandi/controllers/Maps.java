package com.aircandi.controllers;

import java.util.Map;

import android.graphics.drawable.Drawable;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.ui.MapForm;

public class Maps extends EntityControllerBase {

	public Maps() {
		mColorPrimary = R.color.holo_blue_dark;
		mBrowseClass = MapForm.class;
	}

	@Override
	public Entity makeNew() {
		return null;
	}

	@Override
	public Drawable getIcon() {
		Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_map_temp);
		//icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);		
		return icon;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return null;
	}
}
