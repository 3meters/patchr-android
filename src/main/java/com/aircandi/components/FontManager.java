package com.aircandi.components;

import android.graphics.Typeface;
import android.widget.TextView;

import com.aircandi.Aircandi;

public class FontManager {

	public static Typeface fontRobotoThin;
	public static Typeface fontRobotoLight;
	public static Typeface fontRobotoRegular;
	public static Typeface fontRobotoMedium;
	public static Typeface fontRobotoBold;

	public static Typeface fontRobotoCondensedLight;
	public static Typeface fontRobotoCondensedRegular;
	public static Typeface fontRobotoCondensedBold;

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
		fontRobotoThin = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Thin.ttf");
		fontRobotoLight = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Light.ttf");
		fontRobotoRegular = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Regular.ttf");
		fontRobotoMedium = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Medium.ttf");
		fontRobotoBold = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Bold.ttf");
		fontRobotoCondensedLight = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "RobotoCondensed-Light.ttf");
		fontRobotoCondensedRegular = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "RobotoCondensed-Regular.ttf");
		fontRobotoCondensedBold = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "RobotoCondensed-Bold.ttf");
	}

	protected FontManager() {
		initialize();
	}

	public void setTypefaceThin(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoThin);
		}
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

	public void setTypefaceBold(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoBold);
		}
	}

	public void setTypefaceCondensedLight(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoCondensedLight);
		}
	}

	public void setTypefaceCondensedRegular(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoCondensedRegular);
		}
	}

	public void setTypefaceCondensedBold(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoCondensedBold);
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
