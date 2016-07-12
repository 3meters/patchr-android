package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.UserManager;
import com.patchr.model.Location;
import com.patchr.objects.CacheStamp.StampSource;
import com.patchr.objects.LinkOld.Direction;
import com.patchr.objects.enums.PhotoCategory;
import com.patchr.utilities.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entity as described by the service protocol standards.
 *
 * @author Jayma
 */
@SuppressWarnings("ucd")
public abstract class Entity extends ServiceBase implements Cloneable, Serializable {

	private static final long serialVersionUID = -3902834532692561618L;

	/*--------------------------------------------------------------------------------------------
	 * Service fields
	 *--------------------------------------------------------------------------------------------*/

	/* Database fields */

	public String      subtitle;
	public String      description;
	public PhotoOld    photo;
	public LocationOld location;
	public String      patchId;

	/* Synthetic fields */

	public List<LinkOld> linksIn;
	public List<LinkOld> linksOut;
	public List<Count>   linksInCounts;
	public List<Count>   linksOutCounts;

	public String  toId;                                         // Used to find entities this entity is linked to
	public String  fromId;                                       // Used to find entities this entity is linked from
	public String  linkId;                                       // Used to update the link used to include this entity in a set
	public Boolean linkEnabled;                                  // Used to update the link used to include this entity in a set

	/* Service synthesized fields */

	public Patch  patch;
	public Number score;
	public Number rank;

	/* Local convenience fields */

	public Float distance;                                     // Used to cache most recent distance calculation.
	public Number  index = 0;                                   // Used to cross reference list position for mapping.

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Entity setPropertiesFromMap(Entity entity, Map map) {

		synchronized (entity) {
		    /*
			 * Need to include any properties that need to survive encode/decoded between activities.
			 */
			entity = (Entity) ServiceBase.setPropertiesFromMap(entity, map);

			entity.subtitle = (String) map.get("subtitle");
			entity.description = (String) map.get("description");

			entity.patchId = (String) (map.get("_acl") != null ? map.get("_acl") : map.get("patchId"));

			entity.toId = (String) (map.get("_to") != null ? map.get("_to") : map.get("toId"));
			entity.fromId = (String) (map.get("_from") != null ? map.get("_from") : map.get("fromId"));
			entity.linkId = (String) (map.get("_link") != null ? map.get("_link") : map.get("linkId"));
			entity.linkEnabled = (Boolean) ((map.get("linkEnabled") != null) ? map.get("linkEnabled") : true);

			entity.score = (Number) map.get("score");
			entity.rank = (Number) map.get("rank");

			if (map.get("photo") != null) {
				entity.photo = PhotoOld.setPropertiesFromMap(new PhotoOld(), (Map<String, Object>) map.get("photo"));
			}

			if (map.get("location") != null) {
				entity.location = LocationOld.setPropertiesFromMap(new LocationOld(), (Map<String, Object>) map.get("location"));
			}

			if (map.get("patch") != null) {
				entity.patch = Patch.setPropertiesFromMap(new Patch(), (Map<String, Object>) map.get("patch"));
			}

			if (map.get("linksIn") != null) {
				entity.linksIn = new ArrayList<LinkOld>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksIn");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksIn.add(LinkOld.setPropertiesFromMap(new LinkOld(), linkMap));
				}
			}

			if (map.get("linksOut") != null) {
				entity.linksOut = new ArrayList<LinkOld>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOut");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksOut.add(LinkOld.setPropertiesFromMap(new LinkOld(), linkMap));
				}
			}

