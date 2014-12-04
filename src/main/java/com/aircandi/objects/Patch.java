package com.aircandi.objects;

import android.support.annotation.Nullable;

import com.aircandi.Constants;
import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Patch extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = -3599862145425838670L;
	public static final  String collectionId     = "patches";
	public static final  String schemaName       = "patch";
	public static final  String schemaId         = "pa";

	/*--------------------------------------------------------------------------------------------
	 * service fields
	 *--------------------------------------------------------------------------------------------*/
	@Expose
	public Category category;
	@Expose
	public Number   signalFence;

	/*--------------------------------------------------------------------------------------------
	 * client fields (NONE are transferred)
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public String getBeaconId() {
		final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) return beacon.id;
		return null;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static Patch setPropertiesFromMap(Patch entity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entity = (Patch) Entity.setPropertiesFromMap(entity, map, nameMapping);
		if (map.get("category") != null) {
			entity.category = Category.setPropertiesFromMap(new Category(), (HashMap<String, Object>) map.get("category"), nameMapping);
		}

		return entity;
	}

	@Override
	@Nullable
	public Patch clone() {
		final Patch place = (Patch) super.clone();
		if (place != null) {
			if (category != null) {
				place.category = category.clone();
			}
		}
		return place;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortByProximityAndDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.hasActiveProximity() && !object2.hasActiveProximity())
				return -1;
			else if (object2.hasActiveProximity() && !object1.hasActiveProximity())
				return 1;
			else {
				/*
				 * Ordering
				 * 1. has distance
				 * 2. distance is null
				 */
				if (object1.distance == null && object2.distance == null)
					return 0;
				else if (object1.distance == null && object2.distance != null)
					return 1;
				else if (object2.distance == null && object1.distance != null)
					return -1;
				else if (object1.distance != null && object1.distance.intValue() < object2.distance.intValue())
					return -1;
				else if (object1.distance != null && object1.distance.intValue() > object2.distance.intValue())
					return 1;
				else
					return 0;
			}
		}
	}

	@SuppressWarnings("ucd")
	public static class ReasonType {
		public static String WATCH    = "watch";
		public static String LOCATION = "location";
		public static String RECENT   = "recent";
		public static String OTHER    = "other";
	}
}