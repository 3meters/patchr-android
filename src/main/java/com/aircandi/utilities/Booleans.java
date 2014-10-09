package com.aircandi.utilities;

import com.aircandi.Patchr;

public class Booleans {

	public static Boolean getBoolean(int resId) {
		return Patchr.applicationContext.getResources().getBoolean(resId);
	}
}