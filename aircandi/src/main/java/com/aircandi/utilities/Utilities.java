package com.aircandi.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.components.Logger;

public class Utilities {

	@SuppressWarnings("ucd")
	public static final String md5(final String s) {
		try {
			/* Create MD5 Hash */
			final MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			final byte[] messageDigest = digest.digest();

			/* Create Hex String */
			final StringBuffer hexString = new StringBuffer(500);
			final StringBuilder hex = new StringBuilder(500);
			for (int i = 0; i < messageDigest.length; i++) {
				hex.setLength(0);
				hex.append(Integer.toHexString(0xFF & messageDigest[i]));
				while (hex.length() < 2) {
					hex.insert(0, "0");
				}
				hexString.append(hex);
			}
			return hexString.toString();

		}
		catch (NoSuchAlgorithmException e) {
			if (Aircandi.DEBUG) {
				e.printStackTrace();
			}
		}
		return "";
	}

	@SuppressWarnings("ucd")
	public static final int random(int min, int max) {
		final Random random = new Random();
		final int i1 = random.nextInt(max - min + 1) + min;
		return i1;
	}

	public static Boolean validEmail(String email) {
		return EMAIL_ADDRESS.matcher(email).matches();
	}

	public static Boolean validWebUri(String webUri) {
		return WEB_URL.matcher(webUri).matches();
	}

	@SuppressWarnings("ucd")
	public static String loadStringFromRaw(Integer resId) {
		InputStream inputStream = null;
		BufferedReader reader = null;
		try {
			inputStream = Aircandi.applicationContext.getResources().openRawResource(resId);
			reader = new BufferedReader(new InputStreamReader(inputStream));
			final StringBuilder text = new StringBuilder(10000);
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line);
			}
			return text.toString();
		}
		catch (IOException exception) {
			return null;
		}
		finally {
			try {
				inputStream.close();
				reader.close();
			}
			catch (IOException e) {
				if (Aircandi.DEBUG) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("ucd")
	public static void writeStringToFile(String fileName, String data, Context context) {

		File root = Environment.getExternalStorageDirectory();
		File outDir = new File(root.getAbsolutePath() + File.separator + "aircandi");
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		try {
			if (!outDir.isDirectory()) throw new IOException("Unable to create directory aircandi. Maybe the SD card is mounted?");
			File outputFile = new File(outDir, fileName);
			Writer writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(data);
			Logger.d(root, "Report successfully saved to: " + outputFile.getAbsolutePath());
			UI.showToastNotification("Report successfully saved to: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG);
			writer.close();
		}
		catch (IOException e) {
			Logger.w(root, e.getMessage());
		}
	}

	public static Long getMemoryAvailable() {
		MemoryInfo memoryInfo = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) Aircandi.applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(memoryInfo);
		long availableMemory = memoryInfo.availMem / 1048576L;
		return availableMemory;
	}

	@SuppressWarnings("ucd")
	public static ScreenSize getScreenSize() {
		int screenLayout = Aircandi.applicationContext.getResources().getConfiguration().screenLayout;
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

	@SuppressLint("InlinedApi")
	public static int calculateMemoryCacheSize(Context context) {
		/*
		 * Get memory class of this device, exceeding this amount will throw an
		 * OutOfMemory exception.
		 */
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int memClass = am.getMemoryClass();
		
		if (Constants.SUPPORTS_HONEYCOMB) {
			final boolean largeHeap = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
			if (largeHeap) {
				memClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);
			}
		}

		Logger.i(context, "Device memory class: " + String.valueOf(memClass));
		Aircandi.memoryClass = memClass;

		/* Use 1/4th of the available memory for this memory cache. */
		final int cacheSize = (memClass << 10 << 10) >> 2;
		Logger.i(context, "Memory cache size: " + String.valueOf(cacheSize));

		return cacheSize;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static class ActivityManagerHoneycomb {
		static int getLargeMemoryClass(ActivityManager activityManager) {
			return activityManager.getLargeMemoryClass();
		}
	}

	private static final Pattern	EMAIL_ADDRESS						= Pattern.compile(
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
	private static final String		TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL	= "(?:"
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
	private static final String		GOOD_IRI_CHAR						= "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

	/**
	 * Regular expression pattern to match most part of RFC 3987
	 * Internationalized URLs, aka IRIs. Commonly used Unicode characters are
	 * added.
	 */
	private static final Pattern	WEB_URL								= Pattern
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
																						+ "(?:\\b|$)");						// and finally, a word boundary or end of
																																// input.  This is to stop foo.sure from
																																// matching as foo.su

}