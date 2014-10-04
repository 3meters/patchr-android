package com.aircandi.objects;

public class MessageTriggers {

	public static class TriggerType {
		public static String NEARBY = "nearby";        // sent because this user is nearby
		public static String WATCH_TO  = "watch_to";      // sent because this user is watching the 'to' entity
		public static String OWN_TO = "own_to";        // sent because this user is the owner of the 'to' entity
		public static String NONE   = "none";          // sent without a special reason
	}

	public static class TriggerCategory {
		public static String NEARBY = "nearby";
		public static String WATCH  = "watch";
		public static String OWN    = "own";
		public static String NONE   = "none";
	}
}
