package com.aircandi.utilities;

import com.aircandi.Aircandi;

public class Booleans {

	public static Boolean getBoolean(int resId) {
		return Aircandi.applicationContext.getResources().getBoolean(resId);
	}
}