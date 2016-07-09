package com.patchr.objects;

import android.content.Intent;

import com.patchr.Constants;
import com.patchr.objects.PhotoOld.PhotoSource;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Shortcut extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 4979315562693226461L;

	public String      id;
	public String      name;
	public String      subtitle;
	public String      description;
	public String      schema;
	public String      app;                                                                                    // usually maps to type property (if exists) on polymorphic entity like applinks
	public String      appId;
	public String      appUrl;
	public Number      validatedDate;
	public Number      position;
	public PhotoOld    photo;
	public LocationOld location;
	public Boolean     content;
	public String      action;
	public Number      sortDate;

	/* Users (synthesized for the client) */

	public User creator;

	/* client only properties */

	public Integer        count;
	public List<Shortcut> group;
	public String         linkType;                // so we know if entity shortcut represents is targeted via like/watch/content etc.
	public Intent         intent;

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static Shortcut setPropertiesFromMap(Shortcut shortcut, Map map) {
		/*
		 * Need to include any properties that need to survive encode/decoded between activities.
		 */
		shortcut.id = (String) map.get("id");
		shortcut.name = (String) map.get("name");
		shortcut.subtitle = (String) map.get("subtitle");
		shortcut.description = (String) map.get("description");
		shortcut.sortDate = (Number) map.get("sortDate");
		shortcut.schema = (String) map.get("schema");
		shortcut.app = (String) map.get("app");
		shortcut.appId = (String) map.get("appId");
		shortcut.appUrl = (String) map.get("appUrl");
		shortcut.validatedDate = (Number) map.get("validatedDate");
		shortcut.position = (Number) map.get("position");
		shortcut.content = (Boolean) map.get("content");
		shortcut.action = (String) map.get("action");

		if (map.get("photo") != null) {
			shortcut.photo = PhotoOld.setPropertiesFromMap(new PhotoOld(), (Map<String, Object>) map.get("photo"));
		}

		if (map.get("location") != null) {
			shortcut.location = LocationOld.setPropertiesFromMap(new LocationOld(), (Map<String, Object>) map.get("location"));
		}

		if (map.get("creator") != null) {
			shortcut.creator = User.setPropertiesFromMap(new User(), (Map<String, Object>) map.get("creator"));
		}

		return shortcut;
	}

	public static Shortcut builder(Entity entity
			, String schema
			, String type
			, String action
			, String name
			, String subtitle
			, String image
			, Integer position
			, Boolean content
			, Boolean synthetic) {

		Shortcut shortcut = new Shortcut()
				.setAppId(entity.id)
				.setSchema(schema)
				.setApp(type)
				.setName(name)
				.setSubtitle(subtitle)
				.setPhoto(new PhotoOld(image, null, null, null, PhotoSource.resource))
				.setPosition(position)
				.setContent(content)
				.setAction(action);

		return shortcut;
	}

	@Override public Shortcut clone() {
		try {
			final Shortcut shortcut = (Shortcut) super.clone();

			if (photo != null) {
				shortcut.photo = photo.clone();
			}

			return shortcut;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public PhotoOld getPhoto() {
		return this.photo;
	}

	public PhotoOld getBrokenPhoto() {
		String prefix = "img_broken";
		String source = PhotoSource.resource;
		PhotoOld photo = new PhotoOld(prefix, null, null, null, source);
		return photo;
	}

	public Boolean isActive(Entity entity) {
		if (this.app.equals(Constants.TYPE_APP_MAP)) {
			if (entity.getLocation() == null) return false;
		}
		return true;
	}

	public Entity getAsEntity() {
		Entity entity = new SimpleEntity();
		entity.id = id;
		entity.name = name;
		entity.photo = photo;
		entity.subtitle = subtitle;
		entity.schema = schema;
		entity.description = description;
		entity.creator = creator;

		return entity;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public String getName() {
		return name;
	}

	public Shortcut setName(String name) {
		this.name = name;
		return this;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public Shortcut setSubtitle(String subtitle) {
		this.subtitle = subtitle;
		return this;
	}

	public String getAppId() {
		return appId;
	}

	public Shortcut setAppId(String appId) {
		this.appId = appId;
		return this;
	}

	public String getApp() {
		return app;
	}

	public Shortcut setApp(String app) {
		this.app = app;
		return this;
	}

	public String getAppUrl() {
		return appUrl;
	}

	public Shortcut setAppUrl(String appUrl) {
		this.appUrl = appUrl;
		return this;
	}

	public Shortcut setPhoto(PhotoOld photo) {
		this.photo = photo;
		return this;
	}

	public Number getPosition() {
		return (position != null) ? position : 0;
	}

	public Shortcut setPosition(Number position) {
		this.position = position;
		return this;
	}

	public String getSchema() {
		return schema;
	}

	public Shortcut setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Boolean isContent() {
		return ((content == null) ? true : content);
	}

	public Shortcut setContent(Boolean content) {
		this.content = content;
		return this;
	}

	public String getAction() {
		return action;
	}

	public Shortcut setAction(String action) {
		this.action = action;
		return this;
	}

	public String getId() {
		return id;
	}

	public Shortcut setId(String id) {
		this.id = id;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public static class SortByPositionSortDate implements Comparator<Shortcut> {

		@Override
		public int compare(Shortcut object1, Shortcut object2) {

			if (object1.getPosition().intValue() < object2.getPosition().intValue())
				return -1;
			else if (object1.getPosition().intValue() == object2.getPosition().intValue()) {
				if (object1.sortDate == null || object2.sortDate == null)
					return 0;
				else {
					if (object1.sortDate.longValue() < object2.sortDate.longValue())
						return 1;
					else if (object1.sortDate.longValue() == object2.sortDate.longValue()) return 0;
					return -1;
				}
			}
			return 1;
		}
	}

	@SuppressWarnings("ucd")
	public static enum InstallStatus {
		NONE,
		ACCEPTED,
		LATER,
		DECLINED
	}
}