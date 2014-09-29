package com.aircandi.objects;

/**
 * Created by Jayma on 9/29/2014.
 */
public class MessageTriggerType {
	public static class TriggerType {
		public static String NEARBY     = "nearby";        // sent because this user is nearby
		public static String WATCH      = "watch";         // sent because this user is watching the entity
		public static String WATCH_TO   = "watch_to";      // sent because this user is watching the 'to' entity
		public static String WATCH_USER = "watch_user";    // sent because this user is watching another user
		public static String OWN        = "own";           // sent because this user is the owner of the entity
		public static String OWN_TO     = "own_to";        // sent because this user is the owner of the 'to' entity
		public static String NONE       = "none";          // sent without a special reason
	}
}
