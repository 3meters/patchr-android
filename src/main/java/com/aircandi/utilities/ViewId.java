package com.aircandi.utilities;

import android.support.annotation.NonNull;

import java.util.concurrent.atomic.AtomicInteger;

public class ViewId {
	@NonNull
	private static ViewId INSTANCE = new ViewId();

	private AtomicInteger seq;

	private ViewId() {
		seq = new AtomicInteger(Integer.MAX_VALUE);
	}

	public int getUniqueId() {
		return seq.decrementAndGet();
	}

	@NonNull
	public static ViewId getInstance() {
		return INSTANCE;
	}
}
