package com.aircandi.ui.components;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jayma on 9/25/2014.
 */
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
