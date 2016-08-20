package com.patchr.components;

import android.graphics.Typeface;

public class FontManager {

	public static Typeface fontLight;
	public static Typeface fontRegular;
	public static Typeface fontMedium;

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
		fontLight = Typeface.create("sans-serif-light", Typeface.NORMAL);
		fontRegular = Typeface.create("sans-serif", Typeface.NORMAL);
		fontMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
	}

	protected FontManager() {
		initialize();
	}

	public interface FontManagerCreator {
		FontManager create();
	}
}
