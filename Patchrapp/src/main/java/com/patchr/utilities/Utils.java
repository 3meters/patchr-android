package com.patchr.utilities;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.UserManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class Utils {

	private static class URLSpanNoUnderline extends URLSpan {
		public URLSpanNoUnderline(String url) {
			super(url);
		}

		@Override public void updateDrawState(TextPaint ds) {
			super.updateDrawState(ds);
			ds.setUnderlineText(false);
		}
	}

	public static String encode(String target) {
		try {
			return URLEncoder.encode(target, "UTF-8");
		}
		catch (UnsupportedEncodingException e) { /* ignore */ }
		return null;
	}

	public static Boolean validEmail(@NonNull String email) {
		return EMAIL_ADDRESS.matcher(email).matches();
	}

	public static Boolean validWebUri(@NonNull String webUri) {
		return WEB_URL.matcher(webUri).matches();
	}

	public static String capitalize(String string) {
		return string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1).toLowerCase(Locale.US);
	}

	public static String getImageKey() {
		final String stringDate = DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME);
		final String imageKey = String.format("%1$s_%2$s.jpg", UserManager.userId, stringDate); // User id at root to avoid collisions
		return imageKey;
	}

	public static String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuilder hexString = new StringBuilder();
			for (byte aMessageDigest : messageDigest) {
				String h = Integer.toHexString(0xFF & aMessageDigest);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();
		}
		catch (NoSuchAlgorithmException e) { /* ignore */ }
		return "";
	}

	public static String getProcessName() {
		String currentProcName = "";
		int pid = android.os.Process.myPid();
		ActivityManager manager = (ActivityManager) Patchr.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
		if (infos != null) {
			for (ActivityManager.RunningAppProcessInfo info : infos) {
				if (info.pid == pid) {
					return  info.processName;
				}
			}
		}
		return null;
	}

	public static void stripUnderlines(TextView textView) {
		Spannable s = new SpannableString(textView.getText());
		URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
		for (URLSpan span : spans) {
			int start = s.getSpanStart(span);
			int end = s.getSpanEnd(span);
			s.removeSpan(span);
			span = new URLSpanNoUnderline(span.getURL());
			s.setSpan(span, start, end, 0);
		}
		textView.setText(s);
	}

	public static String initialsFromName(String fullname) {
		String[] words = fullname.split(" ");
		String initials = "";
		for (String word : words) {
			initials += word.charAt(0);
			if (initials.length() >= 2) {
				break;
			}
		}
		return initials;
	}

	public static Long numberFromName(String fullname) {
		Logger.v(Utils.class, "User name: " + fullname);
		Long accum = 0L;
		for (char character : fullname.toCharArray()) {
			accum += (int) character;
		}
		return accum;
	}

	public static String distanceFormatted(Float distance) {
		String info = "here";
		/*
		 * If distance = -1 then we don't have the location info
		 * yet needed to correctly determine distance.
		 */
		if (distance == null) {
			info = "--";
		}
		else {
			if (distance == -1f) { // $codepro.audit.disable floatComparison
				info = "--";
			}
			else {
				final float miles = distance * LocationManager.MetersToMilesConversion;
				final float feet = distance * LocationManager.MetersToFeetConversion;
				final float yards = distance * LocationManager.MetersToYardsConversion;

				if (feet >= 0) {
					if (miles >= 0.1) {
						info = String.format(Locale.US, "%.1f mi", miles);
					}
					else if (feet >= 50) {
						info = String.format(Locale.US, "%.0f yds", yards);
					}
					else {
						info = String.format(Locale.US, "%.0f ft", feet);
					}
				}
				if (feet <= 60) {
					info = "here";
				}
			}
		}
		return info;
	}

	public static Boolean devModeEnabled() {
		return (isDev() && Constants.DEV_ENABLED);
	}

	public static Boolean isDev() {
		return (UserManager.currentUser != null && UserManager.currentUser.developer != null && UserManager.currentUser.developer);
	}

	public static void guard(Boolean condition) {
		guard(condition, "Assertion guard failed");
	}

	public static void guard(Boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	public static int randomColor(long seed) {
		Random generator = new Random(seed);
		generator.nextFloat();
		float hue = generator.nextFloat() * 360.0f;
		float[] hsv = {hue, 0.7f, 0.8f};
		return Color.HSVToColor(hsv);
	}

	public static Long getMemoryAvailable() {
		MemoryInfo memoryInfo = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) Patchr.applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(memoryInfo);
		return memoryInfo.availMem / 1048576L;
	}

	@NonNull public static ScreenSize getScreenSize() {
		int screenLayout = Patchr.applicationContext.getResources().getConfiguration().screenLayout;
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

	public enum ScreenSize {
		SMALL,
		NORMAL,
		LARGE,
		XLARGE,
		UNDEFINED
	}

	public static int maxMemoryMB() {
		return (int) (Runtime.getRuntime().maxMemory() / Constants.SIZE_MEGABYTES);
	}

	public static int freeMemoryMB() {
		return (int) (Runtime.getRuntime().freeMemory() / Constants.SIZE_MEGABYTES);
	}

	public static int totalMemoryMB() {
		return (int) (Runtime.getRuntime().totalMemory() / Constants.SIZE_MEGABYTES);
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