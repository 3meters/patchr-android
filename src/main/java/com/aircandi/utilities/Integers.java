package com.aircandi.utilities;

import com.aircandi.Patchr;

public class Integers {

	public static Integer getInteger(int resId) {
		return Patchr.applicationContext.getResources().getInteger(resId);
	}
}