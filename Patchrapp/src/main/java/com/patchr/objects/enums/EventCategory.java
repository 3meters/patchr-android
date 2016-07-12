package com.patchr.objects.enums;

/**
 * Created by jaymassena on 7/10/16.
 */
public class EventCategory {
	/*
	 * Used to characterize the action associated with the notification.
	 * Used to enable/disable status notifications based on user preference settings.
	 */
	public static String INSERT = "insert";         // notification about a patch|message insert
	public static String SHARE  = "share";          // notification about patch|message|photo share
	public static String LIKE   = "like";           // notification about patch|message like
	public static String WATCH  = "watch";          // notification about patch watch
	public static String NONE   = "none";
}
