package com.aircandi.objects;

import android.support.annotation.NonNull;

;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class SimpleEntity extends Entity implements Cloneable, Serializable {

	private static final long serialVersionUID = 4362288672244729448L;

	public static Entity setPropertiesFromMap(Entity entity, @NonNull Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Entity) Entity.setPropertiesFromMap(entity, map, nameMapping);
		}
		return entity;
	}


	@Override
	public String getCollection() {
		return null;
	}
}