package com.patchr.components;

import com.google.android.gms.tagmanager.ContainerHolder;

public class ContainerManager {
	private static ContainerHolder containerHolder;

	/**
	 * Utility class; don't instantiate.
	 */
	private ContainerManager() {}

	public static ContainerHolder getContainerHolder() {
		return containerHolder;
	}

	public static void setContainerHolder(ContainerHolder c) {
		containerHolder = c;
	}
}
