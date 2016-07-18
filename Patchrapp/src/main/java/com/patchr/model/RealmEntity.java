package com.patchr.model;

import android.content.Intent;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.LocationManager;
import com.patchr.components.UserManager;
import com.patchr.objects.LinkCount;
import com.patchr.objects.enums.EventCategory;
import com.patchr.objects.enums.LinkDestination;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.enums.LinkType;
import com.patchr.objects.enums.MemberStatus;
import com.patchr.objects.Session;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.TriggerCategory;
import com.patchr.utilities.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

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
public class RealmEntity extends RealmObject {

	/* Service persisted fields */
	@PrimaryKey
	public String id;
	public String schema;
	public String type;
	public String name;
	public String namelc;
	public String subtitle;
	public String description;
	public String photoJson;

	public String  locationJson;
	public String  aclId; // _acl
	public String  patchId; // _acl
	public Boolean locked;

	public String ownerId;
	public String creatorId;
	public String modifierId;
	public Long   createdDate;
	public Long   modifiedDate;
	public Long   activityDate;
	public Long   sortDate;

	/* Patch entity */
	public String visibility;           // private|public|hidden

	/* User entity */
	public String  area;
	public Boolean developer;
	public String  email;
	public String  password;
	public String  role;
	public String  phoneJson;

	/* Message entity */
	public RealmEntity            message;
	public RealmList<RealmEntity> recipients;

	/* Beacon entity */
	public String  ssid;
	public String  bssid;
	public Integer signal;                                    // Used to evaluate location accuracy

	/* Notification entity */
	public String  targetId;
	public String  parentId;
	public String  userId;
	public Long    sentDate;
	public String  trigger;
	public String  event;
	public String  ticker;
	public Integer priority;
	public String  photoBigJson;
	public String  summary;

	/* Link as entity */
	public Boolean enabled;

	/* Service calculated fields */
	public RealmEntity owner;
	public RealmEntity creator;
	public RealmEntity modifier;
	public RealmEntity patch;
	public String      linkJson;
	public String      reason;        // Search suggestions
	public Float       score;         // Search suggestions

	/* Local calculated fields */
	public Integer countLikes             = 0;
	public Integer countMembers           = 0;
	public Integer countPending           = 0;
	public Boolean userMemberJustApproved = false;
	public Boolean userMemberMuted        = false;
	public String  userMemberStatus       = MemberStatus.NonMember;
	public String userMemberId;
	public String userLikesId;
	public Boolean userLikes     = false;
	public Integer patchesOwned  = 0;
	public Integer patchesMember = 0;
	public Integer countMessages;
	public Boolean userHasMessaged = false;
	public String shortcutForId;
	public Float  distance;              // Used to cache most recent distance calculation.

	/* Local convenience fields */
	@Ignore public Integer index = 0;                                   // Used to cross reference list position for mapping.
	@Ignore public String authType;
	@Ignore public Intent intent;
	@Ignore public Boolean personalized = true;
	@Ignore public Session session;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static RealmEntity setPropertiesFromMap(RealmEntity entity, Map map) {
		return setPropertiesFromMap(entity, map, false);
	}

