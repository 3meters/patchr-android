package com.aircandi.objects;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Post extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = 4362288672244719448L;
	public static final  String collectionId     = "posts";

	public static Post setPropertiesFromMap(Post entity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Post) Entity.setPropertiesFromMap(entity, map, nameMapping);
		}
		return entity;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}
}