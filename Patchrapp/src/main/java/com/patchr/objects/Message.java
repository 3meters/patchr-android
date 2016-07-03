package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.utilities.DateTime;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Message extends Entity implements Cloneable, Serializable {

	private static final long serialVersionUID = 4362288672244719348L;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Message setPropertiesFromMap(Message entity, Map map) {
	    /*
	     * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Message) Entity.setPropertiesFromMap(entity, map);
		}
		return entity;
	}

	public static Entity build() {
		Message entity = new Message();
		entity.schema = Constants.SCHEMA_ENTITY_MESSAGE;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		return entity;
	}

	@Override public Message clone() {
		final Message clone = (Message) super.clone();
		return clone;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class MessageType {
		public static String Post   = "post";
		public static String Share  = "share";
		public static String Invite = "invite";
	}
}