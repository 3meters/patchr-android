package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.utilities.DateTime;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public class Patch extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = -3599862145425838670L;

	/*--------------------------------------------------------------------------------------------
	 * service fields
	 *--------------------------------------------------------------------------------------------*/

	public String  visibility;                                    // private|public|hidden
	public Boolean locked;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public Boolean isVisibleToCurrentUser() {
		if (visibility != null && !visibility.equals(Constants.PRIVACY_PUBLIC) && !isOwnedByCurrentUser()) {
			LinkOld link = linkFromAppUser(Constants.TYPE_LINK_MEMBER);
			if (link == null || !link.enabled) {
				return false;
			}
		}
		return true;
	}

	public Boolean isRestricted() {
		return (visibility != null && !visibility.equals(Constants.PRIVACY_PUBLIC));
	}

	public Boolean isRestrictedForCurrentUser() {
		return (visibility != null && !visibility.equals(Constants.PRIVACY_PUBLIC) && !isOwnedByCurrentUser());
	}

	public String getBeaconId() {
		final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) return beacon.id;
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static Patch setPropertiesFromMap(Patch patch, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		patch = (Patch) Entity.setPropertiesFromMap(patch, map);
		patch.visibility = (String) (map.get("visibility") != null ? map.get("visibility") : map.get("privacy"));
		patch.locked = (Boolean) map.get("locked");

		return patch;
	}

	public static Entity build() {
		Patch entity = new Patch();
		entity.schema = Constants.SCHEMA_ENTITY_PATCH;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.visibility = Constants.PRIVACY_PUBLIC;
		return entity;
	}

	@Override public Patch clone() {
		final Patch patch = (Patch) super.clone();
		return patch;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortByProximityAndDistance implements Comparator<Entity> {

		@Override public int compare(@NonNull Entity object1, @NonNull Entity object2) {

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
				else if (object1.distance == null)
					return 1;
				else if (object2.distance == null)
					return -1;
				else {
					if (object1.distance.intValue() < object2.distance.intValue())
						return -1;
					else if (object1.distance.intValue() > object2.distance.intValue())
						return 1;
					else
						return 0;
				}
			}
		}
	}

	public static class ReasonType {
		public static String WATCH    = "watch";
		public static String LOCATION = "location";
		public static String RECENT   = "recent";
		public static String OTHER    = "other";
	}

	public static class Type {
		public static String EVENT   = "event";
		public static String GROUP   = "group";
		public static String PLACE   = "place";
		public static String PROJECT = "project";
		public static String OTHER   = "other";
	}
}