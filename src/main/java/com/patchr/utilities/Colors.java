package com.patchr.utilities;

import android.support.v4.content.ContextCompat;

import com.patchr.Patchr;

public class Colors {

	public static int getColor(int resId) {
		return ContextCompat.getColor(Patchr.applicationContext, resId);
	}
}