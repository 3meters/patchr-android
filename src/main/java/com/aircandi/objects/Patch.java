package com.aircandi.objects;

import android.support.annotation.NonNull;

import com.aircandi.Constants;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

import java.io.Serializable;
import java.util.Comparator;
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
	public Boolean locked = false;
	@Expose
	@SerializedName(name = "visibility")
	public String privacy;                                    // private|public|hidden

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public Boolean isVisibleToCurrentUser() {
		if (privacy != null && !privacy.equals(Constants.PRIVACY_PUBLIC) && !isOwnedByCurrentUser()) {
			Link link = linkFromAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null || !link.enabled) {
				return false;
			}
		}
		return true;
	}

	@NonNull
	public Boolean isRestricted() {
		return (privacy != null && !privacy.equals(Constants.PRIVACY_PUBLIC));
	}

	@NonNull
	public Boolean isRestrictedForCurrentUser() {
		return (privacy != null && !privacy.equals(Constants.PRIVACY_PUBLIC) && !isOwnedByCurrentUser());
	}

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

	public static Patch setPropertiesFromMap(Patch patch, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		patch = (Patch) Entity.setPropertiesFromMap(patch, map, nameMapping);
		patch.locked = (Boolean) ((map.get("locked") != null) ? map.get("locked") : false);
		patch.privacy = (String) (nameMapping ? map.get("visibility") : map.get("privacy"));

		return patch;
	}

	@Override
	public Patch clone() {
		final Patch patch = (Patch) super.clone();
		return patch;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortByProximityAndDistance implements Comparator<Entity> {

		@Override
		public int compare(@NonNull Entity object1, @NonNull Entity object2) {

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

	@SuppressWarnings("ucd")
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