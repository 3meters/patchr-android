package com.aircandi.utilities;

import com.aircandi.Patchr;

public class Colors {

	public static int getColor(int resId) {
		return Patchr.applicationContext.getResources().getColor(resId);
	}
}