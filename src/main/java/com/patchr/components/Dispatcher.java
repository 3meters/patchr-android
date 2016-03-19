package com.patchr.components;

import org.greenrobot.eventbus.EventBus;

public class Dispatcher {

	private static class BusHolder {
		public static final EventBus instance = EventBus.getDefault();
	}

	public static EventBus getInstance() {
		return BusHolder.instance;
	}

	private Dispatcher() {}
}
