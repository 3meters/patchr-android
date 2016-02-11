package com.patchr.components;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.EntitiesRequestEvent;
import com.patchr.events.EntityRequestEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.LinkMuteEvent;
import com.patchr.events.NotificationsRequestEvent;
import com.patchr.events.RegisterInstallEvent;
import com.patchr.events.ShareCheckEvent;
import com.patchr.events.TrendRequestEvent;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Beacon;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Cursor;
import com.patchr.objects.Document;
import com.patchr.objects.Entity;
import com.patchr.objects.Install;
import com.patchr.objects.Link;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.LinkSpec;
import com.patchr.objects.LinkSpecFactory;
import com.patchr.objects.LinkSpecItem;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.Proximity;
import com.patchr.objects.ServiceBase.UpdateScope;
import com.patchr.objects.ServiceData;
import com.patchr.objects.Shortcut;
import com.patchr.objects.User;
import com.patchr.service.RequestType;
import com.patchr.service.ResponseFormat;
import com.patchr.service.ServiceRequest;
import com.patchr.service.ServiceResponse;
import com.patchr.ui.AircandiForm;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.parse.ParseInstallation;
import com.squareup.otto.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
 */

public class DataController {

	private Number mActivityDate;                                           // Monitored by nearby
	private              Boolean     mRegistering = false;
	private static final EntityStore ENTITY_STORE = new EntityStore();