	public static RealmEntity setPropertiesFromMap(RealmEntity entity, Map map, boolean asShortcut) {

		if (map.isEmpty()) return null;

		synchronized (entity) {

			entity.id = (String) (map.get("_id") != null ? map.get("_id") : map.get("id"));
			entity.schema = (String) map.get("schema");
			entity.type = (String) map.get("type");
			entity.name = (String) map.get("name");
			entity.namelc = (String) map.get("namelc");
			entity.subtitle = (String) map.get("subtitle");
			entity.description = (String) map.get("description");

			if (asShortcut) {
				entity.shortcutForId = (String) (map.get("_id") != null ? map.get("_id") : map.get("id"));
				entity.personalized = false;
				if (!entity.id.contains("sh.")) {
					entity.id = "sh.".concat(entity.id);
				}
			}

			entity.patchId = (String) (map.get("_acl") != null ? map.get("_acl") : map.get("patchId"));
			entity.ownerId = (String) (map.get("_owner") != null ? map.get("_owner") : map.get("ownerId"));
			entity.creatorId = (String) (map.get("_creator") != null ? map.get("_creator") : map.get("creatorId"));
			entity.modifierId = (String) (map.get("_modifier") != null ? map.get("_modifier") : map.get("modifierId"));
			entity.createdDate = map.get("createdDate") != null ? ((Double) map.get("createdDate")).longValue() : null;
			entity.modifiedDate = map.get("modifiedDate") != null ? ((Double) map.get("modifiedDate")).longValue() : null;
			entity.activityDate = map.get("activityDate") != null ? ((Double) map.get("activityDate")).longValue() : null;
			entity.sortDate = map.get("sortDate") != null ? ((Double) map.get("sortDate")).longValue() : null;

			if (entity.activityDate == null && entity.createdDate != null) {
				entity.activityDate = entity.createdDate; // Service doesn't set activityDate until there is activity
			}

			entity.score = map.get("score") != null ? ((Double) map.get("score")).floatValue() : null;
			entity.reason = (String) map.get("reason");
			entity.locked = (Boolean) map.get("locked");
			entity.enabled = (Boolean) map.get("enabled");

			if (map.get("photo") != null) {
				String photoJson = Patchr.gson.toJson(map.get("photo"), SimpleMap.class);
				entity.photoJson = photoJson;
			}
			if (map.get("location") != null) {
				String locationJson = Patchr.gson.toJson(map.get("location"), SimpleMap.class);
				entity.locationJson = locationJson;
			}
			if (map.get("patch") != null) {
				entity.patch = RealmEntity.setPropertiesFromMap(new RealmEntity(), (Map<String, Object>) map.get("patch"), true);
			}
			if (map.get("creator") != null) {
				entity.creator = RealmEntity.setPropertiesFromMap(new RealmEntity(), (Map<String, Object>) map.get("creator"), true);
			}
			if (map.get("owner") != null) {
				entity.owner = RealmEntity.setPropertiesFromMap(new RealmEntity(), (Map<String, Object>) map.get("owner"), true);
			}
			if (map.get("modifier") != null) {
				entity.modifier = RealmEntity.setPropertiesFromMap(new RealmEntity(), (Map<String, Object>) map.get("modifier"), true);
			}
			if (map.get("link") != null) {
				/*
				 * This is the only place that uses a link. We use it to support watch link state and management.
				 * Will get pulled in on a user for a list of users watching a patch and on patches when showing patches a
				 * user is watching (let's us show that a watch request is pending/approved).
				 */
				String linkJson = Patchr.gson.toJson(map.get("link"), SimpleMap.class);
				entity.linkJson = linkJson;
			}

			entity.countLikes = 0;
			entity.countMembers = 0;
			entity.countPending = 0;

			if (map.get("linkCounts") instanceof List) {
				List<Map<String, Object>> linkCounts = (List<Map<String, Object>>) map.get("linkCounts");
				for (Map<String, Object> linkMap : linkCounts) {
					LinkCount linkCount = LinkCount.setPropertiesFromMap(new LinkCount(), linkMap);
					if (linkCount.from != null && linkCount.from.equals(LinkDestination.Users) && linkCount.type.equals(LinkType.Like)) {
						entity.countLikes = linkCount.count;
					}
					if (linkCount.from != null && linkCount.from.equals(LinkDestination.Users) && linkCount.type.equals(LinkType.Watch) && linkCount.enabled) {
						entity.countMembers = linkCount.count;
					}
					if (linkCount.from != null && linkCount.from.equals(LinkDestination.Users) && linkCount.type.equals(LinkType.Watch) && !linkCount.enabled) {
						entity.countPending = linkCount.count;
					}
				}
			}

			entity.userMemberStatus = MemberStatus.NonMember;
			entity.userMemberMuted = false;
			entity.userMemberId = null;
			entity.userLikes = false;
			entity.userLikesId = null;

			if (map.get("links") instanceof List) {  // Null is ok here
				List<Map<String, Object>> linkMaps = (List<Map<String, Object>>) map.get("links");
				for (Map<String, Object> linkMap : linkMaps) {
					if (linkMap.get("fromSchema").equals(Constants.SCHEMA_ENTITY_USER) && linkMap.get("type").equals(LinkType.Watch)) {
						entity.userMemberId = (String) linkMap.get("_id");
						entity.userMemberStatus = MemberStatus.Pending;
						if (linkMap.get("enabled") != null) {
							if ((Boolean) linkMap.get("enabled")) {
								entity.userMemberStatus = MemberStatus.Member;
							}
						}
						if (linkMap.get("mute") != null) {
							if ((Boolean) linkMap.get("mute")) {
								entity.userMemberMuted = true;
							}
						}
					}
					if (linkMap.get("fromSchema").equals(Constants.SCHEMA_ENTITY_USER) && linkMap.get("type").equals(LinkType.Like)) {
						entity.userLikesId = (String) linkMap.get("_id");
						entity.userLikes = true;
					}
				}
			}

			if (entity.schema == null) return entity;

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				entity.area = (String) map.get("area");
				entity.email = (String) map.get("email");
				entity.role = (String) map.get("role");
				entity.developer = (Boolean) map.get("developer");
				entity.authType = (String) map.get("authType");

				if (map.get("phone") != null) {
					String phoneJson = Patchr.gson.toJson(map.get("phone"), SimpleMap.class);
					entity.phoneJson = phoneJson;
				}

				entity.patchesMember = 0;
				entity.patchesOwned = 0;

				if (map.get("linkCounts") instanceof List) {  // Null is ok here
					List<Map<String, Object>> linkCounts = (List<Map<String, Object>>) map.get("linkCounts");
					for (Map<String, Object> linkMap : linkCounts) {
						LinkCount linkCount = LinkCount.setPropertiesFromMap(new LinkCount(), linkMap);
						if (linkCount.to != null && linkCount.to.equals(LinkDestination.Patches) && linkCount.type.equals(LinkType.Create)) {
							entity.patchesOwned = linkCount.count;
						}
						if (linkCount.to != null && linkCount.to.equals(LinkDestination.Patches) && linkCount.type.equals(LinkType.Watch)) {
							entity.patchesMember = linkCount.count;
						}
					}
				}
			}
			else if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				entity.visibility = (String) map.get("visibility");

				entity.countMessages = 0;

				if (map.get("linkCounts") instanceof List) {
					List<Map<String, Object>> linkCounts = (List<Map<String, Object>>) map.get("linkCounts");
					for (Map<String, Object> linkMap : linkCounts) {
						LinkCount linkCount = LinkCount.setPropertiesFromMap(new LinkCount(), linkMap);
						if (linkCount.from.equals(LinkDestination.Messages) && linkCount.type.equals(LinkType.Content)) {
							entity.countMessages = linkCount.count;
						}
					}
				}

				entity.userHasMessaged = false;

				if (map.get("links") instanceof List) {  // Null is ok here
					List<Map<String, Object>> linkMaps = (List<Map<String, Object>>) map.get("links");
					for (Map<String, Object> linkMap : linkMaps) {
						if (linkMap.get("fromSchema").equals(Constants.SCHEMA_ENTITY_MESSAGE) && linkMap.get("type").equals(LinkType.Content)) {
							entity.userHasMessaged = true;
						}
					}
				}
			}
			else if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				if (map.get("linked") instanceof List) {  // Null is ok here
					List<Map<String, Object>> linkMaps = (List<Map<String, Object>>) map.get("linked");
					for (Map<String, Object> linkMap : linkMaps) {
						if (linkMap.get("schema").equals(Constants.SCHEMA_ENTITY_PATCH)) {
							entity.patch = RealmEntity.setPropertiesFromMap(new RealmEntity(), linkMap, true);
						}
						else if (linkMap.get("schema").equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
							entity.message = RealmEntity.setPropertiesFromMap(new RealmEntity(), linkMap, true);
						}
						else if (linkMap.get("schema").equals(Constants.SCHEMA_ENTITY_USER)) {
							if (entity.recipients == null) {
								entity.recipients = new RealmList<RealmEntity>();
							}
							RealmEntity recipient = RealmEntity.setPropertiesFromMap(new RealmEntity(), linkMap, true);
							if (recipient != null) {
								entity.recipients.add(recipient);
							}
						}
					}
				}
			}
			else if (entity.schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
				entity.targetId = (String) (map.get("targetId") != null ? map.get("targetId") : map.get("_target"));
				entity.parentId = (String) (map.get("parentId") != null ? map.get("parentId") : map.get("_parent"));
				entity.userId = (String) (map.get("userId") != null ? map.get("userId") : map.get("_user"));
				entity.sentDate = map.get("sentDate") != null ? ((Double) map.get("sentDate")).longValue() : null;
				entity.trigger = (String) map.get("trigger");
				entity.event = (String) map.get("event");
				entity.ticker = (String) map.get("ticker");
				entity.priority = map.get("priority") != null ? ((Double) map.get("priority")).intValue() : null;
				entity.summary = (String) map.get("summary");

				if (map.get("photoBig") != null) {
					String photoJson = Patchr.gson.toJson(map.get("photoBig"), SimpleMap.class);
					entity.photoBigJson = photoJson;
				}
			}
			else if (entity.schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
				entity.ssid = (String) map.get("ssid");
				entity.bssid = (String) map.get("bssid");
				entity.signal = map.get("signal") != null ? ((Double) map.get("signal")).intValue() : null;
			}
		}
		return entity;
	}

	public Boolean isOwnedByCurrentUser() {
		return (ownerId != null && ownerId.equals(UserManager.currentUser.id));
	}

	public Boolean isVisibleToCurrentUser() {
		return !(isRestricted() && !isOwnedByCurrentUser()) || userMemberStatus.equals(MemberStatus.Member);
	}

	public Boolean isRestricted() {
		return (visibility != null && !visibility.equals(Constants.PRIVACY_PUBLIC));
	}

	public Boolean isRestrictedForCurrentUser() {
		return (isRestricted() && !isOwnedByCurrentUser());
	}

	public String getBeaconId() {
		final RealmEntity beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) return beacon.id;
		return null;
	}

	public Long idAsLong() {
		return Long.parseLong(this.id.replaceAll("[^0-9]", "").substring(1));
	}

	public RealmEntity getActiveBeacon(String type, Boolean primaryOnly) {
		return null;
	}

	public RealmEntity getParent(String type, String targetSchema) {
		return null;
	}

	public Boolean hasActiveProximity() {
		return false;
	}

	public Float getDistance(Boolean refresh) {

		if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
			if (refresh || this.distance == null) {

				this.distance = -1f;

				if (this.signal.intValue() >= -40) {
					this.distance = 1f;
				}
				else if (this.signal.intValue() >= -50) {
					this.distance = 2f;
				}
				else if (this.signal.intValue() >= -55) {
					this.distance = 3f;
				}
				else if (this.signal.intValue() >= -60) {
					this.distance = 5f;
				}
				else if (this.signal.intValue() >= -65) {
					this.distance = 7f;
				}
				else if (this.signal.intValue() >= -70) {
					this.distance = 10f;
				}
				else if (this.signal.intValue() >= -75) {
					this.distance = 15f;
				}
				else if (this.signal.intValue() >= -80) {
					this.distance = 20f;
				}
				else if (this.signal.intValue() >= -85) {
					this.distance = 30f;
				}
				else if (this.signal.intValue() >= -90) {
					this.distance = 40f;
				}
				else if (this.signal.intValue() >= -95) {
					this.distance = 60f;
				}
				else {
					this.distance = 80f;
				}
			}

			return this.distance * LocationManager.FeetToMetersConversion;
		}
		else {
			if (refresh || distance == null) {
				distance = null;
				final RealmEntity beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
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
	}

	public String getTriggerCategory() {
		if (this.trigger.contains("nearby")) return TriggerCategory.NEARBY;
		if (this.trigger.contains("watch")) return TriggerCategory.WATCH;
		if (this.trigger.contains("own")) return TriggerCategory.OWN;
		return TriggerCategory.NONE;
	}

	public String getEventCategory() {
		if (this.event.contains("share")) return EventCategory.SHARE;
		if (this.event.contains("insert")) return EventCategory.INSERT;
		if (this.event.contains("watch")) return EventCategory.WATCH;
		if (this.event.contains("like")) return EventCategory.LIKE;
		return EventCategory.NONE;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public PhoneNumber getPhone() {
		if (phoneJson != null) {
			return PhoneNumber.setPropertiesFromMap(new PhoneNumber(), Patchr.gson.fromJson(this.phoneJson, SimpleMap.class));
		}
		return null;
	}

	public Photo getPhoto() {
		if (photoJson != null) {
			return Photo.setPropertiesFromMap(new Photo(), Patchr.gson.fromJson(this.photoJson, SimpleMap.class));
		}
		return null;
	}

	public void setPhoto(Photo photo) {
		if (photo == null) {
			this.photoJson = null;
		}
		else {
			this.photoJson = Patchr.gson.toJson(photo, Photo.class);
		}
	}

	public Link getLink() {
		if (linkJson != null) {
			return Link.setPropertiesFromMap(new Link(), Patchr.gson.fromJson(this.linkJson, SimpleMap.class));
		}
		return null;
	}

	public Photo getPhotoBig() {
		if (photoBigJson != null) {
			return Photo.setPropertiesFromMap(new Photo(), Patchr.gson.fromJson(this.photoBigJson, SimpleMap.class));
		}
		return null;
	}

	public void setPhotoBig(Photo photo) {
		if (photo == null) {
			this.photoBigJson = null;
		}
		else {
			this.photoBigJson = Patchr.gson.toJson(photo, Photo.class);
		}
	}

	public Location getLocation() {
		if (locationJson != null) {
			return Location.setPropertiesFromMap(new Location(), Patchr.gson.fromJson(this.locationJson, SimpleMap.class));
		}
		return null;
	}

	public void setLocation(Location location) {
		if (location == null) {
			this.locationJson = null;
		}
		else {
			this.locationJson = Patchr.gson.toJson(location, Location.class);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Helpers
	 *--------------------------------------------------------------------------------------------*/

	public static void extras(String schema, SimpleMap parameters) {

		if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {

			/* Link counts */
			List<SimpleMap> linkCounts = Arrays.asList(
				new LinkSpec().setTo(LinkDestination.Patches).setType(LinkType.Create).asMap(),
				new LinkSpec().setTo(LinkDestination.Patches).setType(LinkType.Watch).setEnabled(true).asMap());
			parameters.put("linkCount", linkCounts);
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {

			/* Links */
			List<SimpleMap> links = Arrays.asList(
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Like).setFields("_id,type,schema").setFilter(Maps.asMap("_from", UserManager.userId)).asMap());
			parameters.put("links", links);

			/* Linked */
			List<SimpleMap> linked = Arrays.asList(
				new LinkSpec().setTo(LinkDestination.Patches).setType(LinkType.Content).setLimit(1).setFields("_id,name,photo,schema,type").asMap(),
				new LinkSpec().setTo(LinkDestination.Messages).setType(LinkType.Share).setLimit(1).setRefs(Maps.asMap("_owner", "_id,name,photo,schema,type")).asMap(),
				new LinkSpec().setTo(LinkDestination.Patches).setType(LinkType.Share).setLimit(1).asMap(),
				new LinkSpec().setTo(LinkDestination.Users).setType(LinkType.Share).setLimit(5).asMap());
			parameters.put("linked", linked);

			/* Link counts */
			List<SimpleMap> linkCounts = Arrays.asList(
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Like).asMap());
			parameters.put("linkCount", linkCounts);

			/* Refs */
			parameters.put("refs", Maps.asMap("_owner", "_id,name,photo,schema,type"));
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {

			/* Links */
			List<Map<String, Object>> links = Arrays.asList(
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Watch).setFields("_id,type,enabled,mute,schema").setFilter(Maps.asMap("_from", UserManager.userId)).asMap(),
				new LinkSpec().setFrom(LinkDestination.Messages).setType(LinkType.Content).setLimit(1).setFields("_id,type,schema").setFilter(Maps.asMap("_owner", UserManager.userId)).asMap());
			parameters.put("links", links);

			/* Link counts */
			List<Map<String, Object>> linkCounts = Arrays.asList(
				new LinkSpec().setFrom(LinkDestination.Messages).setType(LinkType.Content).asMap(),
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Watch).setEnabled(true).asMap(),
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Watch).setEnabled(false).asMap());
			parameters.put("linkCount", linkCounts);

			/* Refs */
			parameters.put("refs", Maps.asMap("_owner", "_id,name,photo,schema,type"));
		}
	}

	public static String getSchemaForId(String id) {
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

	public static String getCollectionForSchema(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
			return Constants.COLLECTION_ENTITY_BEACON;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			return Constants.COLLECTION_ENTITY_PATCHES;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			return Constants.COLLECTION_ENTITY_USERS;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			return Constants.COLLECTION_ENTITY_MESSAGE;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
			return Constants.COLLECTION_ENTITY_NOTIFICATIONS;
		}
		return null;
	}

	public Map<String, Object> criteria(Boolean activityOnly) {
		if (activityDate == null) return null;
		if (activityOnly) {
			return Maps.asMap("activityDate", Maps.asMap("$gt", activityDate));
		}
		else {
			List<Map<String, Object>> array = new ArrayList<Map<String, Object>>();
			array.add(Maps.asMap("activityDate", Maps.asMap("$gt", activityDate)));
			array.add(Maps.asMap("modifiedDate", Maps.asMap("$gt", modifiedDate)));
			return Maps.asMap("$or", array);
		}
	}
}