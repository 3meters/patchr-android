package com.aircandi.utilities;

import com.aircandi.Aircandi;

public class Colors {

	public static int getColor(int resId) {
		return Aircandi.applicationContext.getResources().getColor(resId);
	}
}