			if (map.get("linksInCounts") != null) {
				entity.linksInCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksInCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksInCounts.add(Count.setPropertiesFromMap(new Count(), countMap));
				}
			}

			if (map.get("linksOutCounts") != null) {
				entity.linksOutCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOutCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksOutCounts.add(Count.setPropertiesFromMap(new Count(), countMap));
				}
			}
		}
		return entity;
	}

	public Shortcut getAsShortcut() {
		Shortcut shortcut = new Shortcut()
			.setAppId(id)
			.setId(id)
			.setName((name != null) ? name : null)
			.setPhoto(getPhoto())
			.setSchema((schema != null) ? schema : null)
			.setPosition(position);

		shortcut.sortDate = (sortDate != null) ? sortDate : modifiedDate;
		shortcut.content = true;
		shortcut.action = Constants.ACTION_VIEW;
		return shortcut;
	}

	public CacheStamp getCacheStamp() {
		CacheStamp cacheStamp = new CacheStamp(this.activityDate, this.modifiedDate);
		cacheStamp.source = StampSource.ENTITY.name().toLowerCase(Locale.US);
		return cacheStamp;
	}

	public Boolean isTempId() {
		return (id != null && id.substring(0, 5).equals("temp:"));
	}

	public Boolean isOwnedByCurrentUser() {
		Boolean owned = (ownerId != null
			&& UserManager.shared().authenticated()
			&& ownerId.equals(UserManager.currentUser.id));
		return owned;
	}

	public boolean sameAs(Object obj) {
		if (obj == null) return false;
		if (!((Object) this).getClass().equals(obj.getClass())) return false;

		final Entity other = (Entity) obj;

		return Type.equal(this.id, other.id)
			&& Type.equal(this.name, other.name)
			&& Type.equal(this.description, other.description)
			&& this.photo.uri(PhotoCategory.NONE).equals(other.photo.uri(PhotoCategory.NONE))
			&& !(this.linksIn != null && other.linksIn != null && this.linksIn.size() != other.linksIn.size())
			&& !(this.linksOut != null && other.linksOut != null && this.linksOut.size() != other.linksOut.size());
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public PhotoOld getPhoto() {
		return this.photo;
	}

	public Location getLocation() {
//		/*
//		 * We do n
//		 */
//		RealmLocation _location = null;
//
//		if (this.location != null
//			&& this.location.lat != null
//			&& this.location.lng != null) {
//			_location = new AirLocation(this.location.lat.doubleValue(), this.location.lng.doubleValue());
//			_location.accuracy = this.location.accuracy;
//			_location.provider = this.location.provider;
//		}
//
//		if (_location == null) {
//			final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
//			if (beacon != null && beacon.location != null && beacon.location.lat != null && beacon.location.lng != null) {
//				_location = beacon.location;
//			}
//		}

		return null;
	}

	public Float getDistance(Boolean refresh) {
		/*
		 * Priority order:
		 * - Linked to currently visible primary beacon.
		 * - Has a location.
		 */

		if (refresh || distance == null) {
			distance = null;
			final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null) {
				distance = beacon.getDistance(refresh);  // Estimate based on signal strength
			}
			else {
				final Location entityLocation = getLocation();
				final Location deviceLocation = LocationManager.getInstance().getLocationLocked();

				if (entityLocation != null && deviceLocation != null) {
					distance = deviceLocation.distanceTo(entityLocation);
				}
			}
		}
		return distance;
	}

	public Beacon getActiveBeacon(String type, Boolean primaryOnly) {
	    /*
	     * If an entity has more than one viable link, we choose the strongest one.
		 */
		LinkOld activeLink = null;
		if (linksOut != null) {
			LinkOld strongestLink = null;
			Integer strongestLevel = -200;
			for (LinkOld link : linksOut) {
				if (link.type != null && link.type.equals(type)) {
					Entity entity = DataController.getStoreEntity(link.toId);
					if (entity != null && entity.schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
						Beacon beacon = (Beacon) entity;
						if (beacon.signal != null && beacon.signal.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.signal.intValue();
						}
					}
				}
			}
			activeLink = strongestLink;
		}

		if (activeLink != null) {
			Beacon beacon = (Beacon) DataController.getStoreEntity(activeLink.toId);
			return beacon;
		}

		return null;
	}

	public List<? extends Entity> getLinkedEntitiesByLinkTypeAndSchema(List<String> types, List<String> schemas, Direction direction, Boolean traverse) {
	    /*
	     * Currently only called by EntityCache.removeEntityTree
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		if (linksIn != null) {
			if (direction == Direction.in || direction == Direction.both) {
				for (LinkOld link : linksIn) {
					if (types == null || (link.type != null && types.contains(link.type))) {
						Entity entity = DataController.getStoreEntity(link.fromId);
						if (entity != null) {
							if (Type.isTrue(traverse)) {
								entities.addAll(entity.getLinkedEntitiesByLinkTypeAndSchema(types, schemas, Direction.in, traverse));
							}
							if (schemas == null || schemas.contains(entity.schema)) {
								entities.add(entity);
							}
						}
					}
				}
			}
		}
		if (linksOut != null) {
			if (direction == Direction.out || direction == Direction.both) {
				for (LinkOld link : linksOut) {
					if (types == null || (link.type != null && types.contains(link.type))) {
						Entity entity = DataController.getStoreEntity(link.toId);
						if (entity != null) {
							if (Type.isTrue(traverse)) {
								entities.addAll(entity.getLinkedEntitiesByLinkTypeAndSchema(types, schemas, Direction.out, traverse));
							}
							if (schemas == null || schemas.contains(entity.schema)) {
								entities.add(entity);
							}
						}
					}
				}
			}
		}
		return entities;
	}

	public Entity getParent(String type, String targetSchema) {
		if (toId == null && linksOut != null) {
			for (LinkOld link : linksOut) {
				if (type == null || (link.type != null && link.type.equals(type))) {
					if (targetSchema == null || (link.targetSchema != null && link.targetSchema.equals(targetSchema))) {
						return DataController.getStoreEntity(link.toId);
					}
				}
			}
		}
		else if (toId != null) {
			return DataController.getStoreEntity(toId);
		}
		return null;
	}

	public LinkOld getParentLink(String type, String targetSchema) {
		if (linksOut != null) {
			for (LinkOld link : linksOut) {
				if ((type == null || (link.type != null && link.type.equals(type)))
					&& (targetSchema == null || link.targetSchema.equals(targetSchema)))
					return link;
			}
		}
		return null;
	}

	public Boolean hasActiveProximity() {
		if (linksOut != null) {
			for (LinkOld link : linksOut) {
				if (link.type != null && link.type.equals(Constants.TYPE_LINK_PROXIMITY)) {
					Beacon beacon = (Beacon) DataController.getStoreEntity(link.toId);
					if (beacon != null) return true;
				}
			}
		}
		return false;
	}

	public Count getCount(String type, String schema, Boolean enabled, Direction direction) {
		List<Count> linkCounts = linksInCounts;
		if (direction == Direction.out) {
			linkCounts = linksOutCounts;
		}

		if (linkCounts != null) {
			for (Count linkCount : linkCounts) {
				if ((type == null || (linkCount.type != null && linkCount.type.equals(type)))
					&& (schema == null || (linkCount.schema != null && linkCount.schema.equals(schema)))
					&& (enabled == null || (linkCount.enabled != null && linkCount.enabled.equals(enabled))))
					return linkCount;
			}
		}
		return null;
	}

	public LinkOld getLink(String type, String targetSchema, String targetId, Direction direction) {
		List<LinkOld> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			for (LinkOld link : links) {
				if (type == null || (link.type != null && link.type.equals(type))) {
					if (targetSchema == null || link.targetSchema.equals(targetSchema)) {
						if (targetId == null || targetId.equals((direction == Direction.in) ? link.fromId : link.toId))
							return link;
					}
				}
			}
		}
		return null;
	}

	public List<LinkOld> getLinks(String type, String targetSchema, String targetId, Direction direction) {
		List<LinkOld> links = new ArrayList<LinkOld>();
		List<LinkOld> tempLinks = linksIn;
		if (direction == Direction.out) {
			tempLinks = linksOut;
		}
		if (tempLinks != null) {
			for (LinkOld link : tempLinks) {
				if (type == null || (link.type != null && link.type.equals(type))) {
					if (targetSchema == null || link.targetSchema.equals(targetSchema)) {
						if (targetId == null || targetId.equals((direction == Direction.in) ? link.fromId : link.toId))
							links.add(link);
					}
				}
			}
		}
		return links;
	}

	public LinkOld removeLinksByTypeAndTargetSchema(String type, String targetSchema, String targetId, Direction direction) {
		List<LinkOld> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			Iterator<LinkOld> iterLinks = links.iterator();
			while (iterLinks.hasNext()) {
				LinkOld link = iterLinks.next();
				if (link.type != null && link.type.equals(type) && link.targetSchema.equals(targetSchema)) {
					if (targetId == null || targetId.equals((direction == Direction.in) ? link.fromId : link.toId)) {
						iterLinks.remove();
					}
				}
			}
		}
		return null;
	}

	public List<Shortcut> getShortcuts(@NonNull ShortcutSettings settings, Comparator<ServiceBase> linkSorter, Comparator<Shortcut> shortcutSorter) {

		List<Shortcut> shortcuts = new ArrayList<Shortcut>();
		List<LinkOld> links = (settings.direction == Direction.in) ? linksIn : linksOut;

		if (links != null) {
			if (linkSorter != null) {
				Collections.sort(links, linkSorter);
			}
			for (LinkOld link : links) {
				if ((settings.linkType == null || (link.type != null && link.type.equals(settings.linkType))) && link.shortcut != null) {
					if (settings.linkTargetSchema == null || (link.targetSchema.equals(settings.linkTargetSchema))) {
						if (settings.linkBroken
							|| (!settings.linkBroken && (link.shortcut.validatedDate == null || link.shortcut.validatedDate.longValue() != -1))) {
								/*
								 * Must clone or the groups added below will cause circular references
								 * that choke serializing to json.
								 */
							Shortcut shortcut = link.shortcut.clone();
							shortcut.linkType = settings.linkType;
							shortcuts.add(shortcut);
						}
					}
				}
			}

			if (shortcutSorter != null) {
				Collections.sort(shortcuts, shortcutSorter);
			}
		}

		return shortcuts;
	}

	public LinkOld linkFromAppUser(String linkType) {
		if (UserManager.shared().authenticated()) {
			if (linksIn != null) {
				for (LinkOld link : linksIn) {
					if (link.type != null
						&& link.type.equals(linkType)
						&& link.fromId.equals(UserManager.currentUser.id))
						return link;
				}
			}
		}
		return null;
	}

	public LinkOld linkByAppUser(String linkType, String schema) {
		if (UserManager.shared().authenticated()) {
			if (linksIn != null) {
				for (LinkOld link : linksIn) {
					if (link.type != null
						&& link.type.equals(linkType)
						&& link.targetSchema.equals(schema)
						&& link.creatorId.equals(UserManager.currentUser.id))
						return link;
				}
			}
			if (linksOut != null) {
				for (LinkOld link : linksOut) {
					if (link.type != null
						&& link.type.equals(linkType)
						&& link.targetSchema.equals(schema)
						&& link.creatorId.equals(UserManager.currentUser.id))
						return link;
				}
			}
		}
		return null;
	}

	public void removeLink() {}

	public static String getLabelForSchema(@NonNull String schema) {
		String label = schema;
		return label;
	}

	public static String getSchemaForId(@NonNull String id) {
		String prefix = id.substring(0, 2);
		if (prefix.equals("be")) {
			return Constants.SCHEMA_ENTITY_BEACON;
		}
		else if (prefix.equals("pa")) {
			return Constants.SCHEMA_ENTITY_PATCH;
		}
		else if (prefix.equals("us")) {
			return Constants.SCHEMA_ENTITY_USER;
		}
		else if (prefix.equals("me")) {
			return Constants.SCHEMA_ENTITY_MESSAGE;
		}
		else if (prefix.equals("no")) {
			return Constants.SCHEMA_ENTITY_NOTIFICATION;
		}
		return null;
	}

	public Long idAsLong() {
		return Long.parseLong(this.id.replaceAll("[^0-9]", "").substring(1));
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	@Override public Entity clone() {

		final Entity entity = (Entity) super.clone();
		if (entity != null) {

			if (linksIn != null) {
				entity.linksIn = (List<LinkOld>) ((ArrayList) linksIn).clone();
			}
			if (linksOut != null) {
				entity.linksOut = (List<LinkOld>) ((ArrayList) linksOut).clone();
			}
			if (linksInCounts != null) {
				entity.linksInCounts = (List<Count>) ((ArrayList) linksInCounts).clone();
			}
			if (linksOutCounts != null) {
				entity.linksOutCounts = (List<Count>) ((ArrayList) linksOutCounts).clone();
			}
			if (photo != null) {
				entity.photo = photo.clone();
			}
			if (patch != null) {
				entity.patch = patch.clone();
			}
			if (location != null) {
				entity.location = location.clone();
			}
		}
		return entity;
	}

	@Override public String toString() {
		return this.id;
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	@Override public boolean equals(Object object) {
	    /*
	     * Object class implementation of equals uses reference but we want to compare
         * using semantic equality.
         */
		return object != null
			&& object instanceof Entity
			&& !((this.id == null) || (((Entity) object).id == null))
			&& (this == object || this.id.equals(((Entity) object).id));
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortByRank implements Comparator<Entity> {

		@Override
		public int compare(@NonNull Entity object1, @NonNull Entity object2) {

			if (object1.rank == null || object2.rank == null)
				return 0;
			else if (object1.rank.intValue() < object2.rank.intValue())
				return -1;
			else if (object1.rank.intValue() > object2.rank.intValue())
				return 1;
			else
				return 0;
		}
	}
}