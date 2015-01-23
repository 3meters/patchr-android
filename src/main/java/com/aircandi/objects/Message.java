package com.aircandi.objects;

import android.support.annotation.NonNull;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

import java.io.Serializable;
import java.util.HashMap;
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

	@Expose
	@SerializedName(name = "_root")
	public String rootId;
	@Expose
	@SerializedName(name = "_replyTo")
	public String replyToId;

	/* Reply to user (synthesized for the client) */

	@Expose(serialize = false, deserialize = true)
	public User replyTo;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Message setPropertiesFromMap(Message entity, Map map, Boolean nameMapping) {
	    /*
	     * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Message) Entity.setPropertiesFromMap(entity, map, nameMapping);

			entity.rootId = (String) (nameMapping ? map.get("_root") : map.get("rootId"));
			entity.replyToId = (String) (nameMapping ? map.get("_replyTo") : map.get("replyToId"));

			if (map.get("replyTo") != null) {
				entity.replyTo = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("replyTo"), nameMapping);
			}
		}
		return entity;
	}

	@NonNull
	public Boolean isOwnerAccess() {
		return true;
	}

	@Override
	public Message clone() {
		final Message clone = (Message) super.clone();
		return clone;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class MessageType {
		public static String ROOT  = "root";
		public static String REPLY = "reply";
		public static String SHARE = "share";
		public static String ALERT = "alert";
	}
}