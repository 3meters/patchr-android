package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.utilities.DateTime;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Message extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = 4362288672244719348L;
	public static final  String collectionId     = "messages";
	public static final  String schemaName       = "message";
	public static final  String schemaId         = "me";

	/*--------------------------------------------------------------------------------------------
	 * Service fields
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Message setPropertiesFromMap(Message entity, Map map, Boolean nameMapping) {
	    /*
	     * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Message) Entity.setPropertiesFromMap(entity, map, nameMapping);
		}
		return entity;
	}

	public static Entity build() {
		Message entity = new Message();
		entity.schema = Constants.SCHEMA_ENTITY_MESSAGE;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		return entity;
	}

	@NonNull public Boolean isOwnerAccess() {
		return true;
	}

	@Override public Message clone() {
		final Message clone = (Message) super.clone();
		return clone;
	}

	@Override public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class MessageType {
		public static String ROOT  = "root";
		public static String SHARE = "share";
	}
}