	private DataController() {
		try {
			Dispatcher.getInstance().register(this);
		}
		catch (IllegalArgumentException ignore) { /* ignore */ }
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

	@Subscribe
	public void onEntityRequest(final EntityRequestEvent event) {

		/* Called on main thread */

		/* Provide cache entity if available */
		final Entity entity = ENTITY_STORE.getStoreEntity(event.entityId);
		if (entity != null) {
			DataResultEvent data = new DataResultEvent()
					.setActionType(event.actionType)
					.setEntity(entity)
					.setTag(event.tag);
			Dispatcher.getInstance().post(data);
		}

		/* Check service for fresher version of the entity */
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntity");

				LinkSpec links = LinkSpecFactory.build(event.linkProfile);
				final List<String> loadEntityIds = new ArrayList<String>();
				loadEntityIds.add(event.entityId);

				ServiceResponse serviceResponse = ENTITY_STORE.loadEntities(loadEntityIds, links, event.cacheStamp, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					final List<Entity> entities = (List<Entity>) serviceData.data;
					if (entities.size() > 0) {
						Entity entity = entities.get(0);
						DataResultEvent data = new DataResultEvent()
								.setActionType(event.actionType)
								.setMode(event.mode)
								.setEntity(entity)
								.setTag(event.tag);
						Dispatcher.getInstance().post(data);
					}
					else {
						/*
						 * We can't tell the difference between an entity missing because of the where criteria
						 * or because of no match on the entity id. We treat both cases as a no-op.
						 */
						DataNoopEvent noop = new DataNoopEvent().setActionType(event.actionType).setTag(event.tag);
						Dispatcher.getInstance().post(noop);
					}
				}
				else {
					DataErrorEvent error = new DataErrorEvent(serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setMode(event.mode)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onEntitiesRequest(final EntitiesRequestEvent event) {

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
				LinkSpec options = LinkSpecFactory.build(event.linkProfile);

				ServiceResponse serviceResponse = ENTITY_STORE.loadEntitiesForEntity(event.entityId, options, event.cursor, event.cacheStamp, null, event.tag);

				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					/*
					 * The parent entity is always returned unless we pass a cache stamp and it does
					 * not have a fresher cache stamp.
                     */
					if (event.cacheStamp != null && serviceData.entity == null) {
						DataNoopEvent noop = new DataNoopEvent().setActionType(event.actionType).setTag(event.tag);
						Dispatcher.getInstance().post(noop);
					}
					else {
						DataResultEvent data = new DataResultEvent()
								.setEntities((List<Entity>) serviceData.data)
								.setMore(serviceData.more)
								.setActionType(event.actionType)
								.setMode(event.mode)
								.setCursor(event.cursor)
								.setScopingEntity(serviceData.entity)  // Entity straight from db and not processed by getEntities
								.setTag(event.tag);
						Dispatcher.getInstance().post(data);
					}
				}
				else {
					DataErrorEvent error = new DataErrorEvent(serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setMode(event.mode)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onTrendRequest(final TrendRequestEvent event) {

		/* Called on main thread */

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetTrend");
				/*
				 * By default returns sorted by rank in ascending order.
				 */
				ModelResult result = getTrending(event.toSchema
						, event.fromSchema
						, event.linkType
						, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						DataResultEvent data = new DataResultEvent()
								.setEntities((List<Entity>) result.data)
								.setActionType(event.actionType)
								.setMode(event.mode)
								.setTag(event.tag);
						Dispatcher.getInstance().post(data);
					}
				}
				else {
					DataErrorEvent error = new DataErrorEvent(result.serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setMode(event.mode)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onNotificationsRequest(final NotificationsRequestEvent event) {

		/* Called on main thread */

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetNotifications");
				ModelResult result = loadNotifications(event.entityId
						, event.cursor
						, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						DataResultEvent data = new DataResultEvent()
								.setEntities((List<Entity>) result.data)
								.setCursor(event.cursor)
								.setActionType(event.actionType)
								.setMode(event.mode)
								.setMore(((ServiceData) result.serviceResponse.data).more)
								.setTag(event.tag);
						Dispatcher.getInstance().post(data);
					}
				}
				else {
					DataErrorEvent error = new DataErrorEvent(result.serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setMode(event.mode)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onLinkInsert(final LinkInsertEvent event) {

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

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					DataResultEvent data = new DataResultEvent()
							.setActionType(event.actionType)
							.setTag(event.tag);
					Dispatcher.getInstance().post(data);
				}
				else {
					DataErrorEvent error = new DataErrorEvent(result.serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onLinkMute(final LinkMuteEvent event) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncMuteLink");

				ModelResult result = muteLink(event.linkId, event.mute, event.actionEvent);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					DataResultEvent data = new DataResultEvent()
							.setActionType(event.actionType)
							.setTag(event.tag);
					Dispatcher.getInstance().post(data);
				}
				else {
					DataErrorEvent error = new DataErrorEvent(result.serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onLinkDelete(final LinkDeleteEvent event) {

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

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					DataResultEvent data = new DataResultEvent()
							.setActionType(event.actionType)
							.setTag(event.tag);
					Dispatcher.getInstance().post(data);
				}
				else {
					DataErrorEvent error = new DataErrorEvent(result.serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onShareCheck(final ShareCheckEvent event) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncShareCheck");

				ModelResult result = checkShare(event.entityId
						, event.userId
						, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					DataResultEvent data = new DataResultEvent()
							.setActionType(event.actionType)
							.setData(result.data)
							.setTag(event.tag);
					Dispatcher.getInstance().post(data);
				}
				else {
					DataErrorEvent error = new DataErrorEvent(result.serviceResponse.errorResponse);
					error.setActionType(event.actionType)
					     .setTag(event.tag);
					Dispatcher.getInstance().post(error);
				}
				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Subscribe
	public void onRegisterInstall(RegisterInstallEvent event) {

		if (mRegistering) return;
		mRegistering = true;

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncRegisterInstall");

				/* We register installs even if the user is anonymous. */
				ModelResult result = registerInstall();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Patchr.settingsEditor.putBoolean(StringManager.getString(R.string.setting_install_registered), true);
					Patchr.settingsEditor.commit();
				}
				mRegistering = false;

				return null;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	/*--------------------------------------------------------------------------------------------
	 * Cache queries
	 *--------------------------------------------------------------------------------------------*/

	public static Entity getStoreEntity(String entityId) {
		return ENTITY_STORE.getStoreEntity(entityId);
	}

	/*--------------------------------------------------------------------------------------------
	 * Combo service/cache queries
	 *--------------------------------------------------------------------------------------------*/

	public synchronized ModelResult getEntity(String entityId, Boolean refresh, LinkSpec linkOptions, Object tag) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final ModelResult result = new ModelResult();

		Entity entity = ENTITY_STORE.getStoreEntity(entityId);
		if (refresh || entity == null) {
			final List<String> loadEntityIds = new ArrayList<String>();
			loadEntityIds.add(entityId);

			/* This is the only place in the code that calls loadEntities */
			result.serviceResponse = ENTITY_STORE.loadEntities(loadEntityIds, linkOptions, null, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
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

	public ModelResult suggest(String input, SuggestScope suggestScope, String userId, AirLocation location, long limit, Object tag) {

		final ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();

		parameters.putString("provider", Constants.PLACE_SUGGEST_PROVIDER);
		parameters.putString("input", input.toLowerCase(Locale.US)); // matches any word that as input as prefix
		parameters.putLong("limit", limit);
		if (userId != null) {
			parameters.putString("_user", userId); // So service can handle places the current user is watching
		}

		if (suggestScope == SuggestScope.PATCHES) {
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
	 * user updates
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult signin(String email, String password, String activityName, Object tag) {
		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("email", email);
		parameters.putString("password", password);
		parameters.putString("installId", Patchr.getInstance().getinstallId());
		parameters.putBoolean("getEntities", true);

		LinkSpec links = LinkSpecFactory.build(LinkSpecType.LINKS_FOR_USER_CURRENT);
		if (links != null) {
			parameters.putString("links", "object:" + Json.objectToJson(links));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_AUTH + "signin")
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
			UserManager.getInstance().setCurrentUser(user, true);

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_signin", null, 0);
			Logger.i(this, "User signed in: " + UserManager.getInstance().getCurrentUser().name);
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
		if (UserManager.getInstance().authenticated()) {
			serviceRequest.setSession(UserManager.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		/*
		 * We treat user as signed out even if the service call failed.
		 */
		Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_signout", null, 0);
		Logger.i(this, "User signed out: "
				+ UserManager.getInstance().getCurrentUser().name
				+ " (" + UserManager.getInstance().getCurrentUser().id + ")");

		/* Set to anonymous user */
		UserManager.getInstance().setCurrentUser(null, false);

		if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
			Logger.w(this, "User sign out but service call failed: " + UserManager.getInstance().getCurrentUser().id);
		}
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
				.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "changepw")
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
			UserManager.getInstance().setCurrentUser(user, true);

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "password_change", null, 0);
			Logger.i(this, "User changed password: " + UserManager.getInstance().getCurrentUser().name);
		}
		return result;
	}

	public ModelResult requestPasswordReset(String email, Object tag) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("email", email);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "reqresetpw")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
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

	public ModelResult resetPassword(String password, User tempUser, Object tag) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("password", password);
		parameters.putString("installId", Patchr.getInstance().getinstallId());

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "resetpw")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setTag(tag)
				.setResponseFormat(ResponseFormat.JSON);

		serviceRequest.setSession(tempUser.session);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "password_reset", null, 0);
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User user = serviceData.user;
			user.session = serviceData.session;
			UserManager.getInstance().setCurrentUser(user, true);

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_signin", null, 0);
			Logger.i(this, "Password reset and user signed in: " + UserManager.getInstance().getCurrentUser().name);
		}

		return result;
	}

	public ModelResult registerUser(User user, Bitmap bitmap, Object tag) {
		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("secret", ContainerManager.getContainerHolder().getContainer().getString(Patchr.USER_SECRET));
		parameters.putString("installId", Patchr.getInstance().getinstallId());
		parameters.putBoolean("getEntities", true);
		user.id = null; // remove temp id we assigned
		/*
		 * Call to user/create internally calls auth/signin after creating the user. The final
		 * response comes from auth/signin. New users don't have any links yet so we don't
		 * need to provide any link specs.
		 */
		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(Constants.URL_PROXIBASE_SERVICE_USER + "create")
				.setRequestType(RequestType.INSERT)
				.setRequestBody(Json.objectToJson(user, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
				.setParameters(parameters)
				.setTag(tag)
				.setUseSecret(true)
				.setResponseFormat(ResponseFormat.JSON);

		/* Insert user */
		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			Reporting.sendEvent(Reporting.TrackerCategory.USER, "user_register", null, 0);
			String jsonResponse = (String) result.serviceResponse.data;
			ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
			User registeredUser = serviceData.user;
			registeredUser.session = serviceData.session;
			result.data = registeredUser;
			/*
			 * Put image to S3 if we have one. Handles setting up the photo object on user
			 */
			if (bitmap != null && !bitmap.isRecycled()) {

				result.serviceResponse = storeImageAtS3(null, registeredUser, bitmap);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					/*
					 * Update user to capture the uri for the image we saved.
					 *
					 * To get to here, we had to have a successful user registration. If this
					 * call fails, the user photo won't be captured but we still consider the
					 * registration operation a success.
					 */
					serviceRequest = new ServiceRequest()
							.setUri(registeredUser.getEntryUri())
							.setRequestType(RequestType.UPDATE)
							.setRequestBody(Json.objectToJson(registeredUser, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
							.setResponseFormat(ResponseFormat.JSON);

					if (UserManager.getInstance().authenticated()) {
						serviceRequest.setSession(user.session);
					}
					NetworkManager.getInstance().request(serviceRequest);
				}
			}
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Entity updates
	 *--------------------------------------------------------------------------------------------*/

	private ModelResult insertEntity(Entity entity, Boolean waitForContent, Object tag) {
		return insertEntity(entity, null, null, null, null, waitForContent, tag);
	}

	@SuppressWarnings("ConstantConditions")
	public ModelResult insertEntity(Entity entity
			, List<Link> links
			, List<Beacon> beacons
			, Beacon primaryBeacon
			, Bitmap bitmap
			, Boolean waitForContent
			, Object tag) {
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
			result.serviceResponse = storeImageAtS3(entity, null, bitmap);
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
				AirLocation location = LocationManager.getInstance().getAirLocationLocked();
				if (location != null) {
					parameters.putString("location", "object:" + Json.objectToJson(location));
				}

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

						/* Final resort if patch doesn't have it's own location */
						if (location != null) {

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
					.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setTag(tag)
					.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			String action = entity.synthetic ? "entity_upsize" : "entity_insert";
			Reporting.sendEvent(Reporting.TrackerCategory.EDIT, action, entity.schema, 0);
			Json.ObjectType serviceDataType = Json.ObjectType.ENTITY;

			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, serviceDataType, Json.ServiceDataWrapper.TRUE);
			final Entity insertedEntity = (Entity) serviceData.data;
			entity.id = insertedEntity.id;
			/*
			 * Optimization: Add soft 'create' link so user entity doesn't have to be refetched
			 */
			if (!entity.synthetic) {
				if (UserManager.getInstance().authenticated()) {
					UserManager.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
					ENTITY_STORE.fixupAddLink(UserManager.getInstance().getCurrentUser().id
							, insertedEntity.id
							, Constants.TYPE_LINK_CREATE
							, null
							, UserManager.getInstance().getCurrentUser().getAsShortcut(), insertedEntity.getAsShortcut());
				}
			}

			result.data = insertedEntity;

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				mActivityDate = DateTime.nowDate().getTime();
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
			result.serviceResponse = storeImageAtS3(entity, null, bitmap);
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
					.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setTag(tag)
					.setResponseFormat(ResponseFormat.JSON);

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
				ENTITY_STORE.fixupEntityUser(entity);
			}

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				mActivityDate = DateTime.nowDate().getTime();
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
		Entity entity = null;

		if (!cacheOnly) {
			entity = ENTITY_STORE.getStoreEntity(entityId);

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
					.setUri(Constants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setTag(tag)
					.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			if (entity != null) {
				Reporting.sendEvent(Reporting.TrackerCategory.EDIT, "entity_delete", entity.schema, 0);
			}
			entity = ENTITY_STORE.removeEntityTree(entityId);
			/*
			 * Remove 'create' link
			 * 
			 * FIXME: This needs to be generalized to hunt down all links that have
			 * this entity at either end and clean them up including any counts.
			 */
			if (UserManager.getInstance().authenticated()) {
				UserManager.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
				ENTITY_STORE.fixupRemoveLink(UserManager.getInstance().getCurrentUser().id, entityId, Constants.TYPE_LINK_CREATE, null);
			}

			if (entity != null && entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				mActivityDate = DateTime.nowDate().getTime();
			}
		}
		return result;
	}

	public ModelResult deleteMessage(String entityId, Boolean cacheOnly, String seedParentId, Object tag) {
		/*
		 * We sequence calls to delete the message and if the message is a seed then
		 * we add a second call to remove any links from replies to the patch.
		 */
		ModelResult result = deleteEntity(entityId, cacheOnly, tag);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS && seedParentId != null) {
			result = removeLinks(entityId, seedParentId, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, "remove_entity_message", tag);
		}
		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, Boolean untuning, Object tag) {

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
					if (location != null) {

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
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, untuning ? "patch_untune" : "patch_tune", null, 0);
			mActivityDate = DateTime.nowDate().getTime();   // So nearby fragment picks up the change

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
										Entity cacheEntity = ENTITY_STORE.getStoreEntity(entity.id);
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
						Entity cacheEntity = ENTITY_STORE.getStoreEntity(entity.id);
						if (cacheEntity != null) {
							cacheEntity.activityDate = DateTime.nowDate().getTime();
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Inserts link at the service and inserts link locally if the 'from' or 'to'
	 * entities are in the cache.
	 */
	public ModelResult insertLink(String linkId
			, String fromId
			, String toId
			, String type
			, Boolean enabled
			, Shortcut toShortcut
			, String actionEvent
			, Boolean skipCache
			, Object tag
			, Shortcut fromShortcut) {
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
				ENTITY_STORE.fixupAddLink(fromId, toId, type, enabled, fromShortcut, toShortcut);
			}
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, actionEvent, Entity.getSchemaForId(toId), 0);
		}

		return result;
	}

	/**
	 * Deletes link at the service and deletes link locally if the 'from' or 'to'
	 * entities are in the cache.
	 */
	public ModelResult deleteLink(String fromId
			, String toId
			, String type
			, Boolean enabled
			, String schema
			, String actionEvent
			, Object tag) {
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
			 * Fail could be because of Constants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			ENTITY_STORE.fixupRemoveLink(fromId, toId, type, enabled);
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

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, actionEvent, null, 0);
		}

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
			Reporting.sendEvent(Reporting.TrackerCategory.LINK, "entity_remove", schema, 0);
			if (UserManager.getInstance().authenticated()) {
				UserManager.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
			}
			ENTITY_STORE.fixupRemoveLink(fromId, toId, type, null);
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Reports
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult getTrending(String toSchema, String fromSchema, String trendType, Object tag) {
		ModelResult result = new ModelResult();

		final User currentUser = UserManager.getInstance().getCurrentUser();

		LinkSpec links = new LinkSpec().setActive(new ArrayList<LinkSpecItem>());
		links.shortcuts = false;

		links.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
				, UserManager.getInstance().authenticated() ? Maps.asMap("_from", currentUser.id) : null)
				.setDirection(Direction.in));
		links.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1
				, UserManager.getInstance().authenticated() ? Maps.asMap("_creator", currentUser.id) : null)
				.setDirection(Direction.in));

		final Bundle parameters = new Bundle();
		parameters.putBoolean("getEntities", true);
		parameters.putInt("limit", 50);
		parameters.putString("links", "object:" + Json.objectToJson(links));

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
			result.data = entities;
		}
		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Other service tasks
	 *--------------------------------------------------------------------------------------------*/

	public ModelResult registerInstall() {

		Logger.i(this, "Registering install with Aircandi service");
		String parseInstallId = ParseInstallation.getCurrentInstallation().getInstallationId();
		if (parseInstallId == null) {
			throw new IllegalStateException("parseInstallId cannot be null");
		}

		Install install = new Install(UserManager.getInstance().authenticated() ? UserManager.getInstance().getCurrentUser().id : null
				, parseInstallId
				, Patchr.getInstance().getinstallId());

		install.clientVersionName = Patchr.getVersionName(Patchr.applicationContext, AircandiForm.class);
		install.clientVersionCode = Patchr.getVersionCode(Patchr.applicationContext, AircandiForm.class);
		install.clientPackageName = Patchr.applicationContext.getPackageName();
		install.deviceName = AndroidManager.getInstance().getDeviceName();
		install.deviceType = "android";
		install.deviceVersionName = Build.VERSION.RELEASE;

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

	public ModelResult updateProximity(List<String> beaconIds, AirLocation location, String installId, Object tag) {

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

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Reporting.sendEvent(Reporting.TrackerCategory.USER, document.type + "_insert", document.type, 0);
		}

		return result;
	}

	private ServiceResponse storeImageAtS3(Entity entity, User user, Bitmap bitmap) {
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
		final String imageKey = String.valueOf((user != null) ? user.id : UserManager.getInstance().getCurrentUser().id) + "_" + stringDate + ".jpg";
		ServiceResponse serviceResponse = S3.getInstance().putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (entity != null) {
				entity.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), Photo.PhotoSource.aircandi_images);
			}
			else if (user != null) {
				user.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), Photo.PhotoSource.aircandi_images);
			}
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
				final List<Link> links = (List<Link>) serviceData.data;
				result.data = links;
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
		return ENTITY_STORE.removeEntities(schema, type, foundByProximity);
	}

	public void clearStore() {
		ENTITY_STORE.clearStore();
	}

	/*--------------------------------------------------------------------------------------------
	 * Cache queries
	 *--------------------------------------------------------------------------------------------*/

	public List<? extends Entity> getPatches(Boolean proximity) {

		Integer searchRangeMeters = Constants.PATCH_NEAR_RADIUS;

		List<Patch> patches = (List<Patch>) ENTITY_STORE.getStoreEntities(
				Constants.SCHEMA_ENTITY_PATCH,
				Constants.TYPE_ANY,
				searchRangeMeters,
				proximity);

		Collections.sort(patches, new Patch.SortByProximityAndDistance());
		Number limit = Patchr.applicationContext.getResources().getInteger(R.integer.limit_patches_radar);

		return (patches.size() > limit.intValue()) ? patches.subList(0, limit.intValue()) : patches;
	}

	public List<? extends Entity> getBeacons() {
		return (List<Beacon>) ENTITY_STORE.getStoreEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null /* proximity required */);
	}

	/*--------------------------------------------------------------------------------------------
	 * Other fetch routines
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public CacheStamp getGlobalCacheStamp() {
		CacheStamp cacheStamp = new CacheStamp(mActivityDate, null);
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
			final String json = text.toString();
			return json;
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
		return ENTITY_STORE;
	}

	public DataController setActivityDate(Number activityDate) {
		mActivityDate = activityDate;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static enum SuggestScope {
		PATCHES,
		USERS,
		PATCHES_USERS,
		ALL
	}

	public static enum ActionType {
		ACTION_GET_ENTITY,
		ACTION_GET_ENTITIES,
		ACTION_GET_TREND,
		ACTION_GET_NOTIFICATIONS,
		ACTION_LINK_INSERT_LIKE,
		ACTION_LINK_DELETE_LIKE,
		ACTION_LINK_INSERT_WATCH,
		ACTION_LINK_DELETE_WATCH,
		ACTION_LINK_MUTE_WATCH,
		ACTION_SHARE_CHECK,
		ACTION_VIEW_CLICK
	}
}