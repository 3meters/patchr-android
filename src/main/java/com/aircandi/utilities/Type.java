package com.aircandi.utilities;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.jetbrains.annotations.Nullable;

public class Type {

	@Nullable
	public static String emptyAsNull(String stringValue) {
		if ("".equals(stringValue)) return null;
		return stringValue;
	}

	@Nullable
	public static Boolean isTrue(@Nullable Boolean value) {
		return (value != null && value);
	}

	@Nullable
	public static Boolean isFalse(@Nullable Boolean value) {
		return (value == null || !value);
	}

	public static Boolean equal(@NonNull String value1, String value2) {
		if (TextUtils.isEmpty(value1) && TextUtils.isEmpty(value2)) return true;
		return (value1.equals(value2));
	}
}