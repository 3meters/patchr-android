package com.aircandi.components;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager.Tag;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.CacheStamp.StampSource;
import com.aircandi.objects.Category;
import com.aircandi.objects.Count;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Document;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Install;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.Log;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoType;
import com.aircandi.objects.Proximity;
import com.aircandi.objects.ServiceBase.UpdateScope;
import com.aircandi.objects.ServiceData;
import com.aircandi.objects.ServiceEntry;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.User;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceResponse;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EntityManager {

	private static final EntityCache         mEntityCache         = new EntityCache();
	private              Map<String, String> mCacheStampOverrides = new HashMap<String, String>();
	private Number mActivityDate;
	private Links  mLinks;
	/*
	 * Categories are cached by a background thread.
	 */
	private List<Category> mCategories = Collections.synchronizedList(new ArrayList<Category>());

	/*--------------------------------------------------------------------------------------------
	 * Cache queries
	 *--------------------------------------------------------------------------------------------*/

	public static Entity getCacheEntity(String entityId) {
		return mEntityCache.get(entityId);
	}

	/*--------------------------------------------------------------------------------------------
	 * Combo service/cache queries
	 *--------------------------------------------------------------------------------------------*/

	public synchronized ModelResult getEntity(String entityId, Boolean refresh, Links linkOptions) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final ModelResult result = getEntities(Arrays.asList(entityId), refresh, linkOptions);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final List<Entity> entities = (List<Entity>) result.data;
			if (entities.size() > 0) {
				result.data = entities.get(0);
			}
			else {
				result.data = null;
			}
		}
		return result;
	}

	private synchronized ModelResult getEntities(List<String> entityIds, Boolean refresh, Links linkOptions) {
		/*
		 * Results in a service request if missing entities or refresh is true.
		 */
		final ModelResult result = new ModelResult();

		final List<String> loadEntityIds = new ArrayList<String>();
		List<Entity> entities = new ArrayList<Entity>();

		for (String entityId : entityIds) {
			Entity entity = mEntityCache.get(entityId);
			if (refresh || entity == null) {
				loadEntityIds.add(entityId);
			}
			else {
				entities.add(entity);
			}
		}

		result.data = entities;

		if (loadEntityIds.size() > 0) {
			result.serviceResponse = mEntityCache.loadEntities(loadEntityIds, linkOptions);
			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
				ServiceData serviceData = (ServiceData) result.serviceResponse.data;
				result.data = serviceData.data;
			}
		}
		return result;
	}

	public synchronized ModelResult loadEntitiesForEntity(String entityId, Links linkOptions, Cursor cursor, Stopwatch stopwatch) {
		final ModelResult result = new ModelResult();

		result.serviceResponse = mEntityCache.loadEntitiesForEntity(entityId, linkOptions, cursor, stopwatch);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			ServiceData serviceData = (ServiceData) result.serviceResponse.data;
			result.data = serviceData.data;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * service queries
	 *--------------------------------------------------------------------------------------------*/

	public synchronized ModelResult loadMessages(String entityId, Links linkOptions, Cursor cursor) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getMessages")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			result.serviceResponse.data = serviceData;
			result.data = loadedEntities;
		}

		return result;
	}

	public synchronized ModelResult loadNotifications(String entityId, Cursor cursor) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getNotifications")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			result.serviceResponse.data = serviceData;
			result.data = loadedEntities;
		}

		return result;
	}

	public synchronized CacheStamp loadCacheStamp(String entityId, CacheStamp cacheStamp) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (cacheStamp != null) {
			if (cacheStamp.activityDate != null) {
				parameters.putLong("activityDate", cacheStamp.activityDate.longValue());
			}

			if (cacheStamp.modifiedDate != null) {
				parameters.putLong("modifiedDate", cacheStamp.modifiedDate.longValue());
			}
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "checkActivity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		/* In case of a failure, we echo back the provided cache stamp to the caller */
		CacheStamp cacheStampService = null;

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.RESULT, Json.ServiceDataWrapper.TRUE);
			if (serviceData.data != null) {
				cacheStampService = (CacheStamp) serviceData.data;
				cacheStampService.source = StampSource.SERVICE.name().toLowerCase(Locale.US);
				if (mCacheStampOverrides.containsKey(entityId)) {
					Logger.v(this, "Using cache stamp override: " + entityId);
					/*
					 * Guarantees that the service cache stamp will not equal
					 * any other cache stamp.
					 */
					cacheStampService.override = true;
				}
			}
		}
		return cacheStampService;
	}

	public synchronized ModelResult loadCategories() {

		final ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_PATCHES + "categories")
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.CATEGORY, Json.ServiceDataWrapper.TRUE);
			mCategories = (List<Category>) serviceData.data;
			result.serviceResponse.data = mCategories;
		}
		return result;
	}

	public synchronized ModelResult loadDocuments(String params) {

		final ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + "documents" + "?" + params)
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.DOCUMENT, Json.ServiceDataWrapper.TRUE);
			result.data = serviceData.data;
		}
		return result;
	}

	public ModelResult suggest(String input, SuggestScope suggestScope, String userId, AirLocation location, long limit) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();

		parameters.putString("provider", ServiceConstants.PLACE_SUGGEST_PROVIDER);
		parameters.putString("input", input.toLowerCase(Locale.US)); // matches any word that as input as prefix
		parameters.putLong("limit", limit);
		parameters.putString("_user", userId); // So service can handle places the current user is watching

		if (suggestScope == SuggestScope.PLACES) {
			parameters.putBoolean("places", true);
		}
		else if (suggestScope == SuggestScope.PATCHES) {
			parameters.putBoolean("patches", true);
		}
		else if (suggestScope == SuggestScope.USERS) {
			parameters.putBoolean("users", true);
		}
		else if (suggestScope == SuggestScope.PATCHES_USERS) {
			parameters.putBoolean("patches", true);
			parameters.putBoolean("users", true);
		}

		if (suggestScope != SuggestScope.USERS) {
			/*
			 * Foursquare won't return anything if lat/lng isn't provided.
			 */
			if (location != null) {
				parameters.putString("location", "object:" + Json.objectToJson(location));
				parameters.putInt("radius", ServiceConstants.PLACE_SUGGEST_RADIUS);
				parameters.putInt("timeout", ServiceConstants.TIMEOUT_PLACE_SUGGEST);
			}
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_SUGGEST)
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			result.serviceResponse.data = serviceData;
			result.data = (List<Entity>) serviceData.data;
		}
		return result;
	}

	private ModelResult getDocumentId(String collection) {

		final ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + collection + "/genId")
				.setRequestType(RequestType.GET)
				.setSuppressUI(true)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.SERVICE_ENTRY, Json.ServiceDataWrapper.TRUE);
			result.serviceResponse.data = ((ServiceEntry) serviceData.data).id;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * user updates
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult signin(String email, String password, String activityName) {
		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("email", email);
		parameters.putString("password", password);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_AUTH + "signin")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setActivityName(activityName)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.session = serviceData.session;
			Patchr.getInstance().setCurrentUser(user, true);

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_signin", null, 0);
			Logger.i(this, "User signed in: " + Patchr.getInstance().getCurrentUser().name);
		}
		return result;
	}

	public ModelResult activateCurrentUser(Boolean refreshUser) {

		ModelResult result = new ModelResult();
		User user = Patchr.getInstance().getCurrentUser();

		if (user.isAnonymous()) {

			Logger.i(this, "Activating anonymous user");

			/* Cancel any current notifications in the status bar */
			NotificationManager.getInstance().cancelNotification(Tag.INSERT);
			NotificationManager.getInstance().cancelNotification(Tag.NOTIFICATION);
			NotificationManager.getInstance().cancelNotification(Tag.SHARE);

			/* Clear user settings */
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), null);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), null);
			Patchr.settingsEditor.commit();
		}
		else {

			Logger.i(this, "Activating authenticated user: " + Patchr.getInstance().getCurrentUser().id);

			/* Load user data */
			if (refreshUser) {
				Links options = mLinks.build(LinkProfile.LINKS_FOR_USER_CURRENT);
				result = getEntity(user.id, true, options);
			}

			/* Update settings */
			final String jsonUser = Json.objectToJson(user);
			final String jsonSession = Json.objectToJson(user.session);

			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user), jsonUser);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_user_session), jsonSession);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_last_email), user.email);
			Patchr.settingsEditor.commit();
		}

		return result;
	}

	public ModelResult signoutComplete() {
		final ModelResult result = new ModelResult();
		/*
		 * We use a short timeout with no retry because failure doesn't
		 * really hurt anything.
		 */
		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_AUTH + "signout")
				.setRequestType(RequestType.GET)
				.setIgnoreResponseData(true)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We treat user as signed out even if the service call failed.
		 */
		Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_signout", null, 0);
		Logger.i(this, "User signed out: "
				+ Patchr.getInstance().getCurrentUser().name
				+ " (" + Patchr.getInstance().getCurrentUser().id + ")");

		/* Set to anonymous user */
		User anonymous = (User) loadEntityFromResources(R.raw.user_entity, Json.ObjectType.ENTITY);
		Patchr.getInstance().setCurrentUser(anonymous, true);

		if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
			Logger.w(this, "User sign out but service call failed: " + Patchr.getInstance().getCurrentUser().id);
		}
		return result;
	}

	public ModelResult updatePassword(String userId, String passwordOld, String passwordNew, String activityName) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("userId", userId);
		parameters.putString("oldPassword", passwordOld);
		parameters.putString("newPassword", passwordNew);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "changepw")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setActivityName(activityName)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.session = serviceData.session;
			Patchr.getInstance().setCurrentUser(user, true);

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "password_change", null, 0);
			Logger.i(this, "User changed password: " + Patchr.getInstance().getCurrentUser().name);
		}
		return result;
	}

	public ModelResult requestPasswordReset(String email) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("email", email);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "reqresetpw")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.USER, "request_password_reset", null, 0);
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.session = serviceData.session;
			result.data = user;
		}

		return result;
	}

	public ModelResult resetPassword(String password, User tempUser) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("password", password);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "resetpw")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		serviceRequest.setSession(tempUser.session);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "password_reset", null, 0);
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.session = serviceData.session;
			Patchr.getInstance().setCurrentUser(user, true);

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_signin", null, 0);
			Logger.i(this, "Password reset and user signed in: " + Patchr.getInstance().getCurrentUser().name);
		}

		return result;
	}

	public ModelResult registerUser(User user, Bitmap bitmap) {
		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("secret", ContainerManager.getContainerHolder().getContainer().getString(Patchr.USER_SECRET));
		parameters.putString("installId", Patchr.getInstance().getinstallId());
		user.id = null; // remove temp id we assigned

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "create")
				.setRequestType(RequestType.INSERT)
				.setRequestBody(Json.objectToJson(user, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
				.setParameters(parameters)
				.setUseSecret(true)
				.setResponseFormat(ResponseFormat.JSON);

		/* insert user. */
		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_register", null, 0);
			String jsonResponse = (String) result.serviceResponse.data;
			ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			user = serviceData.user;
			user.session = serviceData.session;
			/*
			 * Put image to S3 if we have one. Handles setting up the photo
			 * object on user
			 */
			if (bitmap != null && !bitmap.isRecycled()) {
				result.serviceResponse = storeImageAtS3(null, user, bitmap, PhotoType.USER);
			}

			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
				/*
				 * Update user to capture the uri for the image we saved.
				 */
				serviceRequest = new ServiceRequest()
						.setUri(user.getEntryUri())
						.setRequestType(RequestType.UPDATE)
						.setRequestBody(Json.objectToJson(user, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
						.setResponseFormat(ResponseFormat.JSON);

				if (!user.isAnonymous()) {
					serviceRequest.setSession(user.session);
				}

				/* Doing an update so we don't need anything back */
				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					jsonResponse = (String) result.serviceResponse.data;
					serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
					final User insertedUser = (User) serviceData.data;
					insertedUser.session = user.session;
					result.data = insertedUser;
				}
			}
		}

		return result;
	}

	public ModelResult getUserStats(String entityId) {
		ModelResult result = new ModelResult();

		// http://ariseditions.com:8080/actions/user_events/us.000000.00000.000.000001

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_ACTIONS + "user_events/" + entityId)
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.COUNT, Json.ServiceDataWrapper.TRUE);
			final List<Count> stats = (List<Count>) serviceData.data;
			result.data = stats;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Entity updates
	 *--------------------------------------------------------------------------------------------*/

	private ModelResult insertEntity(Entity entity, Boolean waitForContent) {
		return insertEntity(entity, null, null, null, null, waitForContent);
	}

	@SuppressWarnings("ConstantConditions")
	public ModelResult insertEntity(Entity entity, List<Link> links, List<Beacon> beacons, Beacon primaryBeacon, Bitmap bitmap, Boolean waitForContent) {
		/*
		 * Inserts the entity in the entity service collection and Links are created to all the included beacons. The
		 * inserted entity is retrieved from the service and pushed into the local cache. The cached entity is returned
		 * in the data property of the result object.
		 * 
		 * Updates activityDate in the database:
		 * - on any upstream entities linked to in the process
		 * - beacons links can be created
		 * - custom link can be created
		 * - create link is created from user but not followed
		 */

		Logger.i(this, "Inserting entity: " + entity.name);
		ModelResult result = new ModelResult();

		/* Upload image to S3 as needed */
		if (bitmap != null && !bitmap.isRecycled()) {
			PhotoType photoType = PhotoType.GENERAL;
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				photoType = PhotoType.USER;
			}
			result.serviceResponse = storeImageAtS3(entity, null, bitmap, photoType);
		}

		/* Pre-fetch an id so a failed request can be retried */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			result = getDocumentId(entity.getCollection());
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			entity.id = (String) result.serviceResponse.data;

			/* Construct entity, link, and observation */
			final Bundle parameters = new Bundle();

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {

				/* Primary beacon id */
				if (primaryBeacon != null) {
					parameters.putString("primaryBeaconId", primaryBeacon.id);
				}

				if (beacons != null && beacons.size() > 0) {
					/*
					 * Linking to beacons or sending to support nearby notifications
					 */
					final List<String> beaconStrings = new ArrayList<String>();

					for (Beacon beacon : beacons) {
						AirLocation location = LocationManager.getInstance().getAirLocationLocked();
						if (location != null && !location.zombie) {

							beacon.location = new AirLocation();

							beacon.location.lat = location.lat;
							beacon.location.lng = location.lng;

							if (location.altitude != null) {
								beacon.location.altitude = location.altitude;
							}
							if (location.accuracy != null) {
								beacon.location.accuracy = location.accuracy;
							}
							if (location.bearing != null) {
								beacon.location.bearing = location.bearing;
							}
							if (location.speed != null) {
								beacon.location.speed = location.speed;
							}
							if (location.provider != null) {
								beacon.location.provider = location.provider;
							}
						}

						beacon.type = Constants.TYPE_BEACON_FIXED;
						beacon.locked = false;
						beaconStrings.add("object:" + Json.objectToJson(beacon, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
					}
					parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
				}
			}

			/* Link */
			if (links != null && links.size() > 0) {
				final List<String> linkStrings = new ArrayList<String>();
				for (Link link : links) {
					linkStrings.add("object:" + Json.objectToJson(link, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
				}
				parameters.putStringArrayList("links", (ArrayList<String>) linkStrings);
			}

			/* Entity */
			parameters.putString("entity", "object:" + Json.objectToJson(entity, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

            /* Upsizing a suggest patch routes through this routine */
			if (entity.synthetic) {
				parameters.putBoolean("skipNotifications", true);
			}

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.JSON);

			if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
				serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
			}

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			String action = entity.synthetic ? "entity_upsize" : "entity_insert";
			Reporting.sendEvent(Reporting.TrackerCategory.EDIT, action, entity.schema, 0);
			Json.ObjectType serviceDataType = Json.ObjectType.ENTITY;

			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, serviceDataType, Json.ServiceDataWrapper.TRUE);
			final Entity insertedEntity = (Entity) serviceData.data;
			/*
			 * Optimization: Add soft 'create' link so user entity doesn't have to be refetched
			 */
			if (!entity.synthetic) {
				Patchr.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
				mEntityCache.addLink(Patchr.getInstance().getCurrentUser().id
						, insertedEntity.id
						, Constants.TYPE_LINK_CREATE
						, null
						, Patchr.getInstance().getCurrentUser().getAsShortcut(), insertedEntity.getAsShortcut());
			}

			result.data = insertedEntity;

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				mActivityDate = DateTime.nowDate().getTime();
			}
		}

		return result;
	}

	public ModelResult updateEntity(Entity entity, Bitmap bitmap) {
		/*
		 * Updates activityDate in the database:
		 * - on the updated entity
		 * - on any upstream entities the updated entity is linked to
		 * - disabled links are excluded
		 * - like/create/watch links are not followed
		 */
		final ModelResult result = new ModelResult();

		/* Upload new images to S3 as needed. */
		if (bitmap != null) {
			PhotoType photoType = PhotoType.GENERAL;
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				photoType = PhotoType.USER;
			}
			result.serviceResponse = storeImageAtS3(entity, null, bitmap, photoType);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Logger.i(this, "Updating entity: " + entity.name);

			/*
			 * Construct entity, link, and observation
			 * 
			 * Note: A property will be removed from the document if it is set to null. The routine
			 * to convert objects to json takes a parameter to ignore or serialize props set to null.
			 * For now, I have special case code to ensure that photo is seriallized as null even
			 * if ignoreNulls = true.
			 */
			final Bundle parameters = new Bundle();
			parameters.putBoolean("returnEntity", false);
			entity.updateScope = UpdateScope.OBJECT;
			parameters.putString("entity", "object:" + Json.objectToJson(entity, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.JSON);

			if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
				serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
			}

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.EDIT, "entity_update", entity.schema, 0);
			/*
			 * Optimization: We crawl entities in the cache and update embedded
			 * user objects so we don't have to refresh all the affected entities
			 * from the service.
			 */
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				mEntityCache.updateEntityUser(entity);
			}

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				mActivityDate = DateTime.nowDate().getTime();
			}
		}

		return result;
	}

	public ModelResult deleteEntity(String entityId, Boolean cacheOnly) {
		/*
		 * Updates activityDate in the database:
		 * - on any upstream entities the deleted entity was linked to
		 * - disabled links are excluded
		 * - like/create/watch links are not followed
		 */
		final ModelResult result = new ModelResult();
		Entity entity = null;

		if (!cacheOnly) {
			entity = mEntityCache.get(entityId);

			if (entity == null) {
				throw new IllegalArgumentException("Deleting entity requires entity from cache");
			}
			/*
			 * Delete the entity and all links and observations it is associated with. We attempt to continue even
			 * if the call to delete the image failed.
			 */
			Logger.i(this, "Deleting entity: " + entity.id);

			final Bundle parameters = new Bundle();
			parameters.putString("entityId", entity.id);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.JSON);

			if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
				serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
			}

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			if (entity != null) {
				Reporting.sendEvent(Reporting.TrackerCategory.EDIT, "entity_delete", entity.schema, 0);
			}
			entity = mEntityCache.removeEntityTree(entityId);
			/*
			 * Remove 'create' link
			 * 
			 * FIXME: This needs to be generalized to hunt down all links that have
			 * this entity at either end and clean them up including any counts.
			 */
			Patchr.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
			mEntityCache.removeLink(Patchr.getInstance().getCurrentUser().id, entityId, Constants.TYPE_LINK_CREATE, null);

			if (entity != null && entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				mActivityDate = DateTime.nowDate().getTime();
			}
		}
		return result;
	}

	public ModelResult deleteMessage(String entityId, Boolean cacheOnly, String seedParentId) {
		/*
		 * We sequence calls to delete the message and if the message is a seed then
		 * we add a second call to remove any links from replies to the patch.
		 */
		ModelResult result = deleteEntity(entityId, cacheOnly);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS && seedParentId != null) {
			result = removeLinks(entityId, seedParentId, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, "remove_entity_message");
		}
		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, Boolean untuning) {

		final ModelResult result = new ModelResult();
		Logger.i(this, untuning ? "Untracking entity" : "Tracking entity");

		/* Construct entity, link, and observation */
		final Bundle parameters = new Bundle();

		/* Beacons */
		if (primaryBeacon != null) {
			parameters.putString("primaryBeaconId", primaryBeacon.id);
		}

		if (beacons != null && beacons.size() > 0) {

			final List<String> beaconStrings = new ArrayList<String>();
			for (Beacon beacon : beacons) {
				if (primaryBeacon != null && beacon.id.equals(primaryBeacon.id)) {
					AirLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null && !location.zombie) {

						beacon.location = new AirLocation();

						beacon.location.lat = location.lat;
						beacon.location.lng = location.lng;

						if (location.altitude != null) {
							beacon.location.altitude = location.altitude;
						}
						if (location.accuracy != null) {
							beacon.location.accuracy = location.accuracy;
						}
						if (location.bearing != null) {
							beacon.location.bearing = location.bearing;
						}
						if (location.speed != null) {
							beacon.location.speed = location.speed;
						}
						if (location.provider != null) {
							beacon.location.provider = location.provider;
						}
					}
				}

				beacon.type = Constants.TYPE_BEACON_FIXED;
				beacon.locked = false;
				beaconStrings.add("object:" + Json.objectToJson(beacon, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
			}

			parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
		}

		/* Entity */
		parameters.putString("entityId", entity.id);

		/* Method */
		String methodName = untuning ? "untrackEntity" : "trackEntity";

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + methodName)
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		/* Reproduce the service call effect locally */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, untuning ? "place_untune" : "place_tune", null, 0);

			if (beacons != null) {
				for (Beacon beacon : beacons) {
					Boolean primary = (primaryBeacon != null && primaryBeacon.id.equals(beacon.id));
					Link link = entity.getLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, beacon.id, Direction.out);
					if (link != null) {
						if (primary) {
							if (untuning) {
								link.incrementStat(Constants.TYPE_COUNT_LINK_PROXIMITY_MINUS, null);
							}
							else {
								link.incrementStat(Constants.TYPE_COUNT_LINK_PROXIMITY, null);
								if (!link.proximity.primary) {
									link.proximity.primary = true;
								}
							}
							/*
							 * If score goes to zero then the proximity links got deleted by the service.
							 * We want to mirror that in the cache without reloading the entity.
							 */
							if (link.getProximityScore() <= 0) {
								Iterator<Link> iterLinks = entity.linksOut.iterator();
								while (iterLinks.hasNext()) {
									Link temp = iterLinks.next();
									if (temp == link) {
										iterLinks.remove();
										/*
										 * Entity could be a clone so grab the one in the cache.
										 */
										Entity cacheEntity = mEntityCache.get(entity.id);
										if (cacheEntity != null) {
											cacheEntity.activityDate = DateTime.nowDate().getTime();
										}
										break;
									}
								}
							}
						}
					}
					else {
						link = new Link(entity.id, beacon.id, Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON);
						link.proximity = new Proximity();
						link.proximity.signal = beacon.signal;
						if (primary) {
							link.incrementStat(Constants.TYPE_COUNT_LINK_PROXIMITY, null);
							link.proximity.primary = true;
						}
						if (entity.linksOut == null) {
							entity.linksOut = new ArrayList<Link>();
						}
						entity.linksOut.add(link);
						/*
						 * Entity could be a clone so grab the one in the cache.
						 */
						Entity cacheEntity = mEntityCache.get(entity.id);
						if (cacheEntity != null) {
							cacheEntity.activityDate = DateTime.nowDate().getTime();
						}
					}
				}
			}
		}

		return result;
	}

	public ModelResult insertLink(String linkId
			, String fromId
			, String toId
			, String type
			, Boolean enabled
			, Shortcut fromShortcut
			, Shortcut toShortcut
			, String actionEvent
			, Boolean skipCache) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId);             // required
		parameters.putString("toId", toId);                 // required
		parameters.putString("type", type);                 // required
		parameters.putString("actionEvent", actionEvent);

		if (enabled != null) {
			parameters.putBoolean("enabled", enabled);      // optional
		}

		if (linkId != null) {
			parameters.putString("linkId", linkId);         // upserts supported so this is optional
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "insertLink")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 *
		 * Could fail because of ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
		 * prevents any user from liking the same entity more than once. Should be safe to ignore.
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (!skipCache) {
				mEntityCache.addLink(fromId, toId, type, enabled, fromShortcut, toShortcut);
			}
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, actionEvent, Entity.getSchemaForId(toId), 0);
		}

		return result;
	}

	public ModelResult deleteLink(String fromId, String toId, String type, Boolean enabled, String schema, String actionEvent) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId);             // required
		parameters.putString("toId", toId);                 // required
		parameters.putString("type", type);                 // required
		parameters.putString("actionEvent", actionEvent);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteLink")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			String action;
			if (actionEvent != null) {
				action = actionEvent;
			}
			else {
				action = "entity_un" + type;
				if (enabled != null) {
					action += action + "_" + (enabled ? "approved" : "requested");
				}
			}

			Reporting.sendEvent(Reporting.TrackerCategory.LINK, action, schema, 0);
			/*
			 * Fail could be because of ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			mEntityCache.removeLink(fromId, toId, type, enabled);
		}

		return result;
	}

	public ModelResult removeLinks(String fromId, String toId, String type, String schema, String actionEvent) {
		/*
		 * Service method is specialized for messages.
		 */
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId);        // required
		parameters.putString("toId", toId);                // required
		parameters.putString("type", type);                // required
		parameters.putString("actionEvent", actionEvent);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "removeLinks")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, "entity_remove", schema, 0);
			Patchr.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
			mEntityCache.removeLink(fromId, toId, type, null);
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Reports
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult getTrending(String toSchema, String fromSchema, String trendType) {
		ModelResult result = new ModelResult();

		/*
		 * Patches ranked by message count
		 * https://api.aircandi.com/v1/stats/to/patches/from/messages?type=content
		 * 
		 * Patches ranked by watchers
		 * https://api.aircandi.com/v1/stats/to/patches/from/users?type=watch
		 * watch
		 */

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_STATS
						+ "to/" + toSchema + (toSchema.equals(Constants.SCHEMA_ENTITY_PATCH) ? "es" : "s")
						+ "/from/" + fromSchema + (fromSchema.equals(Constants.SCHEMA_ENTITY_PATCH) ? "es" : "s")
						+ "?type=" + trendType)
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> entities = (List<Entity>) serviceData.data;
			Collections.sort(entities, new Entity.SortByRank());
			result.data = entities;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Other service tasks
	 *--------------------------------------------------------------------------------------------*/

	public CacheStamp getCacheStamp() {
		CacheStamp cacheStamp = new CacheStamp(mActivityDate, null);
		cacheStamp.source = StampSource.ENTITY_MANAGER.name().toLowerCase(Locale.US);
		return cacheStamp;
	}

	public ModelResult registerInstall(Install install) {

		ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();
		parameters.putString("install", "object:" + Json.objectToJson(install, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "registerInstall")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return result;
	}

	public ModelResult insertDocument(Document document) {

		/* Pre-fetch an id so a failed request can be retried */
		ModelResult result = getDocumentId(Document.collectionId);
		document.id = (String) result.serviceResponse.data;

		final Bundle parameters = new Bundle();
		parameters.putString("document", "object:" + Json.objectToJson(document, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "insertDocument")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.USER, document.type + "_insert", document.type, 0);
		}

		return result;
	}

	public ModelResult deleteDocument(Document document) {

		ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + "documents/" + document.id)
				.setRequestType(RequestType.DELETE)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.USER, document.type + "_insert", document.type, 0);
		}

		return result;
	}

	public ModelResult insertLog(Log log) {

		ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + "logs")
				.setRequestBody(Json.objectToJson(log, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
				.setRequestType(RequestType.INSERT)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		return result;
	}

	public ModelResult sendInvite(List<String> emails, String invitor, String message) {

		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("emails", (ArrayList<String>) emails);
		parameters.putString("appName", StringManager.getString(R.string.name_app_lowercase));

		if (invitor != null) {
			parameters.putString("name", invitor);
		}

		if (message != null) {
			parameters.putString("message", message);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "invite")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Patchr.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.USER, "invite_send", null, 0);
		}

		return result;
	}

	private ServiceResponse storeImageAtS3(Entity entity, User user, Bitmap bitmap, PhotoType photoType) {
		/*
		 * TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */

		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = UI.ensureBitmapScaleForS3(bitmap);

		/*
		 * Push it to S3. It is always formatted/compressed as a jpeg.
		 */
		final String stringDate = DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME);
		final String imageKey = String.valueOf((user != null) ? user.id : Patchr.getInstance().getCurrentUser().id) + "_" + stringDate + ".jpg";
		ServiceResponse serviceResponse = S3.getInstance().putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3, photoType);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (entity != null) {
				entity.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), Photo.getPhotoSourceByPhotoType(photoType));
			}
			else if (user != null) {
				user.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), Photo.getPhotoSourceByPhotoType(photoType));
			}
		}

		return serviceResponse;
	}

	/*--------------------------------------------------------------------------------------------
	 * Utilities
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Cache queries
	 *--------------------------------------------------------------------------------------------*/

	public List<? extends Entity> getPlaces(Boolean proximity) {

		Integer searchRangeMeters = ServiceConstants.PATCH_NEAR_RADIUS;

		List<Patch> places = (List<Patch>) EntityManager.getEntityCache().getCacheEntities(
				Constants.SCHEMA_ENTITY_PATCH,
				Constants.TYPE_ANY,
				searchRangeMeters,
				proximity);

		Collections.sort(places, new Patch.SortByProximityAndDistance());
		Number limit = Patchr.applicationContext.getResources().getInteger(R.integer.limit_places_radar);

		return (places.size() > limit.intValue()) ? places.subList(0, limit.intValue()) : places;
	}

	/*--------------------------------------------------------------------------------------------
	 * Other fetch routines
	 *--------------------------------------------------------------------------------------------*/

	public List<String> getCategoriesAsStringArray(List<Category> categories) {
		final List<String> categoryStrings = new ArrayList<String>();
		for (Category category : categories) {
			categoryStrings.add(category.name);
		}
		return categoryStrings;
	}

	public Entity loadEntityFromResources(Integer entityResId, Json.ObjectType objectType) {
		InputStream inputStream = null;
		BufferedReader reader = null;
		try {
			inputStream = Patchr.applicationContext.getResources().openRawResource(entityResId);
			reader = new BufferedReader(new InputStreamReader(inputStream));
			final StringBuilder text = new StringBuilder(10000);
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line);
			}
			final String jsonEntity = text.toString();
			final Entity entity = (Entity) Json.jsonToObject(jsonEntity, objectType);
			return entity;
		}
		catch (IOException exception) {
			return null;
		}
		finally {
			try {
				if (inputStream != null)
					inputStream.close();
				if (reader != null)
					reader.close();
			}
			catch (IOException ignore) {}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public List<Category> getCategories() {
		return mCategories;
	}

	public static EntityCache getEntityCache() {
		return mEntityCache;
	}

	public Map<String, String> getCacheStampOverrides() {
		return mCacheStampOverrides;
	}

	public Links getLinks() {
		return mLinks;
	}

	public EntityManager setLinks(Links links) {
		mLinks = links;
		return this;
	}

	public Number getActivityDate() {
		return mActivityDate;
	}

	public EntityManager setActivityDate(Number activityDate) {
		mActivityDate = activityDate;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static enum SuggestScope {
		PATCHES,
		PLACES,
		USERS,
		PATCHES_USERS,
		ALL
	}
}