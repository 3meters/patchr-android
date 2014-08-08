package com.aircandi.utilities;

import android.text.TextUtils;

public class Type {

	public static String emptyAsNull(String stringValue) {
		if ("".equals(stringValue)) return null;
		return stringValue;
	}

	public static Boolean isTrue(Boolean value) {
		return (value != null && value);
	}

	public static Boolean isFalse(Boolean value) {
		return (value == null || !value);
	}

	public static Boolean equal(String value1, String value2) {
		if (TextUtils.isEmpty(value1) && TextUtils.isEmpty(value2)) return true;
		return (value1.equals(value2));
	}
}