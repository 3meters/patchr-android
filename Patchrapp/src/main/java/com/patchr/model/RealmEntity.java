package com.patchr.model;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.LocationManager;
import com.patchr.components.UserManager;
import com.patchr.objects.LinkCount;
import com.patchr.objects.LinkDestination;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.LinkType;
import com.patchr.objects.MemberStatus;
import com.patchr.objects.Session;
import com.patchr.objects.SimpleMap;
import com.patchr.utilities.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
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

	/* Service calculated fields */
	public RealmEntity owner;
	public RealmEntity creator;
	public RealmEntity modifier;
	public RealmEntity patch;
	public Link        link;
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
	@Ignore public  Session     session;
	@Ignore private PhoneNumber phone;
	@Ignore private Photo       photo;
	@Ignore private Location    location;
	@Ignore private Photo       photoBig;

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

			if (map.get("photo") != null) {
				String photoJson = Patchr.gson.toJson(map.get("photo"), SimpleMap.class);
				entity.photoJson = photoJson;
				entity.photo = Photo.setPropertiesFromMap(new Photo(), (Map<String, Object>) map.get("photo"));
			}
			if (map.get("location") != null) {
				String locationJson = Patchr.gson.toJson(map.get("location"), SimpleMap.class);
				entity.locationJson = locationJson;
				entity.location = Location.setPropertiesFromMap(new Location(), (Map<String, Object>) map.get("location"));
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
				 * This is the only place that uses the Link object. We use it to support watch link state and management.
				 * Will get pulled in on a user for a list of users watching a patch and on patches when showing patches a
				 * user is watching (let's us show that a watch request is pending/approved).
				 */
				entity.link = Link.setPropertiesFromMap(new Link(), (Map<String, Object>) map.get("link"));
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
					entity.phone = PhoneNumber.setPropertiesFromMap(new PhoneNumber(), (Map<String, Object>) map.get("phone"));
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
							entity.recipients.add(RealmEntity.setPropertiesFromMap(new RealmEntity(), linkMap, true));
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
					entity.photoBig = Photo.setPropertiesFromMap(new Photo(), (Map<String, Object>) map.get("photoBig"));
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

	public static RealmEntity copyToRealmOrUpdate(RealmEntity entity) {
		Realm realm = Realm.getDefaultInstance();
		realm.beginTransaction();
		RealmEntity realmEntity = copyToRealmOrUpdate(realm, entity);
		realm.commitTransaction();
		realm.close();
		return realmEntity;
	}

	public static RealmEntity copyToRealmOrUpdate(Realm realm, RealmEntity entity) {
		RealmEntity realmEntity = realm.copyToRealmOrUpdate(entity);
		return realmEntity;
	}

	public static void deleteNestedObjectsFromRealm(Realm realm, String id) {
		RealmEntity entity = realm.where(RealmEntity.class).equalTo("id", id).findFirst();
		deleteNestedObjectsFromRealm(entity);
	}

	public static void deleteNestedObjectsFromRealm(RealmEntity entity) {
		if (entity != null && entity.isValid()) {

			/* Shortcuts */
			if (entity.creator != null && entity.creator.isValid())
				entity.creator.deleteFromRealm();

			if (entity.owner != null && entity.owner.isValid())
				entity.owner.deleteFromRealm();

			if (entity.modifier != null && entity.modifier.isValid())
				entity.modifier.deleteFromRealm();

			if (entity.patch != null && entity.patch.isValid())
				entity.patch.deleteFromRealm();

			if (entity.message != null && entity.message.isValid())
				entity.message.deleteFromRealm();

			if (entity.recipients != null && entity.recipients.isValid()) {
				entity.recipients.deleteAllFromRealm();
			}
		}
	}

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
			if (UserManager.shared().authenticated()) {
				List<SimpleMap> links = Arrays.asList(
					new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Like).setFields("_id,type,schema").setFilter(Maps.asMap("_from", UserManager.userId)).asMap());
				parameters.put("links", links);
			}

			/* Linked */
			List<SimpleMap> linked = Arrays.asList(
				new LinkSpec().setTo(LinkDestination.Messages).setType(LinkType.Share).setLimit(1).setRefs(Maps.asMap("_creator", "_id,name,photo,schema,type")).asMap(),
				new LinkSpec().setTo(LinkDestination.Patches).setType(LinkType.Content).setLimit(1).setFields("_id,name,photo,schema,type").asMap(),
				new LinkSpec().setTo(LinkDestination.Patches).setType(LinkType.Share).setLimit(1).asMap(),
				new LinkSpec().setTo(LinkDestination.Users).setType(LinkType.Share).setLimit(5).asMap());
			parameters.put("linked", linked);

			/* Link counts */
			List<SimpleMap> linkCounts = Arrays.asList(
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Like).asMap());
			parameters.put("linkCount", linkCounts);

			/* Refs */
			parameters.put("refs", Maps.asMap("_creator", "_id,name,photo,schema,type"));
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {

			/* Links */
			if (UserManager.shared().authenticated()) {
				List<Map<String, Object>> links = Arrays.asList(
					new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Watch).setFields("_id,type,enabled,mute,schema").setFilter(Maps.asMap("_from", UserManager.userId)).asMap(),
					new LinkSpec().setFrom(LinkDestination.Messages).setType(LinkType.Content).setLimit(1).setFields("_id,type,schema").setFilter(Maps.asMap("_creator", UserManager.userId)).asMap());
				parameters.put("links", links);
			}

			/* Link counts */
			List<Map<String, Object>> linkCounts = Arrays.asList(
				new LinkSpec().setFrom(LinkDestination.Messages).setType(LinkType.Content).asMap(),
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Watch).setEnabled(true).asMap(),
				new LinkSpec().setFrom(LinkDestination.Users).setType(LinkType.Watch).setEnabled(false).asMap());
			parameters.put("linkCount", linkCounts);

			/* Refs */
			parameters.put("refs", Maps.asMap("_creator", "_id,name,photo,schema,type"));
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

	public Boolean isOwnedByCurrentUser() {
		Boolean owned = (ownerId != null
			&& UserManager.shared().authenticated()
			&& ownerId.equals(UserManager.currentUser.id));
		return owned;
	}

	public Boolean isVisibleToCurrentUser() {
		if (visibility != null && !visibility.equals(Constants.PRIVACY_PUBLIC) && !isOwnedByCurrentUser()) {
			return userMemberStatus.equals(MemberStatus.Member);
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
		final RealmEntity beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) return beacon.id;
		return null;
	}

	public Long idAsLong() {
		return Long.parseLong(this.id.replaceAll("[^0-9]", "").substring(1));
	}

	public PhoneNumber getPhone() {
		if (this.phone == null && this.phoneJson != null) {
			this.phone = PhoneNumber.setPropertiesFromMap(new PhoneNumber(), Patchr.gson.fromJson(this.phoneJson, SimpleMap.class));
		}
		return this.phone;
	}

	public Photo getPhoto() {
		if (this.photo == null && this.photoJson != null) {
			this.photo = Photo.setPropertiesFromMap(new Photo(), Patchr.gson.fromJson(this.photoJson, SimpleMap.class));
		}
		return this.photo;
	}

	public void setPhoto(Photo photo) {
		if (photo == null) {
			this.photo = null;
			this.photoJson = null;
		}
		else {
			this.photo = photo;
			this.photoJson = Patchr.gson.toJson(photo, Photo.class);
		}
	}

	public Photo getPhotoBig() {
		if (this.photoBig == null && this.photoBigJson != null) {
			this.photoBig = Photo.setPropertiesFromMap(new Photo(), Patchr.gson.fromJson(this.photoBigJson, SimpleMap.class));
		}
		return this.photoBig;
	}

	public void setPhotoBig(Photo photo) {
		if (photo == null) {
			this.photoBig = null;
			this.photoBigJson = null;
		}
		else {
			this.photoBig = photo;
			this.photoBigJson = Patchr.gson.toJson(photo, Photo.class);
		}
	}

	public void setLocation(Location location) {
		if (location == null) {
			this.location = null;
			this.locationJson = null;
		}
		else {
			this.location = location;
			this.locationJson = Patchr.gson.toJson(location, Location.class);
		}
	}

	public Location getLocation() {

		Location _location = null;

		if (this.location == null && this.locationJson != null) {
			this.location = Location.setPropertiesFromMap(new Location(), Patchr.gson.fromJson(this.locationJson, SimpleMap.class));
		}

		if (this.location != null
			&& this.location.lat != null
			&& this.location.lng != null) {
			return this.location;
		}
		else {
			final RealmEntity beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null && beacon.location != null && beacon.location.lat != null && beacon.location.lng != null) {
				return beacon.location;
			}
		}
		return null;
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
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortBySortDate implements Comparator<RealmEntity> {

		@Override
		public int compare(@NonNull RealmEntity object1, @NonNull RealmEntity object2) {
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

	public static class SortBySortDateAscending implements Comparator<RealmEntity> {

		@Override
		public int compare(@NonNull RealmEntity object1, @NonNull RealmEntity object2) {
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

	public static class SortByProximityAndDistance implements Comparator<RealmEntity> {

		@Override public int compare(RealmEntity object1, RealmEntity object2) {
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

	public static class SortBySignalLevel implements Comparator<RealmEntity> {

		@Override public int compare(RealmEntity object1, RealmEntity object2) {

			if (object1.signal == null && object2.signal == null)
				return 0;
			else if (object1.signal == null)
				return 1;
			else if (object2.signal == null)
				return -1;
			else {
				if ((object1.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
					> (object2.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE))
					return -1;
				else if ((object1.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
					< (object2.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE))
					return 1;
				else
					return 0;
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

	public static class Priority {
		public static Integer ONE   = 1;    // All bells and whistles for in-app notifications
		public static Integer TWO   = 2;    // Mute chirping/toast for in-app notifications
		public static Integer THREE = 3;
	}

	public static class NotificationType {
		/*
		 * Used to determine icon to display in notification ui.
		 */
		public static String MESSAGE = "message";
		public static String MEDIA   = "media";
		public static String PLACE   = "patch";
		public static String SHARE   = "share";
		public static String WATCH   = "watch";
		public static String LIKE    = "like";
	}

	public static class TriggerCategory {
		/*
		 * Used to characterize why the current user is receiving the notification.
		 * Used to enable/disable status notifications based on user preference settings.
		 */
		public static String NEARBY = "nearby";         // sent because this user is nearby
		public static String WATCH  = "watch";          // sent because this user is watching the target entity
		public static String OWN    = "own";            // sent because this user is the owner of the target entity
		public static String NONE   = "none";
	}

	public static class EventCategory {
		/*
		 * Used to characterize the action associated with the notification.
		 * Used to enable/disable status notifications based on user preference settings.
		 */
		public static String INSERT = "insert";         // notification about a patch|message insert
		public static String SHARE  = "share";          // notification about patch|message|photo share
		public static String LIKE   = "like";           // notification about patch|message like
		public static String WATCH  = "watch";          // notification about patch watch
		public static String NONE   = "none";
	}
}