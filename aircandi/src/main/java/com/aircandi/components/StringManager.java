package com.aircandi.components;

import android.content.Context;
import android.content.res.Resources;

import com.aircandi.Aircandi;
import com.aircandi.objects.Entity;

@SuppressWarnings("ucd")
public class StringManager {

	private StringManager() {}

	private static class Holder {
		public static final StringManager	instance	= new StringManager();
	}

	public static StringManager getInstance() {
		return Holder.instance;
	}

	public static String getString(int resId) {
		String string = Aircandi.applicationContext.getResources().getString(resId);
		return getString(string, Aircandi.applicationContext, Aircandi.applicationContext.getResources(), null);
	}

	public static String getString(String string) {
		return getString(string, Aircandi.applicationContext, Aircandi.applicationContext.getResources(), null);
	}

	public static String getString(int resId, Context context, Resources resources) {
		String string = Aircandi.applicationContext.getResources().getString(resId);
		return getString(string, context, resources, null);
	}

	/*
	 * Overloads with schema parameter
	 */

	public static String getString(int resId, String schema) {
		String string = Aircandi.applicationContext.getResources().getString(resId);
		return getString(string, Aircandi.applicationContext, Aircandi.applicationContext.getResources(), schema);
	}

	public static String getString(String string, String schema) {
		return getString(string, Aircandi.applicationContext, Aircandi.applicationContext.getResources(), schema);
	}

	public static String getString(int resId, Context context, Resources resources, String schema) {
		String string = Aircandi.applicationContext.getResources().getString(resId);
		return getString(string, context, resources, schema);
	}

	public static String getString(String string, Context context, Resources resources, String schema) {

		final StringBuilder stringBuilder = new StringBuilder(string);
		final String packageName = context.getPackageName();

		int start;

		while ((start = stringBuilder.indexOf("[@string/")) != -1) {
			int end = stringBuilder.indexOf("]", start);
			String tokenResName = stringBuilder.substring(start + 2, end);
			int tokenResId = resources.getIdentifier(tokenResName, null, packageName);
			if (tokenResId == 0) throw new IllegalArgumentException("Failed to resolve link to @" + tokenResName);
			stringBuilder.replace(start, end + 1, resources.getString(tokenResId));
		}

		String outputString = stringBuilder.toString();

		if (schema != null) {
			String label = Entity.getLabelForSchema(schema);
			return outputString.replace("[@schema]", label);
		}
		else {
			return outputString;
		}
	}
}
