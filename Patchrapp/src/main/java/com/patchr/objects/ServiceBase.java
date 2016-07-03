package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.utilities.Reporting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/*
 * Initial user settings (owner,creator):
 * 
 * entities: user, user
 * links: user, user
 * sessions: user, user
 * users: user, admin
 * 
 * beacons: admin, user
 * documents: admin, admin
 * observations: admin, user
 */

@SuppressWarnings("ucd")
public abstract class ServiceBase extends ServiceObject {

	private static final long serialVersionUID = -3650173415935365107L;

	public String id;
	public String schema;
	public String type;
	public String name;
	public String namelc;
	public Number position;

	/* PropertyValue bags */

	public Map<String, Object> data;

	/* user ids */

	public String ownerId;
	public String creatorId;
	public String modifierId;

	/* Dates */

	public Number createdDate;
	public Number modifiedDate;
	public Number activityDate;
	public Number sortDate;

	/* Users (synthesized for the client) */

	public User owner;
	public User creator;
	public User modifier;

	protected ServiceBase() {}

	/*--------------------------------------------------------------------------------------------
	 * Set and get
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static ServiceBase setPropertiesFromMap(ServiceBase base, Map map) {

		base.id = (String) (map.get("_id") != null ? map.get("_id") : map.get("id"));
		base.name = (String) map.get("name");
		base.namelc = (String) map.get("namelc");
		base.schema = (String) map.get("schema");
		base.type = (String) map.get("type");
		base.data = (HashMap<String, Object>) map.get("data");

		base.ownerId = (String) (map.get("_owner") != null ? map.get("_owner") : map.get("ownerId"));
		base.creatorId = (String) (map.get("_creator") != null ? map.get("_creator") : map.get("creatorId"));
		base.modifierId = (String) (map.get("_modifier") != null ? map.get("_modifier") : map.get("modifierId"));

		base.createdDate = (Number) map.get("createdDate");
		base.modifiedDate = (Number) map.get("modifiedDate");
		base.activityDate = (Number) map.get("activityDate");
		base.sortDate = (Number) map.get("sortDate");

		if (base.activityDate == null && base.createdDate != null) {
			base.activityDate = base.createdDate.longValue(); // Service doesn't set activityDate until there is activity
		}

		if (map.get("creator") != null) {
			base.creator = User.setPropertiesFromMap(new User(), (Map<String, Object>) map.get("creator"));
		}
		if (map.get("owner") != null) {
			base.owner = User.setPropertiesFromMap(new User(), (Map<String, Object>) map.get("owner"));
		}
		if (map.get("modifier") != null) {
			base.modifier = User.setPropertiesFromMap(new User(), (Map<String, Object>) map.get("modifier"));
		}

		return base;
	}

	@Override
	public ServiceBase clone() {
		@SuppressWarnings("UnusedAssignment") ServiceBase entry = null;
		try {
			entry = (ServiceBase) super.clone();
			if (owner != null) {
				entry.owner = owner.clone();
			}
			if (creator != null) {
				entry.creator = creator.clone();
			}
			if (modifier != null) {
				entry.modifier = modifier.clone();
			}
			if (data != null) {
				entry.data = new HashMap(data);
			}
		}
		catch (CloneNotSupportedException e) {
			Reporting.logException(e);
		}
		return entry;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortBySortDate implements Comparator<ServiceBase> {

		@Override
		public int compare(@NonNull ServiceBase object1, @NonNull ServiceBase object2) {
			if (object1.sortDate == null || object2.sortDate == null)
				return 0;
			else {
				if (object1.sortDate.longValue() < object2.sortDate.longValue())
					return 1;
				else if (object1.sortDate.longValue() == object2.sortDate.longValue())
					return 0;
				return -1;
			}
		}
	}

	public static class SortBySortDateAscending implements Comparator<ServiceBase> {

		@Override
		public int compare(@NonNull ServiceBase object1, @NonNull ServiceBase object2) {
			if (object1.sortDate == null || object2.sortDate == null)
				return 0;
			else {
				if (object1.sortDate.longValue() > object2.sortDate.longValue())
					return 1;
				else if (object1.sortDate.longValue() == object2.sortDate.longValue())
					return 0;
				return -1;
			}
		}
	}

	/**
	 * object: All properties are serialized including nulls.</br>
	 * PropertyValue: Only non-null properties are serialized.</br>
	 *
	 * @author Jayma
	 */
	@SuppressWarnings("ucd")
	public static enum UpdateScope {
		OBJECT,
		PROPERTY
	}
}
