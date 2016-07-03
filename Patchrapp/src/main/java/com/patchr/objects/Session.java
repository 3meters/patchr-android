package com.patchr.objects;

import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Session extends ServiceBase {

	private static final long   serialVersionUID = 127428776257201066L;

	public String key;

	/* Dates */

	public Number expirationDate;

	public static Session setPropertiesFromMap(Session session, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		session = (Session) ServiceBase.setPropertiesFromMap(session, map);
		session.key = (String) map.get("key");
		session.expirationDate = (Number) map.get("expirationDate");

		return session;
	}
}