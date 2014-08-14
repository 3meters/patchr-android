package com.aircandi.components;

import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Count;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Links;
import com.aircandi.objects.Place;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ucd")
public class EntityCache implements Map<String, Entity> {

	private final Map<String, Entity> mCacheMap = Collections.synchronizedMap(new HashMap<String, Entity>());

	/*--------------------------------------------------------------------------------------------
	 * Cache loading
	 *--------------------------------------------------------------------------------------------*/
	private ServiceResponse dispatch(ServiceRequest serviceRequest) {
		return dispatch(serviceRequest, null);
	}

	private ServiceResponse dispatch(ServiceRequest serviceRequest, Stopwatch stopwatch) {
	    /*
	     * We use this as a choke point for all calls to the aircandi service.
		 */
		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, true, stopwatch);
		return serviceResponse;
	}

	public void decorate(List<Entity> entities, Links linkOptions) {
		for (Entity entity : entities) {
			IEntityController controller = Aircandi.getInstance().getControllerForEntity(entity);
			controller.decorate(entity, linkOptions);
		}
	}

	public ServiceResponse loadEntities(List<String> entityIds, Links linkOptions) {

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("entityIds", (ArrayList<String>) entityIds);
		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Aircandi.getInstance().getCurrentUser().session);
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
                /*
                 * Clear out any cache stamp overrides.
				 */
				for (Entity entity : loadedEntities) {
					if (Aircandi.getInstance().getEntityManager().getCacheStampOverrides().containsKey(entity.id)) {
						Logger.v(this, "Clearing cache stamp override: " + entity.id);
						Aircandi.getInstance().getEntityManager().getCacheStampOverrides().remove(entity.id);
					}
				}

				/*
				 * Keep current user synchronized if we refreshed the current user entity. This
				 * logic also exists in update logic when editing a user entity.
				 */
				if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
					String currentUserId = Aircandi.getInstance().getCurrentUser().id;
					for (Entity entity : loadedEntities) {
						if (entity.id.equals(currentUserId)) {
							/*
							 * We need to update the user that has been persisted for AUTO sign in.
							 */
							final String jsonUser = Json.objectToJson(entity);
							Aircandi.settingsEditor.putString(StringManager.getString(R.string.setting_user), jsonUser);
							Aircandi.settingsEditor.commit();
							/*
							 * Update the global user but retain the session info. We don't need
							 * to call activateCurrentUser because we don't need to refetch link data
							 * or change notification registration.
							 */
							((User) entity).session = Aircandi.getInstance().getCurrentUser().session;
							Aircandi.getInstance().setCurrentUser((User) entity);
						}
					}
				}

				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesForEntity(String forEntityId, Links linkOptions, Cursor cursor, Stopwatch stopwatch) {

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", forEntityId);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		//		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
		//			serviceRequest.setSession(Aircandi.getInstance().getCurrentUser().session);
		//		}

		ServiceResponse serviceResponse = dispatch(serviceRequest, stopwatch);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				for (Entity entity : loadedEntities) {
					/*
					 * Clear out any cache stamp overrides.
					 */
					if (Aircandi.getInstance().getEntityManager().getCacheStampOverrides().containsKey(entity.id)) {
						Logger.v(this, "Clearing cache stamp override: " + entity.id);
						Aircandi.getInstance().getEntityManager().getCacheStampOverrides().remove(entity.id);
					}
					if (cursor != null && cursor.direction != null && cursor.direction.equals("out")) {
						entity.fromId = forEntityId;
					}
					else {
						entity.toId = forEntityId;
					}
				}

				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesByProximity(List<String> beaconIds, Links linkOptions, Cursor cursor, String installId, Stopwatch stopwatch) {

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
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesByProximity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Aircandi.getInstance().getCurrentUser().session);
		}

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call started");
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest, stopwatch);

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
					/*
					 * Clear out any cache stamp overrides.
					 */
					if (Aircandi.getInstance().getEntityManager().getCacheStampOverrides().containsKey(entity.id)) {
						Logger.v(this, "Clearing cache stamp override: " + entity.id);
						Aircandi.getInstance().getEntityManager().getCacheStampOverrides().remove(entity.id);
					}
				}
				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
			}

		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesNearLocation(AirLocation location, Links linkOptions, List<String> excludePlaceIds) {

		final Bundle parameters = new Bundle();

		parameters.putString("location", "object:" + Json.objectToJson(location));
		parameters.putInt("limit", Aircandi.applicationContext.getResources().getInteger(R.integer.limit_places_radar));
		parameters.putInt("timeout", ServiceConstants.TIMEOUT_PLACE_QUERIES);
		parameters.putInt("radius", ServiceConstants.PLACE_NEAR_RADIUS);
		parameters.putString("provider", ServiceConstants.PLACE_NEAR_PROVIDERS);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (excludePlaceIds != null && excludePlaceIds.size() > 0) {
			parameters.putStringArrayList("excludePlaceIds", (ArrayList<String>) excludePlaceIds);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_PLACES + "near")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Aircandi.getInstance().getCurrentUser().session);
		}

		ServiceResponse serviceResponse = Aircandi.getInstance().getEntityManager().dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);

			/* Do a bit of fixup */
			final List<Entity> entities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;
			for (Entity entity : entities) {
				entity.foundByProximity = false;

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {

					Place place = (Place) entity;
					place.locked = false;
					place.enabled = true;
				}
			}

			/* Push place entities to cache */
			decorate(entities, new Links(false, null));
			upsertEntities(entities);
		}
		return serviceResponse;
	}

	/*--------------------------------------------------------------------------------------------
	 * Cache updates
	 *--------------------------------------------------------------------------------------------*/
	private void upsertEntities(List<Entity> entities) {
		for (Entity entity : entities) {
			upsertEntity(entity);
		}
	}

	public synchronized Entity upsertEntity(Entity entity) {
		removeEntityTree(entity.id);
		put(entity.id, entity);
		return get(entity.id);
	}

	public synchronized void updateEntityUser(Entity entity) {
		/*
		 * Updates user objects that are embedded in entities.
		 */
		User user = (User) entity;
		for (Entry<String, Entity> entry : entrySet()) {
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

	public void addLink(String fromId, String toId, String type, Boolean enabled, Shortcut fromShortcut, Shortcut toShortcut) {

		Long time = DateTime.nowDate().getTime();

		Entity toEntity = get(toId);
		if (toEntity != null && toEntity.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
			toEntity = Aircandi.getInstance().getCurrentUser();
		}

		if (toEntity != null) {

			if (toEntity.linksIn == null) {
				toEntity.linksIn = new ArrayList<Link>();
			}

			if (toEntity.linksInCounts == null) {
				toEntity.linksInCounts = new ArrayList<Count>();
				toEntity.linksInCounts.add(new Count(type, fromShortcut.schema, enabled, 1));
			}
			else if (toEntity.getCount(type, fromShortcut.schema, enabled, Direction.in) == null) {
				toEntity.linksInCounts.add(new Count(type, fromShortcut.schema, enabled, 1));
			}
			else {
				toEntity.getCount(type, fromShortcut.schema, enabled, Direction.in).count = toEntity.getCount(type, fromShortcut.schema, enabled, Direction.in).count
						.intValue() + 1;
			}

			Link link = new Link(fromId, toId, type, fromShortcut.schema);
			link.enabled = enabled;
			link.modifiedDate = time;

			if (fromShortcut != null) {
				link.shortcut = fromShortcut;
			}

			toEntity.linksIn.add(link);
			toEntity.activityDate = time;
		}
		/*
		 * Fixup out links too.
		 */
		Entity fromEntity = get(fromId);
		if (fromEntity != null && fromEntity.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
			fromEntity = Aircandi.getInstance().getCurrentUser();
		}

		if (fromEntity != null) {

			if (fromEntity.linksOut == null) {
				fromEntity.linksOut = new ArrayList<Link>();
			}

			if (fromEntity.linksOutCounts == null) {
				fromEntity.linksOutCounts = new ArrayList<Count>();
				fromEntity.linksOutCounts.add(new Count(type, toShortcut.schema, enabled, 1));
			}
			else if (fromEntity.getCount(type, toShortcut.schema, enabled, Direction.out) == null) {
				fromEntity.linksOutCounts.add(new Count(type, toShortcut.schema, enabled, 1));
			}
			else {
				fromEntity.getCount(type, toShortcut.schema, enabled, Direction.out).count = fromEntity.getCount(type, toShortcut.schema, enabled,
						Direction.out).count.intValue() + 1;
			}

			Link link = new Link(fromId, toId, type, toShortcut.schema);
			link.enabled = enabled;
			link.modifiedDate = time;

			if (toShortcut != null) {
				link.shortcut = toShortcut;
			}

			fromEntity.linksOut.add(0, link);
			fromEntity.activityDate = time;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Cache deletes
	 *--------------------------------------------------------------------------------------------*/
	public synchronized Entity removeEntityTree(String entityId) {
		/*
		 * Clean out entity and every entity related to entity. Is not recursive
		 */
		Entity removedEntity = remove(entityId);
		if (removedEntity != null) {
			/*
			 * getLinked..() with traverse = true will return entities that are multiple links away.
			 * We get both strong and weak linked entities.
			 */
			List<String> types = new ArrayList<String>();
			types.add(Constants.TYPE_LINK_CONTENT);
			List<Entity> entities = (List<Entity>) removedEntity.getLinkedEntitiesByLinkTypeAndSchema(types, null, Direction.in, true);
			for (Entity childEntity : entities) {
				remove(childEntity.id);
			}
		}
		return removedEntity;
	}

	public synchronized Integer removeEntities(String schema, String type, Boolean foundByProximity) {

		Integer removeCount = 0;
		final Iterator iterEntities = keySet().iterator();
		Entity entity;
		while (iterEntities.hasNext()) {
			entity = get(iterEntities.next());
			if (schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
				if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
					if (foundByProximity == null || entity.foundByProximity.equals(foundByProximity)) {
						iterEntities.remove();
						removeCount++;
					}
				}
			}
		}
		return removeCount;
	}

	public void removeLink(String fromId, String toId, String type, Boolean enabled) {

		Long time = DateTime.nowDate().getTime();

		Entity toEntity = get(toId);
		if (toEntity != null && toEntity.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
			toEntity = Aircandi.getInstance().getCurrentUser();
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
		Entity fromEntity = get(fromId);
		if (fromEntity != null && fromEntity.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
			fromEntity = Aircandi.getInstance().getCurrentUser();
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

	/*--------------------------------------------------------------------------------------------
	 * Cache reads
	 *--------------------------------------------------------------------------------------------*/
	public synchronized List<? extends Entity> getCacheEntities(String schema, String type, Integer radius, Boolean proximity) {
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = keySet().iterator();
		Entity entity;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (!entity.isHidden()) {
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

	@SuppressWarnings("ucd")
	public synchronized List<? extends Entity> getCacheEntitiesForEntity(String entityId, String schema, String type, Integer radius, Boolean proximity) {
		/*
		 * We rely on the toId property instead of traversing links.
		 */
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = keySet().iterator();
		Entity entity;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if ((entity.toId != null && entity.toId.equals(entityId)) || (entity.fromId != null && entity.fromId.equals(entityId))) {
				if (!entity.isHidden()) {
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
		}
		return entities;
	}

	/*--------------------------------------------------------------------------------------------
	 * Map methods
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void clear() {
		mCacheMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return mCacheMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return mCacheMap.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Entity>> entrySet() {
		return mCacheMap.entrySet();
	}

	@Override
	public Entity get(Object key) {
		return mCacheMap.get(key);
	}

	@Override
	public boolean isEmpty() {
		return mCacheMap.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return mCacheMap.keySet();
	}

	@Override
	public Entity put(String key, Entity value) {
		return mCacheMap.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Entity> map) {
		mCacheMap.putAll(map);
	}

	@Override
	public Entity remove(Object key) {
		return mCacheMap.remove(key);
	}

	@Override
	public int size() {
		return mCacheMap.size();
	}

	@Override
	public Collection<Entity> values() {
		return mCacheMap.values();
	}

}
