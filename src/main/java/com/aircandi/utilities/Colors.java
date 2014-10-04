package com.aircandi.utilities;

import com.aircandi.Patch;

public class Colors {

	public static int getColor(int resId) {
		return Patch.applicationContext.getResources().getColor(resId);
	}
}