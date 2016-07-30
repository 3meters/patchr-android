package com.patchr.objects.enums;

public class TriggerCategory {
	/*
	 * Used to characterize why the current user is receiving the notification.
	 * Used to enable/disable status notifications based on user preference settings.
	 */
	public static String NEARBY = "nearby";         // sent because this user is nearby
	public static String WATCH  = "watch";          // sent because this user is watching the target entity
	public static String OWN    = "own";            // sent because this user is the owner of the target entity
	public static String NONE   = "none";
}
