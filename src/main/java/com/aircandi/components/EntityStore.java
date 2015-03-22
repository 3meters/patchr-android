package com.aircandi.components;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Count;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkSpec;
import com.aircandi.objects.ServiceData;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.User;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceResponse;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("ucd")
public class EntityStore {

	private final Map<String, Entity> mCacheMap = new ConcurrentHashMap<String, Entity>();

	/*--------------------------------------------------------------------------------------------
	 * Store loading from service
	 *--------------------------------------------------------------------------------------------*/

	ServiceResponse loadEntities(List<String> entityIds, LinkSpec linkOptions, CacheStamp cacheStamp, Object tag) {

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("entityIds", (ArrayList<String>) entityIds);

		if (cacheStamp != null) {

			StringBuilder builder = new StringBuilder("object:");
			if (cacheStamp.activityDate != null && cacheStamp.modifiedDate != null) {
				builder.append("{\"$or\":["
						+ "{\"activityDate\":{\"$gt\":" + cacheStamp.activityDate.longValue() + "}},"
						+ "{\"modifiedDate\":{\"$gt\":" + cacheStamp.modifiedDate.longValue() + "}}"
						+ "]}");
				parameters.putString("where", builder.toString());
			}
			else if (cacheStamp.activityDate != null) {
				builder.append("{\"activityDate\":{\"$gt\":" + cacheStamp.activityDate.longValue() + "}}");
				parameters.putString("where", builder.toString());
			}
			else if (cacheStamp.modifiedDate != null) {
				builder.append("{\"modifiedDate\":{\"$gt\":" + cacheStamp.modifiedDate.longValue() + "}}");
				parameters.putString("where", builder.toString());
			}
		}

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				/*
				 * Keep current user synchronized if we refreshed the current user entity. This
				 * logic also exists in update logic when editing a user entity.
				 */
				if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
					String currentUserId = Patchr.getInstance().getCurrentUser().id;
					for (Entity entity : loadedEntities) {
						if (entity.id.equals(currentUserId)) {
							/*
							 * We need to update the user that has been persisted for AUTO sign in.
							 */
							final String jsonUser = Json.objectToJson(entity);
							Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), jsonUser);
							Patchr.settingsEditor.commit();
							/*
							 * Update the global user but retain the session info. We don't need
							 * to call activateCurrentUser because we don't need to refetch link data
							 * or change notification registration.
							 */
							((User) entity).session = Patchr.getInstance().getCurrentUser().session;
							Patchr.getInstance().setCurrentUser((User) entity, false);
						}
					}
				}

				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	ServiceResponse loadEntitiesForEntity(String forEntityId, LinkSpec linkOptions, Cursor cursor, CacheStamp cacheStamp, Stopwatch stopwatch, Object tag) {

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", forEntityId);

		if (cacheStamp != null && cacheStamp.activityDate != null) {
			parameters.putString("where", "object:"
					+ "{\"activityDate\":{\"$gt\":" + cacheStamp.activityDate.longValue() + "}}");
		}

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON)
				.setStopwatch(stopwatch);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				for (Entity entity : loadedEntities) {
					if (cursor != null && cursor.direction != null && cursor.direction.equals("out")) {
						entity.fromId = forEntityId;
					}
					else {
						entity.toId = forEntityId;
					}
				}

				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	ServiceResponse loadEntitiesByProximity(List<String> beaconIds, LinkSpec linkOptions, Cursor cursor, String installId, Object tag, Stopwatch stopwatch) {

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("beaconIds", (ArrayList<String>) beaconIds);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		if (installId != null) {
			parameters.putString("installId", installId);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesByProximity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON)
				.setStopwatch(stopwatch);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call started");
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call complete");
		}

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (stopwatch != null) {
				stopwatch.segmentTime("Load entities: entities deserialized");
				stopwatch.segmentTime("Load entities: service processing time: " + ((ServiceData) serviceResponse.data).time);
			}

			if (loadedEntities != null && loadedEntities.size() > 0) {
				for (Entity entity : loadedEntities) {
					entity.foundByProximity = true;
				}

				synchronized (this) {

					/* Clean out all patches found via proximity before shoving in the latest */
					Integer removeCount = removeEntities(Constants.SCHEMA_ENTITY_PATCH, Constants.TYPE_ANY, true /* found by proximity */);
					Logger.v(this, "Removed proximity places from cache: count = " + String.valueOf(removeCount));

					/* Push patch entities to cache */
					upsertEntities(loadedEntities);
				}
			}
		}

		return serviceResponse;
	}

	ServiceResponse loadEntitiesNearLocation(AirLocation location, LinkSpec linkOptions, String installId, List<String> excludeIds, Object tag) {

		final Bundle parameters = new Bundle();

		parameters.putString("location", "object:" + Json.objectToJson(location));
		parameters.putInt("limit", Patchr.applicationContext.getResources().getInteger(R.integer.limit_places_radar));
		parameters.putInt("radius", Constants.PATCH_NEAR_RADIUS);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (installId != null) {
			parameters.putString("installId", installId);
		}

		if (excludeIds != null && excludeIds.size() > 0) {
			parameters.putStringArrayList("excludeIds", (ArrayList<String>) excludeIds);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_PATCHES + "near")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);

			/* Do a bit of fixup */
			final List<Entity> entities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;
			for (Entity entity : entities) {
				entity.foundByProximity = false;
			}

			synchronized (this) {

				/* Clean out all patches not found via proximity */
				Integer removeCount = removeEntities(Constants.SCHEMA_ENTITY_PATCH, Constants.TYPE_ANY, false /* not found by proximity */);
				Logger.v(this, "Removed synthetic places from cache: count = " + String.valueOf(removeCount));

				/* Push patch entities to cache */
				upsertEntities(entities);
			}
		}

		return serviceResponse;
	}

	/*--------------------------------------------------------------------------------------------
	 * Store reads (local only)
	 *--------------------------------------------------------------------------------------------*/

	Entity getStoreEntity(Object key) {
		//noinspection SuspiciousMethodCalls
		return mCacheMap.get(key);
	}

	@SuppressWarnings("ConstantConditions")
	synchronized List<? extends Entity> getStoreEntities(String schema, String type, Integer radius, Boolean proximity) {
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = mCacheMap.keySet().iterator();
		Entity entity;

		while (iter.hasNext()) {
			//noinspection SuspiciousMethodCalls
			entity = mCacheMap.get(iter.next());
			if (schema == null || schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
				if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
					if (proximity == null || entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true) != null) {
						if (radius == null) {
							entities.add(entity);
						}
						else {
							Float distance = entity.getDistance(true);
							if (distance != null && distance <= radius) {
								entities.add(entity);
							}
							else if (distance == null) {
								Beacon beacon = entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, false);
								if (beacon != null) {
									entities.add(entity);
								}
							}
						}
					}
				}
			}
		}
		return entities;
	}

	@SuppressWarnings({"ucd", "ConstantConditions"})
	synchronized List<? extends Entity> getStoreEntitiesForEntity(String entityId, String schema, String type, Integer radius, Boolean proximity) {
		/*
		 * We rely on the toId property instead of traversing links.
		 */
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = mCacheMap.keySet().iterator();
		Entity entity;
		while (iter.hasNext()) {
			//noinspection SuspiciousMethodCalls
			entity = mCacheMap.get(iter.next());
			if ((entity.toId != null && entity.toId.equals(entityId)) || (entity.fromId != null && entity.fromId.equals(entityId))) {
				if (schema == null || schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
					if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
						if (proximity == null || entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true) != null) {
							if (radius == null) {
								entities.add(entity);
							}
							else {
								Float distance = entity.getDistance(true);
								if (distance != null && distance <= radius) {
									entities.add(entity);
								}
								else if (distance == null) {
									Beacon beacon = entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, false);
									if (beacon != null) {
										entities.add(entity);
									}
								}
							}
						}
					}
				}
			}
		}
		return entities;
	}

	/*--------------------------------------------------------------------------------------------
	 * Store updates (local only)
	 *--------------------------------------------------------------------------------------------*/

	private void upsertEntities(List<Entity> entities) {
		for (Entity entity : entities) {
			upsertEntity(entity);
		}
	}

	synchronized Entity upsertEntity(Entity entity) {

		/* Replace in cache */
		mCacheMap.put(entity.id, entity);

		/* Clean out linked entities so they are refetched as needed. */
		removeLinkedEntities(entity.id);

		return mCacheMap.get(entity.id);
	}

	synchronized void fixupEntityUser(Entity entity) {
		/*
		 * Updates user objects that are embedded in entities. We allow optimistic updating
		 * of the store because users will expect to see their changes and we don't want to
		 * refetch every entity they have a relationship with.
		 */
		User user = (User) entity;
		for (Map.Entry<String, Entity> entry : mCacheMap.entrySet()) {
			if (entry.getValue().creatorId != null && entry.getValue().creatorId.equals(user.id)) {
				if (entry.getValue().creator != null) {
					if (user.photo != null) {
						entry.getValue().creator.photo = user.photo.clone();
					}
					if (user.area != null) {
						entry.getValue().creator.area = user.area;
					}
					if (user.name != null) {
						entry.getValue().creator.name = user.name;
					}
				}
			}
			if (entry.getValue().ownerId != null && entry.getValue().ownerId.equals(user.id)) {
				if (entry.getValue().owner != null) {
					if (user.photo != null) {
						entry.getValue().owner.photo = user.photo.clone();
					}
					if (user.area != null) {
						entry.getValue().owner.area = user.area;
					}
					if (user.name != null) {
						entry.getValue().owner.name = user.name;
					}
				}
			}
			if (entry.getValue().modifierId != null && entry.getValue().modifierId.equals(user.id)) {
				if (entry.getValue().modifier != null) {
					if (user.photo != null) {
						entry.getValue().modifier.photo = user.photo.clone();
					}
					if (user.area != null) {
						entry.getValue().modifier.area = user.area;
					}
					if (user.name != null) {
						entry.getValue().modifier.name = user.name;
					}
				}
			}
		}
	}

	void fixupAddLink(String fromId, String toId, String type, Boolean enabled, Shortcut fromShortcut, Shortcut toShortcut) {
		/*
		 * Optimistically add a link to the store.
		 */
		Long time = DateTime.nowDate().getTime();

		Entity toEntity = mCacheMap.get(toId);
		if (toEntity != null && toEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			toEntity = Patchr.getInstance().getCurrentUser();
		}

		if (toEntity != null) {

			if (toEntity.linksIn == null) {
				toEntity.linksIn = new ArrayList<Link>();
			}

			if (toEntity.linksInCounts == null) {
				toEntity.linksInCounts = new ArrayList<Count>();
				toEntity.linksInCounts.add(new Count(type, fromShortcut.schema, enabled, 1));
			}
			else {
				Count count = toEntity.getCount(type, fromShortcut.schema, enabled, Direction.in);
				if (count == null) {
					toEntity.linksInCounts.add(new Count(type, fromShortcut.schema, enabled, 1));
				}
				else {
					count.count = count.count.intValue() + 1;
				}
			}

			Link link = new Link(fromId, toId, type, fromShortcut.schema);
			link.enabled = enabled;
			link.modifiedDate = time;
			link.shortcut = fromShortcut;

			toEntity.linksIn.add(link);
			toEntity.activityDate = time;
		}
		/*
		 * Fixup out links too.
		 */
		Entity fromEntity = mCacheMap.get(fromId);
		if (fromEntity != null && fromEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			fromEntity = Patchr.getInstance().getCurrentUser();
		}

		if (fromEntity != null) {

			if (fromEntity.linksOut == null) {
				fromEntity.linksOut = new ArrayList<Link>();
			}

			if (fromEntity.linksOutCounts == null) {
				fromEntity.linksOutCounts = new ArrayList<Count>();
				fromEntity.linksOutCounts.add(new Count(type, toShortcut.schema, enabled, 1));
			}
			else {
				Count count = fromEntity.getCount(type, toShortcut.schema, enabled, Direction.out);
				if (count == null) {
					fromEntity.linksOutCounts.add(new Count(type, toShortcut.schema, enabled, 1));
				}
				else {
					count.count = count.count.intValue() + 1;
				}
			}

			Link link = new Link(fromId, toId, type, toShortcut.schema);
			link.enabled = enabled;
			link.modifiedDate = time;
			link.shortcut = toShortcut;

			fromEntity.linksOut.add(0, link);
			fromEntity.activityDate = time;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Store deletes (local only)
	 *--------------------------------------------------------------------------------------------*/

	void clearStore() {
		mCacheMap.clear();
	}

	synchronized Entity removeEntityTree(String entityId) {
		/*
		 * Clean out entity and every entity related to entity. Is not recursive
		 */
		Entity removedEntity = mCacheMap.remove(entityId);
		if (removedEntity != null) {
			/*
			 * getLinked..() with traverse = true will return entities that are multiple links away.
			 * We get both strong and weak linked entities.
			 */
			List<String> types = new ArrayList<String>();
			types.add(Constants.TYPE_LINK_CONTENT);
			List<Entity> entities = (List<Entity>) removedEntity.getLinkedEntitiesByLinkTypeAndSchema(types, null, Direction.in, true);
			for (Entity childEntity : entities) {
				mCacheMap.remove(childEntity.id);
			}
		}
		return removedEntity;
	}

	synchronized Entity removeLinkedEntities(String entityId) {
		/*
		 * Clean out entity and every entity related to entity. Is not recursive
		 */
		Entity entity = mCacheMap.get(entityId);
		if (entity != null) {
			/*
			 * getLinked..() with traverse = true will return entities that are multiple links away.
			 * We get both strong and weak linked entities.
			 */
			List<String> types = new ArrayList<String>();
			types.add(Constants.TYPE_LINK_CONTENT);
			List<Entity> entities = (List<Entity>) entity.getLinkedEntitiesByLinkTypeAndSchema(types, null, Direction.in, true);
			for (Entity childEntity : entities) {
				mCacheMap.remove(childEntity.id);
			}
		}
		return entity;
	}

	synchronized Integer removeEntities(@NonNull String schema, @NonNull String type, Boolean foundByProximity) {

		Integer removeCount = 0;
		final Iterator iterEntities = mCacheMap.keySet().iterator();
		Entity entity;
		while (iterEntities.hasNext()) {
			//noinspection SuspiciousMethodCalls
			entity = mCacheMap.get(iterEntities.next());
			if (schema.equals(Constants.SCHEMA_ANY) || (entity.schema != null && entity.schema.equals(schema))) {
				if (type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
					if (foundByProximity == null || entity.foundByProximity.equals(foundByProximity)) {
						iterEntities.remove();
						removeCount++;
					}
				}
			}
		}
		return removeCount;
	}

	void fixupRemoveLink(String fromId, String toId, String type, Boolean enabled) {
		/*
		 * Optimistically remove a link from the store. Links are sprinkled across entities
		 * so it is reasonable to proactively fixup rather than have to refetch entities to
		 * get fresh links. Our primary purpose is that users expect to see their changes
		 * reflected in a consistent way.
		 */
		Long time = DateTime.nowDate().getTime();

		Entity toEntity = mCacheMap.get(toId);
		if (toEntity != null && toEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			toEntity = Patchr.getInstance().getCurrentUser();
		}

		if (toEntity != null) {
			if (toEntity.linksIn != null) {

				Iterator<Link> iterLinks = toEntity.linksIn.iterator();
				while (iterLinks.hasNext()) {
					Link link = iterLinks.next();
					if (link.fromId != null && link.fromId.equals(fromId) && link.type.equals(type)) {

						toEntity.activityDate = time;

						/* Adjust the count */
						if (toEntity.linksInCounts != null) {
							Count count = toEntity.getCount(type, link.targetSchema, enabled, Direction.in);
							if (count != null) {
								count.count = count.count.intValue() - 1;
							}
						}

						iterLinks.remove();
					}
				}
			}
		}

		/*
		 * Fixup out links too
		 */
		Entity fromEntity = mCacheMap.get(fromId);
		if (fromEntity != null && fromEntity.id.equals(Patchr.getInstance().getCurrentUser().id)) {
			fromEntity = Patchr.getInstance().getCurrentUser();
		}

		if (fromEntity != null) {
			if (fromEntity.linksOut != null) {
				Iterator<Link> iterLinks = fromEntity.linksOut.iterator();
				while (iterLinks.hasNext()) {
					Link link = iterLinks.next();
					if (link.toId != null && link.toId.equals(toId) && link.type.equals(type)) {

						fromEntity.activityDate = time;

						/* Adjust the count */
						if (fromEntity.linksOutCounts != null) {
							Count count = fromEntity.getCount(type, link.targetSchema, enabled, Direction.out);
							if (count != null) {
								count.count = count.count.intValue() - 1;
							}
						}

						iterLinks.remove();
					}
				}
			}
		}
	}
}
