package com.patchr.components;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class Dispatcher {

	private static class BusHolder {
		public static final Bus instance = new Bus(ThreadEnforcer.ANY);
	}

	public static Bus getInstance() {
		return BusHolder.instance;
	}

	private Dispatcher() {}
}