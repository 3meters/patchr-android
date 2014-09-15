package com.aircandi.components;

import android.os.Vibrator;

public class Shaker {
	private Shaker() {}

	public static boolean canShake(Vibrator vibrator) throws NullPointerException {
		boolean vibrates = false;
		if (vibrator != null)
			vibrates = vibrator.hasVibrator();
		else
			throw new NullPointerException();
		return vibrates;
	}
}