package com.patchr.utilities;

import com.patchr.Patchr;

public class Booleans {

	public static Boolean getBoolean(int resId) {
		return Patchr.applicationContext.getResources().getBoolean(resId);
	}
}