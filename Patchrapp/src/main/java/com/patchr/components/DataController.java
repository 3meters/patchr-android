package com.patchr.components;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.parse.ParseInstallation;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.events.DataQueryResultEvent;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.events.EntitiesQueryResultEvent;
import com.patchr.events.EntityQueryEvent;
import com.patchr.events.EntityQueryResultEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.LinkMuteEvent;
import com.patchr.events.NotificationsQueryEvent;
import com.patchr.events.RegisterInstallEvent;
import com.patchr.events.ShareCheckEvent;
import com.patchr.events.TrendQueryEvent;
import com.patchr.model.Location;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.Beacon;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Cursor;
import com.patchr.objects.Document;
import com.patchr.objects.Entity;
import com.patchr.objects.Install;
import com.patchr.objects.LinkOld;
import com.patchr.objects.LinkOld.Direction;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecItem;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.LinkSpecs;
import com.patchr.objects.LocationOld;
import com.patchr.objects.Patch;
import com.patchr.objects.ServiceBase.UpdateScope;
import com.patchr.objects.ServiceData;
import com.patchr.objects.Shortcut;
import com.patchr.objects.Suggest;
import com.patchr.objects.User;
import com.patchr.service.RequestType;
import com.patchr.service.ResponseFormat;
import com.patchr.service.ServiceRequest;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.MainScreen;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
 */

@SuppressWarnings("unchecked")
public class DataController {

	private        Number      activityDate;     // Monitored by nearby
	private        boolean     registering;
	private static EntityStore entityStore;

	private DataController() {
		try {
			Dispatcher.getInstance().register(this);
			entityStore = new EntityStore();
		}
		catch (IllegalArgumentException ignore) {
			/* ignore */
			Logger.w(this, ignore.getLocalizedMessage());
		}
	}

	private static class DataControllerHolder {
		public static final DataController instance = new DataController();
	}

	public static DataController getInstance() {
		return DataControllerHolder.instance;
	}

 	/*--------------------------------------------------------------------------------------------
	 * Data request events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onEntityRequest(final EntityQueryEvent event) {

		/* Called on main thread */

