package com.patchr.components;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.patchr.Patchr;

@SuppressWarnings("ucd")
public class StringManager {

	private static final StringBuilder stringBuilder = new StringBuilder();

	private static class Holder {
		public static final StringManager instance = new StringManager();
	}

	public static StringManager getInstance() {
		return Holder.instance;
	}

	private StringManager() {}

	@NonNull public static String getString(int resId) {
		return getString(Patchr.applicationContext.getResources().getString(resId)
			, Patchr.applicationContext
			, Patchr.applicationContext.getResources()
			, null);
	}

	@NonNull public static String getString(String string) {
		return getString(string
			, Patchr.applicationContext
			, Patchr.applicationContext.getResources()
			, null);
	}

	@NonNull public static String getString(int resId, Context context, Resources resources) {
		return getString(Patchr.applicationContext.getResources().getString(resId)
			, context
			, resources
			, null);
	}
	/*
	 * Overloads with schema parameter
	 */
	@NonNull public static String getString(int resId, String schema) {
		return getString(Patchr.applicationContext.getResources().getString(resId)
			, Patchr.applicationContext
			, Patchr.applicationContext.getResources()
			, schema);
	}

	@NonNull public static String getString(String string, String schema) {
		return getString(string
			, Patchr.applicationContext
			, Patchr.applicationContext.getResources()
			, schema);
	}

	@NonNull public static String getString(int resId, Context context, Resources resources, String schema) {
		return getString(Patchr.applicationContext.getResources().getString(resId)
			, context
			, resources
			, schema);
	}

	@NonNull public static synchronized String getString(String string, Context context, Resources resources, String schema) {

		stringBuilder.setLength(0);
		stringBuilder.trimToSize();
		stringBuilder.append(string);

		int start;

		while ((start = stringBuilder.indexOf("[@string/")) != -1) {
			int end = stringBuilder.indexOf("]", start);
			String tokenResName = stringBuilder.substring(start + 2, end);
			int tokenResId = resources.getIdentifier(tokenResName, null, context.getPackageName());
			if (tokenResId == 0)
				throw new IllegalArgumentException("Failed to resolve link to @" + tokenResName);
			stringBuilder.replace(start, end + 1, resources.getString(tokenResId));
		}

		return stringBuilder.toString();
	}
}
