package com.patchr.utilities;

import android.support.annotation.NonNull;
import android.text.TextUtils;

;

public class Type {

	public static String emptyAsNull(String stringValue) {
		if ("".equals(stringValue)) return null;
		return stringValue;
	}

	@NonNull
	public static Boolean isTrue(Boolean value) {
		return (value != null && value);
	}

	@NonNull
	public static Boolean isFalse(Boolean value) {
		//noinspection PointlessBooleanExpression
		return (value == null || value == false);
	}

	public static Boolean equal(String value1, String value2) {
		return  (TextUtils.isEmpty(value1) && TextUtils.isEmpty(value2) || (value1 != null && value2 != null && value1.equals(value2)));
	}
}