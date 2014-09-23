package com.aircandi.objects;

import android.support.annotation.Nullable;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.EntityControllerBase;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.CacheStamp.StampSource;
import com.aircandi.objects.Link.Direction;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
	 * service fields
	 *--------------------------------------------------------------------------------------------*/ 	/* Database fields */

	@Expose
	public String      subtitle;
	@Expose
	public String      description;
	@Expose
	public Photo       photo;
	@Expose
	public AirLocation location;
	@Expose
	public Number signalFence = -100.0f;
	@Expose
	public String visibility;                                    // private|public|hidden
	@Expose
	@SerializedName(name = "_place")
	public String placeId;

	/* Synthetic fields */

	@Expose(serialize = false, deserialize = true)
	public List<Link>  linksIn;
	@Expose(serialize = false, deserialize = true)
	public List<Link>  linksOut;
	@Expose(serialize = false, deserialize = true)
	public List<Count> linksInCounts;
	@Expose(serialize = false, deserialize = true)
	public List<Count> linksOutCounts;

	@Expose(serialize = false, deserialize = true)
	public String toId;                                            // Used to find entities this entity is linked to
	@Expose(serialize = false, deserialize = true)
	public String fromId;                                        // Used to find entities this entity is linked from

	@Expose(serialize = false, deserialize = true)
	public List<Entity> entities;

	/* Place (synthesized for the client) */

	@Expose(serialize = false, deserialize = true)
	public Place place;

	/* Stat fields (synthesized for the client) */

	@Expose(serialize = false, deserialize = true)
	public String reason;
	@Expose(serialize = false, deserialize = true)
	public Number score;
	@Expose(serialize = false, deserialize = true)
	public Number count;
	@Expose(serialize = false, deserialize = true)
	public Number rank;

	/*--------------------------------------------------------------------------------------------
	 * client fields (NONE are transferred)
	 *--------------------------------------------------------------------------------------------*/    public Boolean hidden = false;                    // Flag entities not currently visible because of fencing.
	public Boolean shareable        = true;                     // Flag whether an entity can be shared or not.
	public Boolean fuzzy            = false;                    // Flag places with inaccurate locations.
	public Boolean checked          = false;                    // Used to track selection in lists.
	public Boolean shortcuts        = false;                    // Do links have shortcuts?
	public Boolean foundByProximity = false;                    // Was this found based on proximity
	public Boolean editing          = false;                    // Used to flag when we can't use id to match up.
	public Boolean highlighted      = false;                    // Used to track one shot highlighting
	public Boolean read             = true;                    // Used to track if the user has browsed.
	public Boolean autowatchable    = false;                    // Used to track if the user has browsed.
	public Float distance;                                        // Used to cache most recent distance calculation.

    /* Entity is not persisted with service, only seeing this for suggested places that
       come from provider. We also use this when injecting a fake beacon or applink. */

	public Boolean synthetic = false;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public Shortcut getShortcut() {
		Shortcut shortcut = new Shortcut()
				.setAppId(id)
				.setId(id)
				.setName((name != null) ? name : null)
				.setPhoto(getPhoto())
				.setSchema((schema != null) ? schema : null)
				.setApp((schema != null) ? schema : null)
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

	public Boolean visibleToCurrentUser() {
		//		if (visibility != null && !visibility.equals(Constants.VISIBILITY_PUBLIC) && !isOwnedByCurrentUser()) {
		//			Link link = linkByAppUser(Constants.TYPE_LINK_WATCH);
		//			if (link == null || !link.enabled) {
		//				return false;
		//			}
		//		}
		return true;
	}

	public Boolean isOwnedByCurrentUser() {
		Boolean owned = (ownerId != null
				&& Aircandi.getInstance().getCurrentUser() != null
				&& ownerId.equals(Aircandi.getInstance().getCurrentUser().id));
		return owned;
	}

	public Boolean isOwnedBySystem() {
		Boolean owned = (ownerId != null && ownerId.equals(ServiceConstants.ADMIN_USER_ID));
		return owned;
	}

	public boolean sameAs(Object obj) {
		if (obj == null) return false;
		if (!((Object) this).getClass().equals(obj.getClass())) return false;

		final Entity other = (Entity) obj;

		if (!Type.equal(this.id, other.id)) return false;
		if (!Type.equal(this.name, other.name)) return false;
		if (!Type.equal(this.description, other.description)) return false;
		if (!this.getPhoto().getUri().equals(other.getPhoto().getUri())) return false;
		if (this.linksIn.size() != other.linksIn.size()) return false;
		return !(this.linksOut != null && other.linksOut != null && this.linksOut.size() != other.linksOut.size());

	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public Photo getPhoto() {
		Photo photo = this.photo;
		if (photo == null) {
			photo = getDefaultPhoto();
			if (photo == null) {
				photo = EntityControllerBase.getDefaultPhoto();
			}
		}
		return photo;
	}

	public Photo getDefaultPhoto() {
		IEntityController controller = Aircandi.getInstance().getControllerForSchema(this.schema);
		return ((controller != null) ? controller.getDefaultPhoto(this.type) : null);
	}

	public Photo getPlaceholderPhoto() {
		IEntityController controller = Aircandi.getInstance().getControllerForSchema(this.schema);
		return ((controller != null) ? controller.getPlaceholderPhoto(this.type) : null);
	}

	public AirLocation getLocation() {
		AirLocation loc = null;
		Entity parent = getParent(null, null);

		if (parent != null) {
			loc = parent.location;
		}
		else {
			final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null) {
				loc = beacon.location;
			}
		}
		if (loc == null
				&& this.location != null
				&& this.location.lat != null
				&& this.location.lng != null) {
			loc = new AirLocation(this.location.lat.doubleValue(), this.location.lng.doubleValue());
			loc.accuracy = this.location.accuracy;
		}
		return loc;
	}

	public Boolean isHidden() {
		Boolean oldIsHidden = hidden;
		this.hidden = false;
		/*
         * Make it harder to fade out than it is to fade in. Entities are only NEW
		 * for the first scan that discovers them.
		 */
		if (signalFence != null) {
			float signalThresholdFluid = signalFence.floatValue();
			Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null) {
				if (!oldIsHidden) {
					signalThresholdFluid = signalFence.floatValue() - 5;
				}

				/* Hide entities that are not within entity declared virtual range */
				if (Booleans.getBoolean(R.bool.entity_fencing_enabled) && beacon.signal.intValue() < signalThresholdFluid) {
					this.hidden = true;
				}
			}
		}
		return this.hidden;
	}

	public Float getDistance(Boolean refresh) {

		if (refresh || distance == null) {
			distance = null;
			final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null) {
				distance = beacon.getDistance(refresh);
			}
			else {
				final AirLocation entityLocation = getLocation();
				final AirLocation deviceLocation = LocationManager.getInstance().getAirLocationLocked();

				if (entityLocation != null && deviceLocation != null) {
					distance = deviceLocation.distanceTo(entityLocation);
					fuzzy = false;
					if (entityLocation.accuracy != null
							&& entityLocation.accuracy.intValue() > Constants.MINIMUM_ACCURACY_FOR_DISTANCE_DISPLAY) {
						fuzzy = true;
					}
					else if (this instanceof Place) {
						Place place = (Place) this;
						if (place.provider.yelp != null) {
							fuzzy = true;
						}
						if (place.provider.google != null || place.provider.factual != null) {
							fuzzy = false;
						}
					}
				}
			}
		}
		return distance;
	}

	public Beacon getActiveBeacon(String type, Boolean primaryOnly) {
        /*
         * If an entity has more than one viable link, we choose the one
		 * using the following priority:
		 * 
		 * - strongest primary
		 * - any primary
		 * - any non-primary
		 */
		Link activeLink = null;
		if (linksOut != null) {
			Link strongestLink = null;
			Integer strongestLevel = -200;
			for (Link link : linksOut) {
				if (link.type.equals(type)) {
					if (link.proximity != null && link.proximity.primary != null && link.proximity.primary) {
						Beacon beacon = (Beacon) EntityManager.getEntityCache().get(link.toId);
						if (beacon != null && beacon.signal.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.signal.intValue();
						}
					}
				}
			}

			if (strongestLink == null && !primaryOnly) {
				for (Link link : linksOut) {
					if (link.type.equals(type)) {
						Beacon beacon = (Beacon) EntityManager.getEntityCache().get(link.toId);
						if (beacon != null && beacon.signal.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.signal.intValue();
						}
					}
				}
			}
			activeLink = strongestLink;
		}

		if (activeLink != null) {
			Beacon beacon = (Beacon) EntityManager.getEntityCache().get(activeLink.toId);
			return beacon;
		}
		return null;
	}

	public Beacon getBeaconFromLink(String type, Boolean primaryOnly) {
        /*
         * If an entity has more than one viable link, we choose the one
		 * using the following priority:
		 * 
		 * - first primary
		 * - first non-primary
		 */
		Link activeLink = null;
		if (linksOut != null) {
			Link strongestLink = null;
			for (Link link : linksOut) {
				if (link.type.equals(type)) {
					if (link.proximity != null && link.proximity.primary != null && link.proximity.primary) {
						strongestLink = link;
						break;
					}
				}
			}

			if (strongestLink == null && !primaryOnly) {
				for (Link link : linksOut) {
					if (link.type.equals(type)) {
						strongestLink = link;
						break;
					}
				}
			}

			activeLink = strongestLink;
		}

		if (activeLink != null) {
			Beacon beacon = new Beacon(activeLink.shortcut.id.substring(3)
					, activeLink.shortcut.name
					, activeLink.shortcut.name
					, -50
					, false);
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
				for (Link link : linksIn) {
					if (types == null || types.contains(link.type)) {
						Entity entity = EntityManager.getCacheEntity(link.fromId);
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
				for (Link link : linksOut) {
					if (types == null || types.contains(link.type)) {
						Entity entity = EntityManager.getCacheEntity(link.toId);
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
			for (Link link : linksOut) {
				if (type == null || (link.type != null && link.type.equals(type))) {
					if (targetSchema == null || (link.targetSchema != null && link.targetSchema.equals(targetSchema))) {
						return EntityManager.getCacheEntity(link.toId);
					}
				}
			}
			return null;
		}
		else
			return EntityManager.getCacheEntity(toId);
	}

	public Link getParentLink(String type, String targetSchema) {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if ((type == null || link.type.equals(type))
						&& (targetSchema == null || link.targetSchema.equals(targetSchema)))
					return link;
			}
		}
		return null;
	}

	public Boolean hasActiveProximity() {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if (link.type.equals(Constants.TYPE_LINK_PROXIMITY) && link.proximity != null) {
					Beacon beacon = (Beacon) EntityManager.getCacheEntity(link.toId);
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
				if ((type == null || linkCount.type.equals(type))
						&& (schema == null || linkCount.schema.equals(schema))
						&& (enabled == null || linkCount.enabled.equals(enabled))) return linkCount;
			}
		}
		return null;
	}

	public Link getLink(String type, String targetSchema, String targetId, Direction direction) {
		List<Link> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			for (Link link : links) {
				if (type == null || link.type.equals(type)) {
					if (targetSchema == null || link.targetSchema.equals(targetSchema)) {
						if (targetId == null || targetId.equals((direction == Direction.in) ? link.fromId : link.toId))
							return link;
					}
				}
			}
		}
		return null;
	}

	public List<Link> getLinks(String type, String targetSchema, String targetId, Direction direction) {
		List<Link> links = new ArrayList<Link>();
		List<Link> tempLinks = linksIn;
		if (direction == Direction.out) {
			tempLinks = linksOut;
		}
		if (tempLinks != null) {
			for (Link link : tempLinks) {
				if (type == null || link.type.equals(type)) {
					if (targetSchema == null || link.targetSchema.equals(targetSchema)) {
						if (targetId == null || targetId.equals((direction == Direction.in) ? link.fromId : link.toId))
							links.add(link);
					}
				}
			}
		}
		return links;
	}

	public Link removeLinksByTypeAndTargetSchema(String type, String targetSchema, String targetId, Direction direction) {
		List<Link> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			Iterator<Link> iterLinks = links.iterator();
			while (iterLinks.hasNext()) {
				Link link = iterLinks.next();
				if (link.type.equals(type) && link.targetSchema.equals(targetSchema)) {
					if (targetId == null || targetId.equals((direction == Direction.in) ? link.fromId : link.toId)) {
						iterLinks.remove();
					}
				}
			}
		}
		return null;
	}

	public List<Shortcut> getShortcuts(ShortcutSettings settings, Comparator<ServiceBase> linkSorter, Comparator<Shortcut> shortcutSorter) {

		List<Shortcut> shortcuts = new ArrayList<Shortcut>();
		List<Link> links = (settings.direction == Direction.in) ? linksIn : linksOut;

		if (links != null) {
			if (linkSorter != null) {
				Collections.sort(links, linkSorter);
			}
			for (Link link : links) {
				if ((settings.linkType == null || link.type.equals(settings.linkType)) && link.shortcut != null) {
					if (settings.linkTargetSchema == null || (link.targetSchema.equals(settings.linkTargetSchema))) {
						if (settings.synthetic == null || link.shortcut.isSynthetic().equals(settings.synthetic)) {
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
			}

			if (shortcutSorter != null) {
				Collections.sort(shortcuts, shortcutSorter);
			}

			if (shortcuts.size() > 0 && settings.groupedByApp) {

				final Map<String, List<Shortcut>> shortcutLists = new HashMap<String, List<Shortcut>>();
				for (Shortcut shortcut : shortcuts) {
					if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
						List<Shortcut> list = new ArrayList<Shortcut>();
						list.add(shortcut);
						shortcutLists.put(shortcut.appUrl, list);
					}
					else {
						if (shortcutLists.containsKey(shortcut.app)) {
							shortcutLists.get(shortcut.app).add(shortcut);
						}
						else {
							List<Shortcut> list = new ArrayList<Shortcut>();
							list.add(shortcut);
							shortcutLists.put(shortcut.app, list);
						}
					}
				}

				shortcuts.clear();
				final Iterator iter = shortcutLists.keySet().iterator();
				while (iter.hasNext()) {
					List<Shortcut> list = shortcutLists.get((String) iter.next());
					Shortcut shortcut = list.get(0);
					shortcut.setCount(0);
					Count count = getCount(shortcut.linkType, shortcut.app, null, settings.direction);
					if (count != null) {
						shortcut.setCount(count.count.intValue());
					}
					shortcut.group = list;
					shortcuts.add(shortcut);
				}
			}
		}

		return shortcuts;
	}

	public Boolean byAppUser(String linkType) {
		if (linksIn != null) {
			for (Link link : linksIn) {
				if (link.type.equals(linkType) && link.fromId.equals(Aircandi.getInstance().getCurrentUser().id))
					return true;
			}
		}
		return false;
	}

	public Link linkByAppUser(String linkType) {
		if (linksIn != null) {
			for (Link link : linksIn) {
				if (link.type.equals(linkType) && link.fromId.equals(Aircandi.getInstance().getCurrentUser().id))
					return link;
			}
		}
		return null;
	}

	public void getClientShortcuts(List<Shortcut> shortcuts) {
        /*
         * Shortcuts are in shortcut.isActive() at draw time to determine if it gets shown
		 */

		/* Comments */
		Shortcut shortcut = Shortcut.builder(this
				, Constants.SCHEMA_ENTITY_APPLINK
				, Constants.TYPE_APP_COMMENT
				, Constants.ACTION_VIEW_FOR
				, StringManager.getString(R.string.label_link_comments)
				, null
				, "img_comment_temp"
				, 20
				, false
				, true);
		shortcut.photo.colorize = true;
		shortcut.photo.color = Colors.getColor(Aircandi.getInstance().getControllerForSchema(Constants.TYPE_APP_COMMENT).getColorPrimary());
		shortcut.linkType = Constants.TYPE_LINK_CONTENT;
		shortcuts.add(shortcut);
	}

	public void removeLink() {
	}

	public String getSchemaMapped() {
		String schema = this.schema;
		if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			schema = Constants.SCHEMA_REMAP_PICTURE;
		}
		return schema;
	}

	public String getLabelForSchema() {
		String label = getSchemaMapped();
		if (label.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			label = StringManager.getString(R.string.container_singular_lowercase);
		}
		return label;
	}

	public static String getSchemaMapped(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			schema = Constants.SCHEMA_REMAP_PICTURE;
		}
		return schema;
	}

	public static String getLabelForSchema(String schema) {
		String label = getSchemaMapped(schema);
		if (label.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			label = StringManager.getString(R.string.container_singular_lowercase);
		}
		return label;
	}

	public static String getSchemaForId(String id) {
		String prefix = id.substring(0, 2);
		if (prefix.equals("ap")) {
			return Constants.SCHEMA_ENTITY_APPLINK;
		}
		else if (prefix.equals("be")) {
			return Constants.SCHEMA_ENTITY_BEACON;
		}
		else if (prefix.equals("co")) {
			return Constants.SCHEMA_ENTITY_COMMENT;
		}
		else if (prefix.equals("pl")) {
			return Constants.SCHEMA_ENTITY_PLACE;
		}
		else if (prefix.equals("po")) {
			return Constants.SCHEMA_ENTITY_PICTURE;
		}
		else if (prefix.equals("us")) {
			return Constants.SCHEMA_ENTITY_USER;
		}
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/
	public static Entity setPropertiesFromMap(Entity entity, Map map, Boolean nameMapping) {

		synchronized (entity) {
            /*
			 * Need to include any properties that need to survive encode/decoded between activities.
			 */
			entity = (Entity) ServiceBase.setPropertiesFromMap(entity, map, nameMapping);

			entity.subtitle = (String) map.get("subtitle");
			entity.description = (String) map.get("description");
			entity.signalFence = (Number) map.get("signalFence");
			entity.visibility = (String) ((map.get("visibility") != null) ? map.get("visibility") : Constants.VISIBILITY_PUBLIC);

			entity.hidden = (Boolean) ((map.get("hidden") != null) ? map.get("hidden") : false);
			entity.synthetic = (Boolean) ((map.get("synthetic") != null) ? map.get("synthetic") : false);
			entity.shortcuts = (Boolean) ((map.get("shortcuts") != null) ? map.get("shortcuts") : false);
			entity.checked = (Boolean) ((map.get("checked") != null) ? map.get("checked") : false);
			entity.placeId = (String) (nameMapping ? map.get("_place") : map.get("placeId"));
			entity.editing = (Boolean) ((map.get("editing") != null) ? map.get("checked") : false);
			entity.highlighted = (Boolean) ((map.get("highlighted") != null) ? map.get("highlighted") : false);
			entity.read = (Boolean) ((map.get("read") != null) ? map.get("read") : false);

			entity.toId = (String) (nameMapping ? map.get("_to") : map.get("toId"));
			entity.fromId = (String) (nameMapping ? map.get("_from") : map.get("fromId"));

			entity.reason = (String) map.get("reason");
			entity.score = (Number) map.get("score");
			entity.count = (Number) map.get("count");
			entity.rank = (Number) map.get("rank");

			if (map.get("photo") != null) {
				entity.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"), nameMapping);
			}

			if (map.get("location") != null) {
				entity.location = AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap<String, Object>) map.get("location"), nameMapping);
			}

			if (map.get("place") != null) {
				entity.place = Place.setPropertiesFromMap(new Place(), (HashMap<String, Object>) map.get("place"), nameMapping);
			}

			if (map.get("linksIn") != null) {
				entity.linksIn = new ArrayList<Link>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksIn");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksIn.add(Link.setPropertiesFromMap(new Link(), linkMap, nameMapping));
				}
			}

			if (map.get("linksOut") != null) {
				entity.linksOut = new ArrayList<Link>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOut");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksOut.add(Link.setPropertiesFromMap(new Link(), linkMap, nameMapping));
				}
			}

			if (map.get("linksInCounts") != null) {
				entity.linksInCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksInCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksInCounts.add(Count.setPropertiesFromMap(new Count(), countMap, nameMapping));
				}
			}

			if (map.get("linksOutCounts") != null) {
				entity.linksOutCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOutCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksOutCounts.add(Count.setPropertiesFromMap(new Count(), countMap, nameMapping));
				}
			}

			if (map.get("entities") != null) {
				entity.entities = new ArrayList<Entity>();
				synchronized (entity.entities) {
					final List<LinkedHashMap<String, Object>> childMaps = (List<LinkedHashMap<String, Object>>) map.get("entities");
					for (Map<String, Object> childMap : childMaps) {
						String schema = (String) childMap.get("schema");
						IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
						entity.entities.add(controller.makeFromMap(map, nameMapping));
					}
				}
			}
		}
		return entity;
	}

	@Override
	@Nullable
	public Entity clone() {

		final Entity entity = (Entity) super.clone();
		if (entity != null) {

			if (linksIn != null) {
				entity.linksIn = (List<Link>) ((ArrayList) linksIn).clone();
			}
			if (linksOut != null) {
				entity.linksOut = (List<Link>) ((ArrayList) linksOut).clone();
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
			if (place != null) {
				entity.place = place.clone();
			}
		}
		return entity;
	}

	@Override
	public String toString() {
		return this.id;
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
        /*
         * Object class implementation of equals uses reference but we want to compare
         * using semantic equality.
         */
		if (object == null) return false;
		if (!(object instanceof Entity)) return false;
		if ((this.id == null) || (((Entity) object).id == null)) return false;
		if (this == object) return true;
		return this.id.equals(((Entity) object).id);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public static class SortByRank implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

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