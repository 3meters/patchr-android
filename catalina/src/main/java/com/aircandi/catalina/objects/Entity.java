package com.aircandi.catalina.objects;

import com.aircandi.catalina.Constants;

public abstract class Entity extends com.aircandi.objects.Entity {

	public static String getSchemaForId(String id) {
		String prefix = id.substring(0, 2);
		if (prefix.equals("me")) {
			return Constants.SCHEMA_ENTITY_MESSAGE;
		}
		else {
			return com.aircandi.objects.Entity.getSchemaForId(id);
		}
	}
}
