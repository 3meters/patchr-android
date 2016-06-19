package com.patchr.utilities;

import com.patchr.Patchr;

public class Integers {

	public static Integer getInteger(int resId) {
		return Patchr.applicationContext.getResources().getInteger(resId);
	}
}