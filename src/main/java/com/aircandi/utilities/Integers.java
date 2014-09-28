package com.aircandi.utilities;

import com.aircandi.Aircandi;

public class Integers {

	public static Integer getInteger(int resId) {
		return Aircandi.applicationContext.getResources().getInteger(resId);
	}
}