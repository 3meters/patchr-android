package com.aircandi.utilities;

import com.aircandi.Patch;

public class Integers {

	public static Integer getInteger(int resId) {
		return Patch.applicationContext.getResources().getInteger(resId);
	}
}