		/* Check service for fresher version of the entity */
		new AsyncTask() {

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntity");

				LinkSpecs links = LinkSpecFactory.build(event.linkProfile);
				final List<String> loadEntityIds = new ArrayList<>();
				loadEntityIds.add(event.entityId);

				ServiceResponse serviceResponse = entityStore.loadEntities(loadEntityIds, links, event.cacheStamp, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				EntityQueryResultEvent data = new EntityQueryResultEvent();
				data.actionType = event.actionType;
				data.fetchMode = event.fetchMode;
				data.tag = event.tag;

				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

					ServiceData serviceData = (ServiceData) serviceResponse.data;
					final List<Entity> entities = (List<Entity>) serviceData.data;

					if (entities.size() > 0) {
						data.entity = entities.get(0);
					}
					else {
						data.noop = true; // Could be missing because of criteria or no match on entity id.
					}
				}
				else {
					data.error = serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);

				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onEntitiesRequest(final EntitiesQueryEvent event) {

		/* Called on main thread */

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntities");
				/*
				 * Users as monitor entities: Activity date for user entities is not updated
				 * when an entity they are watching or created is updated. Our goal is to be self
				 * consistent so we add in logic based on the local user.
				 */
				LinkSpecs options = LinkSpecFactory.build(event.linkProfile);

				ServiceResponse serviceResponse = entityStore.loadEntitiesForEntity(event.entityId, options, event.cursor, event.cacheStamp, null, event.tag);

				EntitiesQueryResultEvent resultEvent = new EntitiesQueryResultEvent();
				resultEvent.actionType = event.actionType;
				resultEvent.cursor = event.cursor;
				resultEvent.fetchMode = event.fetchMode;
				resultEvent.tag = event.tag;

				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

					ServiceData serviceData = (ServiceData) serviceResponse.data;
					resultEvent.more = (serviceData.count.intValue() != 0 && serviceData.more);
					resultEvent.scopingEntity = serviceData.entity;  // Entity straight from db and not processed by getEntities
					resultEvent.entities = (List<Entity>) serviceData.data;
					/*
					 * The parent entity is always returned unless we pass a cache stamp and it does
					 * not have a fresher cache stamp.
                     */
					if (event.cacheStamp != null && serviceData.entity == null) {
						resultEvent.noop = true;
					}
				}
				else {
					resultEvent.error = serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(resultEvent);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onTrendRequest(final TrendQueryEvent event) {

		/* Called on main thread */

		new AsyncTask() {

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetTrend");
				/*
				 * By default returns sorted by rank in ascending order.
				 */
				ModelResult result = getTrending(event.toSchema
					, event.fromSchema
					, event.linkType
					, event.cursor
					, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				EntitiesQueryResultEvent data = new EntitiesQueryResultEvent();
				data.actionType = event.actionType;
				data.cursor = event.cursor;
				data.fetchMode = event.fetchMode;
				data.tag = event.tag;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						data.entities = (List<Entity>) result.data;
						data.more = ((ServiceData) result.serviceResponse.data).more;
					}
				}
				else {
					data.error = result.serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onNotificationsRequest(final NotificationsQueryEvent event) {

		/* Called on main thread */

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetNotifications");
				ModelResult result = loadNotifications(event.entityId
					, event.cursor
					, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				EntitiesQueryResultEvent data = new EntitiesQueryResultEvent();
				data.actionType = event.actionType;
				data.cursor = event.cursor;
				data.fetchMode = event.fetchMode;
				data.tag = event.tag;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						data.entities = (List<Entity>) result.data;
						data.more = ((ServiceData) result.serviceResponse.data).more;
					}
				}
				else {
					data.error = result.serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onLinkInsert(final LinkInsertEvent event) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertLink");

				ModelResult result = insertLink(event.linkId
					, event.fromId
					, event.toId
					, event.type
					, event.enabled
					, event.toShortcut, event.actionEvent, event.skipCache, NetworkManager.SERVICE_GROUP_TAG_DEFAULT, event.fromShortcut
				);

				DataQueryResultEvent data = new DataQueryResultEvent();
				data.actionType = event.actionType;
				data.tag = event.tag;
				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					data.error = result.serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onLinkMute(final LinkMuteEvent event) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncMuteLink");

				ModelResult result = muteLink(event.linkId, event.mute, event.actionEvent);

				DataQueryResultEvent data = new DataQueryResultEvent();
				data.actionType = event.actionType;
				data.tag = event.tag;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					data.error = result.serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onLinkDelete(final LinkDeleteEvent event) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteLink");

				ModelResult result = deleteLink(event.fromId
					, event.toId
					, event.type
					, event.enabled
					, event.schema
					, event.actionEvent
					, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				DataQueryResultEvent data = new DataQueryResultEvent();
				data.actionType = event.actionType;
				data.tag = event.tag;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					data.error = result.serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onShareCheck(final ShareCheckEvent event) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncShareCheck");

				ModelResult result = checkShare(event.entityId
					, event.userId
					, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				DataQueryResultEvent data = new DataQueryResultEvent();
				data.actionType = event.actionType;
				data.tag = event.tag;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					data.data = result.data;
				}
				else {
					data.error = result.serviceResponse.errorResponse;
				}
				Dispatcher.getInstance().post(data);
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe public void onRegisterInstall(RegisterInstallEvent event) {

		if (registering) return;

		registering = true;
		Logger.i(this, "Registering install");

		new AsyncTask() {

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRegisterInstall");

				/* We register installs even if the user is anonymous. */
				ModelResult result = registerInstall();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Reporting.track(AnalyticsCategory.ACTION, "Registered Install");
					SharedPreferences.Editor editor = Patchr.settings.edit();
					int clientVersionCode = Patchr.getVersionCode(Patchr.applicationContext, MainScreen.class);
					editor.putBoolean(StringManager.getString(R.string.setting_install_registered), true);
					editor.putInt(StringManager.getString(R.string.setting_install_registered_version_code), clientVersionCode);
					editor.apply();
				}
				registering = false;

				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	/*--------------------------------------------------------------------------------------------
	 * Cache queries
	 *--------------------------------------------------------------------------------------------*/

	public static Entity getStoreEntity(String entityId) {
		return entityStore.getStoreEntity(entityId);
	}

	/*--------------------------------------------------------------------------------------------
	 * Combo service/cache queries
	 *--------------------------------------------------------------------------------------------*/

	public synchronized ModelResult getEntity(String entityId, Boolean refresh, LinkSpecs linkOptions, Object tag) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final ModelResult result = new ModelResult();

		Entity entity = entityStore.getStoreEntity(entityId);
		if (refresh || entity == null) {
			final List<String> loadEntityIds = new ArrayList<>();
			loadEntityIds.add(entityId);

			/* This is the only place in the code that calls loadEntities */
			result.serviceResponse = entityStore.loadEntities(loadEntityIds, linkOptions, null, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
				ServiceData serviceData = (ServiceData) result.serviceResponse.data;
				final List<Entity> entities = (List<Entity>) serviceData.data;
				if (entities.size() > 0) {
					result.data = entities.get(0);
				}
			}
		}
		else {
			result.data = entity;
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * service queries
	 *--------------------------------------------------------------------------------------------*/

	public synchronized ModelResult loadNotifications(String entityId, Cursor cursor, Object tag) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "getNotifications")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

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

	public ModelResult suggest(String input, String suggestScope, String userId, Location location, long limit, Object tag) {

		final ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();

		parameters.putString("provider", Constants.PLACE_SUGGEST_PROVIDER);
		parameters.putString("input", input.toLowerCase(Locale.US)); // matches any word that as input as prefix
		parameters.putLong("limit", limit);
		if (userId != null) {
			parameters.putString("_user", userId); // So service can handle places the current user is watching
		}

		if (suggestScope.equals(Suggest.Patches)) {
			parameters.putBoolean("patches", true);
		}
		else if (suggestScope.equals(Suggest.Users)) {
			parameters.putBoolean("users", true);
		}
		else {
			parameters.putBoolean("patches", true);
			parameters.putBoolean("users", true);
		}

		if (!suggestScope.equals(Suggest.Users)) {
			/*
			 * Foursquare won't return anything if lat/lng isn't provided.
			 */
			if (location != null) {
				parameters.putString("location", "object:" + Json.objectToJson(location));
				parameters.putInt("radius", Constants.PLACE_SUGGEST_RADIUS);
				parameters.putInt("timeout", Constants.TIMEOUT_SERVICE_PLACE_SUGGEST);
			}
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_SUGGEST)
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			result.serviceResponse.data = serviceData;
			result.data = (List<Entity>) serviceData.data;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * User updates
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult tokenLogin(String token, String authType, String activityName, Object tag) {
		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("authorization_code", token);
		parameters.putString("install", Patchr.getInstance().getinstallId());
		parameters.putBoolean("getEntities", true);

		LinkSpecs links = LinkSpecFactory.build(LinkSpecType.LINKS_FOR_USER_CURRENT);
		if (links != null) {
			parameters.putString("links", "object:" + Json.objectToJson(links));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_AUTH + "ak")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setActivityName(activityName)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.authType = authType;
			user.session = serviceData.session;
			//UserManager.shared().setCurrentUser(user, user.session, false);
		}
		return result;
	}

	public ModelResult signoutComplete(Object tag) {
		final ModelResult result = new ModelResult();
		/*
		 * We use a short timeout with no retry because failure doesn't
		 * really hurt anything.
		 */
		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_AUTH + "signout")
			.setRequestType(RequestType.GET)
			.setTag(tag)
			.setIgnoreResponseData(true)
			.setResponseFormat(ResponseFormat.JSON);

		/* Leave this because we are using GET */
		if (UserManager.shared().authenticated()) {
			serviceRequest.setSession(UserManager.currentSession);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		/* Set to anonymous user regardless of success */
		UserManager.shared().setCurrentUser(null, null, false);

		return result;
	}

	public ModelResult updatePassword(String userId, String passwordOld, String passwordNew, String activityName, Object tag) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("userId", userId);
		parameters.putString("oldPassword", passwordOld);
		parameters.putString("newPassword", passwordNew);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "pw/change")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setActivityName(activityName)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.session = serviceData.session;
			//UserManager.shared().setCurrentUser(user, user.session, true);
		}
		return result;
	}

	public ModelResult validateEmail(String email, Object tag) {

		ModelResult result = new ModelResult();

		String uri = String.format(Constants.URL_PROXIBASE_SERVICE_FIND + "/users?q[email]=%1$s", Utils.encode(email));

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(uri)
			.setRequestType(RequestType.GET)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			result.serviceResponse.data = serviceData;
		}

		return result;
	}

	public ModelResult requestPasswordReset(String email, Object tag) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("email", email);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "pw/reqreset")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		return result;
	}

	public ModelResult resetPassword(String password, String token, Object tag) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("password", password);
		parameters.putString("token", token);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "pw/reset")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);

			User user = serviceData.user;
			user.session = serviceData.session;
			//UserManager.shared().setCurrentUser(user, user.session, true);
		}

		return result;
	}

	public ModelResult registerUser(User newUser, Bitmap bitmap, Object tag) {

		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("secret", ContainerManager.getContainerHolder().getContainer().getString(Patchr.USER_SECRET));
		parameters.putString("installId", Patchr.getInstance().getinstallId());
		parameters.putBoolean("getEntities", true);
		newUser.id = null; // remove temp id we assigned
		/*
		 * Call to user/create internally calls auth/signin after creating the user. The final
		 * response comes from auth/signin. New users don't have any links yet so we don't
		 * need to provide any link specs.
		 */
		ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "create")
			.setRequestType(RequestType.INSERT)
			.setRequestBody(Json.objectToJson(newUser, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
			.setParameters(parameters)
			.setTag(tag)
			.setUseSecret(true)
			.setResponseFormat(ResponseFormat.JSON);

		/* Insert user */
		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			String jsonResponse = (String) result.serviceResponse.data;
			ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);

//			RealmEntity user = (RealmEntity) serviceData.user;
//			user.session = serviceData.session;
//			result.data = user;
			/*
			 * Put image to S3 if we have one. Handles setting up the photo object on user
			 */
			if (bitmap != null && !bitmap.isRecycled()) {

//				result.serviceResponse = storeImageAtS3(user, user.id, bitmap);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					/*
					 * Update user to capture the uri for the image we saved.
					 *
					 * To get to here, we had to have a successful user registration. If this
					 * call fails, the user photo won't be captured but we still consider the
					 * registration operation a success.
					 */
					//					serviceRequest = new ServiceRequest()
					//							.setUri(user.getEntryUri())
					//							.setRequestType(RequestType.UPDATE)
					//							.setRequestBody(Json.objectToJson(user, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
					//							.setResponseFormat(ResponseFormat.JSON);

					if (UserManager.shared().authenticated()) {
						serviceRequest.setSession(newUser.session);
					}
					NetworkManager.getInstance().request(serviceRequest);
				}
			}
		}

		return result;
	}

	public ModelResult deleteUser(String userId, Object tag) {
		ModelResult result = new ModelResult();

		String path = Constants.URL_PROXIBASE_SERVICE_USER + userId;
		String query = String.format("?erase=true&session=%1$s&user=%2$s", UserManager.currentSession, UserManager.userId);
		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(path + query)
			.setRequestType(RequestType.DELETE)
			.setTag(tag)
			.setIgnoreResponseData(true)
			.setResponseFormat(ResponseFormat.JSON);

		/* Delete user */
		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Entity updates
	 *--------------------------------------------------------------------------------------------*/

	private ModelResult insertEntity(Entity entity, Boolean waitForContent, Object tag) {
		return insertEntity(entity, null, null, null, waitForContent, tag);
	}

	public ModelResult insertEntity(Entity entity, List<LinkOld> links, List<Beacon> beacons, Bitmap bitmap, Boolean waitForContent, Object tag) {
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
		ModelResult result = new ModelResult();

		/* Upload image to S3 as needed */

		if (bitmap != null && !bitmap.isRecycled()) {
			//result.serviceResponse = storeImageAtS3(entity, null, bitmap);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			/* Clear any temp entity id */
			entity.id = null;

			/* Construct entity, link, and observation */
			final Bundle parameters = new Bundle();

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {

				/*
				 * Location and beaconIds are used to determine  if installs should receive
				 * notifications because they are nearby.
				 */
				Location location = LocationManager.getInstance().getLocationLocked();
				if (location != null) {
					parameters.putString("location", "object:" + Json.objectToJson(location));
				}

				if (beacons != null && beacons.size() > 0) {
					/*
					 * Linking to beacons or sending to support nearby notifications
					 */
					final List<String> beaconStrings = new ArrayList<>();

					for (Beacon beacon : beacons) {

						/* Final resort if patch doesn't have it's own location */
						if (location != null) {

							beacon.location = new LocationOld();

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
						beaconStrings.add("object:" + Json.objectToJson(beacon, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
					}
					parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
				}
			}

			/* Link */
			if (links != null && links.size() > 0) {
				final List<String> linkStrings = new ArrayList<>();
				for (LinkOld link : links) {
					linkStrings.add("object:" + Json.objectToJson(link, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
				}
				parameters.putStringArrayList("links", (ArrayList<String>) linkStrings);
			}

			/* Entity */
			parameters.putString("entity", "object:" + Json.objectToJson(entity, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

			final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			Json.ObjectType serviceDataType = Json.ObjectType.ENTITY;

			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, serviceDataType, Json.ServiceDataWrapper.TRUE);
			final Entity insertedEntity = (Entity) serviceData.data;
			entity.id = insertedEntity.id;
			/*
			 * Optimization: Add soft 'create' link so user entity doesn't have to be refetched
			 */
			if (UserManager.shared().authenticated()) {
//				UserManager.currentUser.activityDate = DateTime.nowDate().getTime();
//				entityStore.fixupAddLink(UserManager.currentUser.id
//					, insertedEntity.id
//					, Constants.TYPE_LINK_CREATE
//					, null
//					, UserManager.currentUser, insertedEntity.getAsShortcut());
			}

			result.data = insertedEntity;

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				activityDate = DateTime.nowDate().getTime();
			}
		}

		return result;
	}

	public ModelResult updateEntity(Entity entity, Bitmap bitmap, Object tag) {
		/*
		 * Updates activityDate in the database:
		 * - on the updated entity
		 * - on any upstream entities the updated entity is linked to
		 * - disabled links are excluded
		 */
		final ModelResult result = new ModelResult();

		/* Upload new images to S3 as needed. */
		if (bitmap != null) {
			//result.serviceResponse = storeImageAtS3(entity, null, bitmap);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
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
				.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			/*
			 * Optimization: We crawl entities in the cache and update embedded
			 * user objects so we don't have to refresh all the affected entities
			 * from the service.
			 */
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				entityStore.fixupEntityUser(entity);
			}

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				activityDate = DateTime.nowDate().getTime();
			}
		}

		return result;
	}

	public ModelResult deleteEntity(String entityId, Boolean cacheOnly, Object tag) {
		/*
		 * Updates activityDate in the database:
		 * - on any upstream entities the deleted entity was linked to
		 * - disabled links are excluded
		 * - like/create/watch links are not followed
		 */
		final ModelResult result = new ModelResult();
		Entity entity;

		if (!cacheOnly) {
			entity = entityStore.getStoreEntity(entityId);

			if (entity == null) {
				throw new IllegalArgumentException("Deleting entity requires entity from cache");
			}
			/*
			 * Delete the entity and all links and observations it is associated with. We attempt to continue even
			 * if the call to delete the image failed.
			 */
			final Bundle parameters = new Bundle();
			parameters.putString("entityId", entity.id);

			final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			entity = entityStore.removeEntityTree(entityId);
			/* Remove 'create' link */
			if (UserManager.shared().authenticated()) {
				UserManager.currentUser.activityDate = DateTime.nowDate().getTime();
				entityStore.fixupRemoveLink(UserManager.currentUser.id, entityId, Constants.TYPE_LINK_CREATE, null);
			}

			if (entity != null && entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				activityDate = DateTime.nowDate().getTime();
			}
		}
		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Boolean untuning, Object tag) {

		final ModelResult result = new ModelResult();
		Logger.i(this, untuning ? "Untracking entity" : "Tracking entity");

		/* Construct entity, link, and observation */
		final Bundle parameters = new Bundle();

		if (beacons != null && beacons.size() > 0) {

			final List<String> beaconStrings = new ArrayList<>();
			for (Beacon beacon : beacons) {
				beacon.type = Constants.TYPE_BEACON_FIXED;
				beaconStrings.add("object:" + Json.objectToJson(beacon, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
			}
			parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
		}

		/* Entity */
		parameters.putString("entityId", entity.id);

		/* Method */
		String methodName = untuning ? "untrackEntity" : "trackEntity";

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + methodName)
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		/* Reproduce the service call effect locally */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			activityDate = DateTime.nowDate().getTime();   // So nearby fragment picks up the change

			if (beacons != null) {
				for (Beacon beacon : beacons) {
					LinkOld link = entity.getLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, beacon.id, Direction.out);
					if (link == null) {
						link = new LinkOld(entity.id, beacon.id, Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON);
						if (entity.linksOut == null) {
							entity.linksOut = new ArrayList<>();
						}
						entity.linksOut.add(link);
						/*
						 * Entity could be a clone so grab the one in the cache.
						 */
						Entity cacheEntity = entityStore.getStoreEntity(entity.id);
						if (cacheEntity != null) {
							cacheEntity.activityDate = DateTime.nowDate().getTime();
						}
					}
				}
			}
		}

		return result;
	}

	public ModelResult insertLink(String linkId, String fromId, String toId, String type, Boolean enabled, Shortcut toShortcut, String actionEvent, Boolean skipCache, Object tag, Shortcut fromShortcut) {
		/*
		 * Inserts link at the service and inserts link locally if the 'from' or 'to'
		 * entities are in the cache.
		 */
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
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "insertLink")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 *
		 * Could fail because of Constants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
		 * prevents any user from liking the same entity more than once. Should be safe to ignore.
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (!skipCache) {
				entityStore.fixupAddLink(fromId, toId, type, enabled, fromShortcut, toShortcut);
			}
		}

		return result;
	}

	public ModelResult deleteLink(String fromId, String toId, String type, Boolean enabled, String schema, String actionEvent, Object tag) {
		/**
		 * Deletes link at the service and deletes link locally if the 'from' or 'to'
		 * entities are in the cache.
		 */
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId);             // required
		parameters.putString("toId", toId);                 // required
		parameters.putString("type", type);                 // required
		parameters.putString("actionEvent", actionEvent);

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "deleteLink")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			/*
			 * Fail could be because of Constants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			entityStore.fixupRemoveLink(fromId, toId, type, enabled);
		}

		return result;
	}

	public ModelResult muteLink(String linkId, Boolean mute, String actionEvent) {
		ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_REST + "links/" + linkId)
			.setRequestType(RequestType.UPDATE)
			.setParameters(new Bundle())
			.setRequestBody("{ \"mute\": " + String.valueOf(mute) + "}")
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		return result;
	}

	public ModelResult removeLinks(String fromId, String toId, String type, String schema, String actionEvent, Object tag) {
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
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "removeLinks")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (UserManager.shared().authenticated()) {
				UserManager.currentUser.activityDate = DateTime.nowDate().getTime();
			}
			entityStore.fixupRemoveLink(fromId, toId, type, null);
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Reports
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult getTrending(String toSchema, String fromSchema, String trendType, Cursor cursor, Object tag) {
		ModelResult result = new ModelResult();

		final RealmEntity currentUser = UserManager.currentUser;

		LinkSpecs links = new LinkSpecs().setActive(new ArrayList<LinkSpecItem>());
		links.shortcuts = false;

		links.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_MEMBER, Constants.SCHEMA_ENTITY_USER, true, true, 1
			, UserManager.shared().authenticated() ? Maps.asMap("_from", currentUser.id) : null)
			.setDirection(Direction.in));
		links.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1
			, UserManager.shared().authenticated() ? Maps.asMap("_creator", currentUser.id) : null)
			.setDirection(Direction.in));

		final Bundle parameters = new Bundle();
		parameters.putBoolean("getEntities", true);
		parameters.putInt("limit", 50);
		parameters.putString("links", "object:" + Json.objectToJson(links));

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_PATCHES + "interesting")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> entities = (List<Entity>) serviceData.data;
			Collections.sort(entities, new Entity.SortByRank());
			result.serviceResponse.data = serviceData;
			result.data = entities;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Other service tasks
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult registerInstall() {

		String parseInstallId = ParseInstallation.getCurrentInstallation().getInstallationId();
		if (parseInstallId == null) {
			throw new IllegalStateException("parseInstallId cannot be null");
		}

		Install install = new Install(UserManager.shared().authenticated() ? UserManager.currentUser.id : null
			, parseInstallId
			, Patchr.getInstance().getinstallId());

		install.clientVersionName = Patchr.getVersionName(Patchr.applicationContext, MainScreen.class);
		install.clientVersionCode = Patchr.getVersionCode(Patchr.applicationContext, MainScreen.class);
		install.clientPackageName = Patchr.applicationContext.getPackageName();
		install.deviceName = AndroidManager.getInstance().getDeviceName();
		install.deviceType = "android";
		install.deviceVersionName = Build.VERSION.RELEASE;      // Android version number. E.g., "1.0" or "3.4b5"

		ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();
		parameters.putString("install", "object:" + Json.objectToJson(install, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "registerInstall")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(NetworkManager.SERVICE_GROUP_TAG_DEFAULT)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Logger.i(this, "Install successfully registered with Aircandi service");
		}

		return result;
	}

	public ModelResult updateProximity(List<String> beaconIds, Location location, String installId, Object tag) {

		if (installId == null) {
			throw new IllegalArgumentException("updateProximity requires installId");
		}

		ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();

		parameters.putString("installId", installId);

		if (beaconIds != null) {
			parameters.putStringArrayList("beaconIds", (ArrayList<String>) beaconIds);
		}

		if (location != null) {
			parameters.putString("location", "object:" + Json.objectToJson(location));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "updateProximity")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return result;
	}

	public ModelResult insertDocument(Document document, Object tag) {

		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("document", "object:" + Json.objectToJson(document, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "insertDocument")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		return result;
	}

	private ServiceResponse storeImageAtS3(RealmEntity entity, String userId, Bitmap bitmap) {

		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = UI.ensureBitmapScaleForS3(bitmap);

		/*
		 * Push it to S3. It is always formatted/compressed as a jpeg.
		 */
		final String stringDate = DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME);
		final String imageKey = String.format("%1$s_%2$s.jpg", userId, stringDate); // User id at root to avoid collisions
		ServiceResponse serviceResponse = S3.getInstance().putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			entity.setPhoto(new Photo(imageKey, bitmap.getWidth(), bitmap.getHeight(), Photo.PhotoSource.aircandi_images));
		}

		return serviceResponse;
	}

	public ModelResult checkShare(String entityId, String userId, Object tag) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);
		parameters.putString("userId", userId);

		final ServiceRequest serviceRequest = new ServiceRequest()
			.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "checkShare")
			.setRequestType(RequestType.METHOD)
			.setParameters(parameters)
			.setTag(tag)
			.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String json = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(json, Json.ObjectType.LINK, Json.ServiceDataWrapper.TRUE);
			if (serviceData.data != null && serviceData.count.intValue() > 0) {
				result.data = (List<LinkOld>) serviceData.data;
			}
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Utilities
	 *--------------------------------------------------------------------------------------------*/

	public void warmup() {
		/* Just here to force DataController to register for dispatches */
	}

	public Integer clearEntities(String schema, String type, Boolean foundByProximity) {
		return entityStore.removeEntities(schema, type, foundByProximity);
	}

	public void clearStore() {
		entityStore.clearStore();
	}

	/*--------------------------------------------------------------------------------------------
	 * Cache queries
	 *--------------------------------------------------------------------------------------------*/

	public List<? extends Entity> getPatches(Boolean proximity) {

		Integer searchRangeMeters = Constants.PATCH_NEAR_RADIUS;

		List<Patch> patches = (List<Patch>) entityStore.getStoreEntities(
			Constants.SCHEMA_ENTITY_PATCH,
			Constants.TYPE_ANY,
			searchRangeMeters,
			proximity);

		Collections.sort(patches, new Patch.SortByProximityAndDistance());
		Number limit = Patchr.applicationContext.getResources().getInteger(R.integer.limit_patches_radar);

		return (patches.size() > limit.intValue()) ? patches.subList(0, limit.intValue()) : patches;
	}

	public List<? extends Entity> getBeacons() {
		return (List<Beacon>) entityStore.getStoreEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null /* proximity required */);
	}

	/*--------------------------------------------------------------------------------------------
	 * Other fetch routines
	 *--------------------------------------------------------------------------------------------*/

	@NonNull public CacheStamp getGlobalCacheStamp() {
		CacheStamp cacheStamp = new CacheStamp(activityDate, null);
		cacheStamp.source = CacheStamp.StampSource.ENTITY_MANAGER.name().toLowerCase(Locale.US);
		return cacheStamp;
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
			return (Entity) Json.jsonToObject(jsonEntity, objectType);
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

	public String loadJsonFromResources(Integer entityResId) {
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
			return text.toString();
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

	public static EntityStore getEntityCache() {
		return entityStore;
	}

	public DataController setActivityDate(Number activityDate) {
		this.activityDate = activityDate;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}