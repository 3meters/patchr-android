package com.aircandi.objects;

import com.aircandi.objects.Shortcut.InstallStatus;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ShortcutMeta {

	public InstallStatus installStatus = InstallStatus.NONE;

	public ShortcutMeta(InstallStatus installStatus) {
		this.installStatus = installStatus;
	}

}