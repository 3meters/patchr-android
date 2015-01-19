package com.aircandi.utilities;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ucd")
public class Maps {
	@NonNull
	public static final <K, V> HashMap<K, V> asHashMap(@NonNull K[] keys, @NonNull V[] values) {
		HashMap<K, V> result = new HashMap<K, V>();
		if (keys.length != values.length) throw new IllegalArgumentException();

		for (int i = 0; i < keys.length; i++) {
			result.put(keys[i], values[i]);
		}
		return result;
	}

	@NonNull
	public static final <K, V> HashMap<K, V> asHashMap(K key, V value) {
		HashMap<K, V> result = new HashMap<K, V>();
		result.put(key, value);
		return result;
	}

	@NonNull
	public static final <K, V> Map<K, V> asMap(@NonNull K[] keys, @NonNull V[] values) {
		return asHashMap(keys, values);
	}

	@NonNull
	public static final <K, V> Map<K, V> asMap(K key, V value) {
		return asHashMap(key, value);
	}
}
