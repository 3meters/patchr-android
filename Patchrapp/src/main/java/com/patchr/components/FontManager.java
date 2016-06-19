package com.patchr.components;

import android.graphics.Typeface;
import android.widget.TextView;

import com.patchr.Patchr;

public class FontManager {

	public static Typeface fontRobotoLight;
	public static Typeface fontRobotoRegular;
	public static Typeface fontRobotoMedium;

	private static FontManager        instance;
	private static FontManagerCreator creator;

	public static FontManager getInstance() {
		if (instance == null) {
			if (creator == null) {
				instance = new FontManager();
			}
			else {
				instance = creator.create();
			}
		}
		return instance;
	}

	private void initialize() {
		fontRobotoLight = Typeface.createFromAsset(Patchr.applicationContext.getAssets(), "Roboto-Light.ttf");
		fontRobotoRegular = Typeface.createFromAsset(Patchr.applicationContext.getAssets(), "Roboto-Regular.ttf");
		fontRobotoMedium = Typeface.createFromAsset(Patchr.applicationContext.getAssets(), "Roboto-Medium.ttf");
	}

	protected FontManager() {
		initialize();
	}

	public void setTypefaceLight(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoLight);
		}
	}

	public void setTypefaceRegular(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoRegular);
		}
	}

	public void setTypefaceMedium(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoMedium);
		}
	}

	/* Defaults */

	public void setTypefaceDefault(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoRegular);
		}
	}

	public void setTypefaceBoldDefault(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoMedium);
		}
	}

	public static interface FontManagerCreator {
		public FontManager create();
	}
}
