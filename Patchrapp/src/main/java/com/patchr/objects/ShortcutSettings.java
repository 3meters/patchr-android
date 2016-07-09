package com.patchr.objects;

import com.patchr.objects.LinkOld.Direction;

/**
 * @author Jayma
 */
public class ShortcutSettings {

	public String    linkType;
	public String    linkTargetSchema;
	public Direction direction;
	public Boolean synthetic    = false;
	public Boolean groupedByApp = false;
	public Boolean linkBroken   = false;
	public Class<?> appClass;

	public ShortcutSettings() {
	}

	public ShortcutSettings(String linkType, String linkTargetSchema, Direction direction, Boolean synthetic, Boolean groupedByApp) {
		this.linkType = linkType;
		this.linkTargetSchema = linkTargetSchema;
		this.direction = direction;
		this.synthetic = synthetic;
		this.groupedByApp = groupedByApp;
	}
}