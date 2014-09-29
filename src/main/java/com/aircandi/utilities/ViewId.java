package com.aircandi.utilities;

import java.util.concurrent.atomic.AtomicInteger;

public class ViewId {
	private static ViewId INSTANCE = new ViewId();

	private AtomicInteger seq;

	private ViewId() {
		seq = new AtomicInteger(Integer.MAX_VALUE);
	}

	public int getUniqueId() {
		return seq.decrementAndGet();
	}

	public static ViewId getInstance() {
		return INSTANCE;
	}
}
