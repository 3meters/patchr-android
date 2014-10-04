package com.aircandi.utilities;

import com.aircandi.Patch;

public class Booleans {

	public static Boolean getBoolean(int resId) {
		return Patch.applicationContext.getResources().getBoolean(resId);
	}
}