package com.patchr.utilities;

import com.patchr.Patchr;

public class Colors {

	public static int getColor(int resId) {
		return Patchr.applicationContext.getResources().getColor(resId);
	}
}