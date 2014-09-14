package com.aircandi.objects;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Comment extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = 4362288672244729448L;
	public static final  String collectionId     = "comments";

	public static Comment setPropertiesFromMap(Comment entity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Comment) Entity.setPropertiesFromMap(entity, map, nameMapping);
		}
		return entity;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}
}