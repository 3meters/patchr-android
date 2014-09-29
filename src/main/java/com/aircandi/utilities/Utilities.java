package com.aircandi.utilities;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;

import com.aircandi.Patch;
import com.aircandi.components.Logger;

import java.util.regex.Pattern;

public class Utilities {

	public static Boolean validEmail(String email) {
		return EMAIL_ADDRESS.matcher(email).matches();
	}

	public static Boolean validWebUri(String webUri) {
		return WEB_URL.matcher(webUri).matches();
	}

	public static Long getMemoryAvailable() {
		MemoryInfo memoryInfo = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) Patch.applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(memoryInfo);
		long availableMemory = memoryInfo.availMem / 1048576L;
		return availableMemory;
	}

	@SuppressWarnings("ucd")
	public static ScreenSize getScreenSize() {
		int screenLayout = Patch.applicationContext.getResources().getConfiguration().screenLayout;
		screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;

		switch (screenLayout) {
			case Configuration.SCREENLAYOUT_SIZE_SMALL:
				return ScreenSize.SMALL;
			case Configuration.SCREENLAYOUT_SIZE_NORMAL:
				return ScreenSize.NORMAL;
			case Configuration.SCREENLAYOUT_SIZE_LARGE:
				return ScreenSize.LARGE;
			case 4: // Configuration.SCREENLAYOUT_SIZE_XLARGE is API >= 9
				return ScreenSize.XLARGE;
			default:
				return ScreenSize.UNDEFINED;
		}
	}

	public static enum ScreenSize {
		SMALL,
		NORMAL,
		LARGE,
		XLARGE,
		UNDEFINED
	}

	public static int calculateMemoryCacheSize(Context context) {
		/*
		 * Get memory class of this device, exceeding this amount will throw an
		 * OutOfMemory exception.
		 */
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int memClass = am.getMemoryClass();

		final boolean largeHeap = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
		if (largeHeap) {
			memClass = am.getLargeMemoryClass();
		}

		Logger.i(context, "Device memory class: " + String.valueOf(memClass));
		Patch.memoryClass = memClass;

		/* Use 1/4th of the available memory for this memory cache. */
		final int cacheSize = (memClass << 10 << 10) >> 2;
		Logger.i(context, "Memory cache size: " + String.valueOf(cacheSize));

		return cacheSize;
	}

	private static final Pattern EMAIL_ADDRESS                    = Pattern.compile(
			"[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
					"\\@" +
					"[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
					"(" +
					"\\." +
					"[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
					")+"
	);
	/**
	 * Regular expression to match all IANA top-level domains for WEB_URL.
	 * List accurate as of 2010/02/05. List taken from:
	 * http://data.iana.org/TLD/tlds-alpha-by-domain.txt
	 * This pattern is AUTO-generated by frameworks/base/common/tools/make-iana-tld-pattern.py
	 */
	private static final String  TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL = "(?:"
			+ "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
			+ "|(?:biz|b[abdefghijmnorstvwyz])"
			+ "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
			+ "|d[ejkmoz]"
			+ "|(?:edu|e[cegrstu])"
			+ "|f[ijkmor]"
			+ "|(?:gov|g[abdefghilmnpqrstuwy])"
			+ "|h[kmnrtu]"
			+ "|(?:info|int|i[delmnoqrst])"
			+ "|(?:jobs|j[emop])"
			+ "|k[eghimnprwyz]"
			+ "|l[abcikrstuvy]"
			+ "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
			+ "|(?:name|net|n[acefgilopruz])"
			+ "|(?:org|om)"
			+ "|(?:pro|p[aefghklmnrstwy])"
			+ "|qa"
			+ "|r[eosuw]"
			+ "|s[abcdeghijklmnortuvyz]"
			+ "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
			+ "|u[agksyz]"
			+ "|v[aceginu]"
			+ "|w[fs]"
			+ "|(?:xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-80akhbyknj4f|xn\\-\\-9t4b11yi5a|xn\\-\\-deba0ad|xn\\-\\-g6w251d|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-zckzah)"
			+ "|y[etu]"
			+ "|z[amw]))";

	private static final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

	/**
	 * Regular expression pattern to match most part of RFC 3987
	 * Internationalized URLs, aka IRIs. Commonly used Unicode characters are
	 * added.
	 */
	private static final Pattern WEB_URL = Pattern
			.compile("((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
					+ "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
					+ "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
					+ "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
					+ TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
					+ "|(?:(?:25[0-5]|2[0-4]" // or ip address
					+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
					+ "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
					+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
					+ "|[1-9][0-9]|[0-9])))"
					+ "(?:\\:\\d{1,5})?)" // plus option port number
					+ "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option QUERY params
					+ "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
					+ "(?:\\b|$)");
}