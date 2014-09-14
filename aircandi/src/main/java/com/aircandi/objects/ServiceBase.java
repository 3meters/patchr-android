package com.aircandi.objects;

import com.aircandi.ServiceConstants;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

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
	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 * 
	 * These annotations are only used when serializing. Each
	 * object has a setPropertiesFromMap method that handles how properties
	 * are deserialized.
	 */
	@Expose
	@SerializedName(name = "_id")
	public String id;
	@Expose
	public String schema;
	@Expose
	public String type;
	@Expose
	public String name;
	@Expose
	public Boolean locked = false;
	@Expose
	public Number position;

	@Expose(serialize = false, deserialize = true)
	public String namelc;

	@Expose(serialize = false, deserialize = true)
	public Boolean enabled = true;                    // promoted from link for convenience

	/* PropertyValue bags */

	@Expose
	public Map<String, Object> data;

	/* user ids */

	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_owner")
	public String ownerId;
	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_creator")
	public String creatorId;
	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_modifier")
	public String modifierId;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number createdDate;
	@Expose(serialize = false, deserialize = true)
	public Number modifiedDate;
	@Expose(serialize = false, deserialize = true)
	public Number activityDate;
	@Expose(serialize = false, deserialize = true)
	public Number sortDate;

	/* Users (synthesized for the client) */

	@Expose(serialize = false, deserialize = true)
	public User owner;
	@Expose(serialize = false, deserialize = true)
	public User creator;
	@Expose(serialize = false, deserialize = true)
	public User modifier;

	protected ServiceBase() {
	}

	/*--------------------------------------------------------------------------------------------
	 * Set and get
	 *--------------------------------------------------------------------------------------------*/
	public String getEntryUri() {
		final String root = ServiceConstants.URL_PROXIBASE_SERVICE_REST;
		final String entity = getCollection();
		final String uri = root + entity + "/" + id;
		return uri;
	}

	public abstract String getCollection();

	public Integer getPosition() {
		return (position != null) ? position.intValue() : 0;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/
	public static ServiceBase setPropertiesFromMap(ServiceBase base, Map map, Boolean nameMapping) {

		base.id = (String) (nameMapping ? (map.get("_id") != null) ? map.get("_id") : map.get("id") : map.get("id"));
		base.name = (String) map.get("name");
		base.schema = (String) map.get("schema");
		base.type = (String) map.get("type");
		base.enabled = (Boolean) map.get("enabled");
		base.locked = (Boolean) map.get("locked");
		base.position = (Number) map.get("position");
		base.data = (HashMap<String, Object>) map.get("data");

		base.ownerId = (String) (nameMapping ? (map.get("_owner") != null) ? map.get("_owner") : map.get("ownerId") : map.get("ownerId"));
		base.creatorId = (String) (nameMapping ? (map.get("_creator") != null) ? map.get("_creator") : map.get("creatorId") : map.get("creatorId"));
		base.modifierId = (String) (nameMapping ? (map.get("_modifier") != null) ? map.get("_modifier") : map.get("modifierId") : map.get("modifierId"));

		base.createdDate = (Number) map.get("createdDate");
		base.modifiedDate = (Number) map.get("modifiedDate");
		base.activityDate = (Number) map.get("activityDate");
		base.sortDate = (Number) map.get("sortDate");

		if (map.get("creator") != null) {
			base.creator = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("creator"), nameMapping);
		}
		if (map.get("owner") != null) {
			base.owner = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("owner"), nameMapping);
		}
		if (map.get("modifier") != null) {
			base.modifier = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("modifier"), nameMapping);
		}

		return base;
	}

	@Override
	public ServiceBase clone() {
		ServiceBase entry = null;
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
		catch (CloneNotSupportedException exception) {
			exception.printStackTrace();
		}
		return entry;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortByPositionSortDate implements Comparator<ServiceBase> {

		@Override
		public int compare(ServiceBase object1, ServiceBase object2) {
			if (object1.getPosition().intValue() < object2.getPosition().intValue())
				return -1;
			else if (object1.getPosition().intValue() == object2.getPosition().intValue()) {
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
			return 1;
		}
	}

	public static class SortByPositionSortDateAscending implements Comparator<ServiceBase> {

		@Override
		public int compare(ServiceBase object1, ServiceBase object2) {
			if (object1.getPosition().intValue() < object2.getPosition().intValue())
				return -1;
			else if (object1.getPosition().intValue() == object2.getPosition().intValue()) {
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
			return 1;
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