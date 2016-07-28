package com.patchr.service;

import android.os.Build;
import android.util.Base64;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.parse.ParseInstallation;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.AndroidManager;
import com.patchr.components.ContainerManager;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.NetworkManager;
import com.patchr.components.UserManager;
import com.patchr.exceptions.ClientVersionException;
import com.patchr.exceptions.NoNetworkException;
import com.patchr.model.Location;
import com.patchr.model.Query;
import com.patchr.model.RealmEntity;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.Session;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.FetchStrategy;
import com.patchr.objects.enums.QueryName;
import com.patchr.objects.enums.Suggest;
import com.patchr.ui.MainScreen;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Maps;
import com.patchr.utilities.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RestClient {

	public static  ProxibaseApi proxiApi;
	private static BingApi      bingApi;
	private static RestClient instance = new RestClient();
	public Long activityDateInsertDeletePatch;

	public static RestClient getInstance() {
		return instance;
	}

	private RestClient() {
		try {
			activityDateInsertDeletePatch = DateTime.nowDate().getTime();

			/* Configure http client */
			OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder().addNetworkInterceptor(new StethoInterceptor());
			OkHttpClient client = httpClient.build();

			Gson gson = new GsonBuilder()
				.serializeNulls()
				.setPrettyPrinting()
				.registerTypeAdapter(RealmEntity.class, new EntitySerializer())
				.excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)    // These are also the defaults
				.create();

			Retrofit retrofitProxi = new Retrofit.Builder()
				.baseUrl("https://api.aircandi.com/v1/")
				.client(client)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
				.build();

			proxiApi = retrofitProxi.create(ProxibaseApi.class);

			Retrofit retrofitBing = new Retrofit.Builder()
				.baseUrl("https://api.datamarket.azure.com/Bing/Search/v1/")
				.client(client)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
				.build();

			bingApi = retrofitBing.create(BingApi.class);
		}
		catch (IllegalArgumentException ignore) {
			/* ignore */
			Logger.w(this, ignore.getLocalizedMessage());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Fetch public
	 *--------------------------------------------------------------------------------------------*/

	public Observable<ProxibaseResponse> fetchEntity(String id, FetchStrategy strategy) {
		return fetchEntity(id, strategy, UserManager.userId, UserManager.sessionKey);
	}

	public Observable<ProxibaseResponse> fetchEntity(String id, FetchStrategy strategy, final String usingUserId, final String usingSession) {

		String schema = RealmEntity.getSchemaForId(id);
		String collection = RealmEntity.getCollectionForSchema(schema);

		SimpleMap parameters = new SimpleMap();
		RealmEntity.extras(schema, parameters);
		if (usingUserId != null && usingSession != null) {
			parameters.put("user", usingUserId);
			parameters.put("session", usingSession);
		}

		addQueryParameter(parameters, strategy, id);

		return post(String.format("find/%1$s/%2$s", collection, id), parameters, null, null, false)
			.map(response -> {
					/* Cherrypick session info so it's available downstream */
				if (usingSession != null) {
					response.session = new Session();
					response.session.key = usingSession;
				}
				if (!response.noop && response.data.size() > 0) {
					updateRealm(response, null, null);
				}
				return response;
			});
	}

	public Observable<ProxibaseResponse> fetchListItems(final FetchStrategy strategy, final QuerySpec querySpec, final String entityId, final Integer skip) {

		if (querySpec.name.equals(QueryName.LikesForMessage)) {
			return fetchLikesForMessage(strategy, querySpec, entityId, skip);
		}
		else if (querySpec.name.equals(QueryName.MembersForPatch)) {
			return fetchMembersForPatch(strategy, querySpec, entityId, skip);
		}
		else if (querySpec.name.equals(QueryName.MessagesByUser)) {
			return fetchMessagesByUser(strategy, querySpec, entityId, skip);
		}
		else if (querySpec.name.equals(QueryName.MessagesForPatch)) {
			return fetchMessagesForPatch(strategy, querySpec, entityId, skip);
		}
		else if (querySpec.name.equals(QueryName.NotificationsForUser)) {
			return fetchNotificationsForUser(querySpec, skip);
		}
		else if (querySpec.name.equals(QueryName.PatchesNearby)) {
			return fetchPatchesNearby(querySpec, skip);
		}
		else if (querySpec.name.equals(QueryName.PatchesOwnedByUser)) {
			return fetchPatchesOwnedByUser(strategy, querySpec, entityId, skip);
		}
		else if (querySpec.name.equals(QueryName.PatchesToExplore)) {
			return fetchPatchesToExplore(querySpec, skip);
		}
		else if (querySpec.name.equals(QueryName.PatchesUserMemberOf)) {
			return fetchPatchesUserMemberOf(strategy, querySpec, entityId, skip);
		}

		throw new IllegalArgumentException("No match on query name.");
	}

	public Observable<ProxibaseResponse> suggest(final String input, String suggestScope, String userId, Location location, long limit) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("provider", "google");
		parameters.put("input", input.toLowerCase(Locale.US));
		parameters.put("limit", limit);

		if (userId != null) {
			parameters.put("_user", userId); // So service can handle places the current user is watching
		}

		if (suggestScope.equals(Suggest.Patches)) {
			parameters.put("patches", true);
		}
		else if (suggestScope.equals(Suggest.Users)) {
			parameters.put("users", true);
		}
		else {
			parameters.put("patches", true);
			parameters.put("users", true);
		}

		if (!suggestScope.equals(Suggest.Users)) {
			/*
			 * Foursquare won't return anything if lat/lng isn't provided.
			 */
			if (location != null) {
				addLocationParameter(parameters, location);
				parameters.put("radius", Constants.PLACE_SUGGEST_RADIUS);
				parameters.put("timeout", Constants.TIMEOUT_SERVICE_PLACE_SUGGEST);
			}
		}

		addSessionParameters(parameters);

		return post("suggest", parameters, null, null, false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Fetch collections
	 *--------------------------------------------------------------------------------------------*/

	private Observable<ProxibaseResponse> fetchLikesForMessage(final FetchStrategy strategy, final QuerySpec querySpec, final String messageId, final Integer skip) {

		final String queryId = querySpec.getId(messageId);

		SimpleMap linked = new SimpleMap();
		linked.put("from", "users");
		linked.put("type", "like");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_USER, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, messageId);

		return post(String.format("find/messages/%1$s", messageId), parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchMembersForPatch(final FetchStrategy strategy, final QuerySpec querySpec, final String patchId, final Integer skip) {

		final String queryId = querySpec.getId(patchId);

		SimpleMap linked = new SimpleMap();
		linked.put("from", "users");
		linked.put("type", "watch");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);
		linked.put("linkFields", "type,enabled");

		Realm realm = Realm.getDefaultInstance();
		RealmEntity patch = realm.where(RealmEntity.class).equalTo("id", patchId).findFirst();
		Boolean isOwner = (patch.ownerId.equals(UserManager.userId));
		if (!isOwner) {
			linked.put("filter", Maps.asMap("enabled", true));
		}
		realm.close();

		RealmEntity.extras(Constants.SCHEMA_ENTITY_USER, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, patchId);

		return post(String.format("find/patches/%1$s", patchId), parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchMessagesByUser(final FetchStrategy strategy, final QuerySpec querySpec, final String userId, final Integer skip) {

		final String queryId = querySpec.getId(userId);

		SimpleMap linked = new SimpleMap();
		linked.put("to", "messages");
		linked.put("type", "create");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_MESSAGE, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, userId);

		return post(String.format("find/users/%1$s", userId), parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchMessagesForPatch(final FetchStrategy strategy, final QuerySpec querySpec, final String patchId, final Integer skip) {

		final String queryId = querySpec.getId(patchId);

		SimpleMap linked = new SimpleMap();
		linked.put("from", "messages");
		linked.put("type", "content");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_MESSAGE, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, patchId);

		return post(String.format("find/patches/%1$s", patchId), parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchNotificationsForUser(final QuerySpec querySpec, final Integer skip) {

		final String queryId = querySpec.getId(null);

		SimpleMap parameters = new SimpleMap();
		parameters.put("limit", Constants.PAGE_SIZE);
		parameters.put("skip", skip);
		parameters.put("more", true);
		addSessionParameters(parameters);   // Notifications keys on the authenticated user id

		return post("user/getNotifications", parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchPatchesNearby(final QuerySpec querySpec, final Integer skip) {

		final String queryId = querySpec.getId(null);

		Location location = LocationManager.getInstance().getLocationLocked();

		SimpleMap parameters = new SimpleMap();
		parameters.put("limit", 50);
		parameters.put("skip", skip);
		parameters.put("more", false);
		parameters.put("radius", 10000);
		parameters.put("rest", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_PATCH, parameters);

		addLocationParameter(parameters, location);
		addSessionParameters(parameters);

		return post("patches/near", parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchPatchesOwnedByUser(final FetchStrategy strategy, final QuerySpec querySpec, final String userId, final Integer skip) {

		final String queryId = querySpec.getId(userId);

		SimpleMap linked = new SimpleMap();
		linked.put("to", "patches");
		linked.put("type", "create");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_PATCH, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");

		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, userId);

		return post(String.format("find/users/%1$s", userId), parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchPatchesToExplore(final QuerySpec querySpec, final Integer skip) {

		final String queryId = querySpec.getId(null);

		SimpleMap parameters = new SimpleMap();
		parameters.put("limit", 50);
		parameters.put("skip", skip);
		parameters.put("more", true);
		RealmEntity.extras(Constants.SCHEMA_ENTITY_PATCH, parameters);

		addSessionParameters(parameters);

		return post("patches/interesting", parameters, queryId, skip, true);
	}

	private Observable<ProxibaseResponse> fetchPatchesUserMemberOf(final FetchStrategy strategy, final QuerySpec querySpec, final String userId, final Integer skip) {

		final String queryId = querySpec.getId(userId);

		SimpleMap linked = new SimpleMap();
		linked.put("to", "patches");
		linked.put("type", "watch");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);
		linked.put("linkFields", "type,enabled");

		RealmEntity.extras(Constants.SCHEMA_ENTITY_PATCH, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");

		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, userId);

		return post(String.format("find/users/%1$s", userId), parameters, queryId, skip, true);
	}

	/*--------------------------------------------------------------------------------------------
	 * Modify
	 *--------------------------------------------------------------------------------------------*/

	public Observable<ProxibaseResponse> postEntity(final String path, SimpleMap data) {

		SimpleMap parameters = data;
		if (!data.containsKey("data")) {
			parameters = new SimpleMap();
			parameters.put("data", data);
		}
		addSessionParameters(parameters);

		return post(path, parameters, null, null, false);
	}

	public Call<Map<String, Object>> postEntityCall(final String path, RealmEntity entity, SimpleMap parameters) {

		SimpleMap body = parameters;
		if (body == null) {
			body = new SimpleMap();
		}
		body.put("data", entity);

		addSessionParameters(body);

		return proxiApi.postCall(path, body);
	}

	public Observable<ProxibaseResponse> deleteEntity(final String path, SimpleMap parameters, final String objectId) {

		if (parameters == null) {
			parameters = new SimpleMap();
		}
		addSessionParameters(parameters);

		return delete(path, parameters, objectId, true);
	}

	public Observable<ProxibaseResponse> insertLink(String fromId, String toId, String type) {

		SimpleMap link = new SimpleMap();
		link.put("_from", fromId);
		link.put("_to", toId);
		link.put("type", type);

		SimpleMap parameters = new SimpleMap();
		parameters.put("data", link);

		addSessionParameters(parameters);

		return post("data/links", parameters, null, null, false);
	}

	public Observable<ProxibaseResponse> enableLinkById(String entityId, String linkId, Boolean enabled) {

		SimpleMap link = new SimpleMap();
		link.put("enabled", enabled);

		SimpleMap parameters = new SimpleMap();
		parameters.put("data", link);

		addSessionParameters(parameters);

		return post(String.format("data/links/%1$s", linkId), parameters, null, null, false);
	}

	public Observable<ProxibaseResponse> muteLinkById(String entityId, String linkId, Boolean muted) {

		SimpleMap link = new SimpleMap();
		link.put("mute", muted);

		SimpleMap parameters = new SimpleMap();
		parameters.put("data", link);

		addSessionParameters(parameters);

		return post(String.format("data/links/%1$s", linkId), parameters, null, null, false);
	}

	public Observable<ProxibaseResponse> deleteLinkById(String linkId) {

		SimpleMap parameters = new SimpleMap();
		addSessionParameters(parameters);

		return delete(String.format("data/links/%1$s", linkId), parameters, null, false);
	}

	public Observable<ProxibaseResponse> deleteLink(String fromId, String toId, String type) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("query[_to]", toId);
		parameters.put("query[_from]", fromId);
		parameters.put("query[type]", type);

		addSessionParameters(parameters);

		return delete("data/links", parameters, null, false);
	}

	/*--------------------------------------------------------------------------------------------
	* Bing
	*--------------------------------------------------------------------------------------------*/

	public Observable<BingResponse> loadSearchImages(String query, Integer limit, Integer offset) {

		SimpleMap parameters = new SimpleMap();

		String queryEncoded = Utils.encode(query);
		parameters.put("Query", "'" + queryEncoded + "'");
		parameters.put("Market", "'en-US'");
		parameters.put("Adult", "'Strict'");
		parameters.put("ImageFilters", "'size:large'");
		parameters.put("$top", limit + 1);
		parameters.put("$skip", offset);
		parameters.put("$format", "Json");

		String password = ContainerManager.getContainerHolder().getContainer().getString(Patchr.BING_ACCESS_KEY);
		String token = "Basic " + Base64.encodeToString((":" + password).getBytes(), Base64.NO_WRAP);

		if (!NetworkManager.getInstance().isConnected()) {
			return Observable.error(new NoNetworkException("Not connected to network"));
		}
		else {
			return bingApi.get(token, "Image", parameters)
				.map(responseMap -> {
					BingResponse response = BingResponse.setPropertiesFromMap(new BingResponse(), responseMap);
					if (response.count.intValue() > limit) {
						response.more = true;
						response.data.remove(response.data.size() - 1);
					}
					return response;
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * User and install
	 *--------------------------------------------------------------------------------------------*/

	public Observable<ProxibaseResponse> signup(SimpleMap data) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("data", data);
		parameters.put("secret", ContainerManager.getContainerHolder().getContainer().getString(Patchr.USER_SECRET));
		parameters.put("installId", Patchr.getInstance().getinstallId());

		return postEntity("user/create", parameters);
	}

	public Observable<ProxibaseResponse> logout(String userId, String sessionKey) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("user", userId);
		parameters.put("session", sessionKey);

		return get("auth/signout", parameters, false);
	}

	public Observable<ProxibaseResponse> login(String email, String password) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("email", email);
		parameters.put("password", password);
		parameters.put("installId", Patchr.getInstance().getinstallId());

		return post("auth/signin", parameters, null, null, false)
			.flatMap(response -> {
				String userId = response.user.id;
				String sessionKey = response.session.key;
				return fetchEntity(userId, FetchStrategy.IgnoreCache, userId, sessionKey);
			})
			.doOnNext(response -> {
				RealmEntity user = response.data.get(0);
				Session session = response.session;
				UserManager.shared().setCurrentUser(user, session);
			});
	}

	public Observable<ProxibaseResponse> tokenLogin(String token, String authType) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("authorization_code", token);
		parameters.put("getEntities", true);
		parameters.put("install", Patchr.getInstance().getinstallId());
		RealmEntity.extras(Constants.SCHEMA_ENTITY_USER, parameters);

		return post("auth/ak", parameters, null, null, true)
			.doOnNext(response -> {
				RealmEntity user = response.data.get(0);
				Session session = response.session;
				UserManager.shared().setCurrentUser(user, session);
			});
	}

	public Observable<ProxibaseResponse> validEmail(String email) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("q[email]", email);

		return get("find/users", parameters, false);
	}

	public Observable<ProxibaseResponse> updatePassword(final String userId, final String passwordOld, final String passwordNew) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("userId", userId);
		parameters.put("oldPassword", passwordOld);
		parameters.put("newPassword", passwordNew);
		parameters.put("installId", Patchr.getInstance().getinstallId());
		addSessionParameters(parameters);

		return post("user/pw/change", parameters, null, null, false).doOnNext(response -> {
			UserManager.shared().handlePasswordChange(response);
		});
	}

	public Observable<ProxibaseResponse> requestPasswordReset(final String email) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("email", email);
		parameters.put("installId", Patchr.getInstance().getinstallId());
		addSessionParameters(parameters);

		return post("user/pw/reqreset", parameters, null, null, false);
	}

	public Observable<ProxibaseResponse> resetPassword(final String password, final String token) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("token", token);
		parameters.put("password", password);
		parameters.put("installId", Patchr.getInstance().getinstallId());

		return post("user/pw/reset", parameters, null, null, false)
			.flatMap(response -> {
				String userId = response.user.id;
				String sessionKey = response.session.key;
				return fetchEntity(userId, FetchStrategy.IgnoreCache, userId, sessionKey);
			})
			.doOnNext(response -> {
				RealmEntity user = response.data.get(0);
				Session session = response.session;
				UserManager.shared().setCurrentUser(user, session);
			});
	}

	public Observable<ProxibaseResponse> registerInstall() {

		if (!NetworkManager.getInstance().isConnected()) {
			return Observable.error(new NoNetworkException("Not connected to network"));
		}
		else {
			SimpleMap install = new SimpleMap();
			install.put("installId", Patchr.getInstance().getinstallId());
			install.put("parseInstallId", ParseInstallation.getCurrentInstallation().getInstallationId());
			install.put("clientVersionName", Patchr.getVersionName(Patchr.applicationContext, MainScreen.class));
			install.put("clientVersionCode", Patchr.getVersionCode(Patchr.applicationContext, MainScreen.class));
			install.put("clientPackageName", Patchr.applicationContext.getPackageName());
			install.put("deviceName", AndroidManager.getInstance().getDeviceName());
			install.put("deviceType", "android");
			install.put("deviceVersionName", Build.VERSION.RELEASE); // Android version number. E.g., "1.0" or "3.4b5"

			if (install.get("parseInstallId") == null) {
				throw new IllegalStateException("parseInstallId cannot be null");
			}

			SimpleMap parameters = new SimpleMap();
			parameters.put("install", install);

			return post("do/registerInstall", parameters, null, null, false);
		}
	}

	public Observable<ProxibaseResponse> updateProximity(@NotNull final Location location) {

		SimpleMap parameters = new SimpleMap();
		parameters.put("installId", Patchr.getInstance().getinstallId());
		addLocationParameter(parameters, location);
		addSessionParameters(parameters);

		return post("do/updateProximity", parameters, null, null, false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Rest
	 *--------------------------------------------------------------------------------------------*/

	private Observable<ProxibaseResponse> post(final String path, final SimpleMap parameters, final String queryId, final Integer skip, final Boolean updateRealm) {

		if (!NetworkManager.getInstance().isConnected()) {
			return Observable.error(new NoNetworkException("Not connected to network"));
		}
		else {
			return proxiApi.post(path, parameters)
				.map(responseMap -> {
					if (!responseMap.isSuccessful()) {
						throwServiceException(responseMap.errorBody());
					}
					ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
					if (updateRequired(response)) {
						throw new ClientVersionException();
					}
					if (updateRealm && !response.noop && response.data.size() > 0) {
						updateRealm(response, queryId, skip);
					}
					return response;
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
		}
	}

	private Observable<ProxibaseResponse> get(final String path, final SimpleMap parameters, final Boolean updateRealm) {

		if (!NetworkManager.getInstance().isConnected()) {
			return Observable.error(new NoNetworkException("Not connected to network"));
		}
		else {
			return proxiApi.get(path, parameters)
				.map(responseMap -> {
					if (!responseMap.isSuccessful()) {
						throwServiceException(responseMap.errorBody());
					}
					ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
					if (updateRealm && !response.noop) {
						updateRealm(response, null, null);
					}
					return response;
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
		}
	}

	private Observable<ProxibaseResponse> delete(final String path, final SimpleMap parameters, final String entityId, final Boolean updateRealm) {

		if (!NetworkManager.getInstance().isConnected()) {
			return Observable.error(new NoNetworkException("Not connected to network"));
		}
		else {
			return proxiApi.delete(path, parameters)
				.map(responseMap -> {
					if (!responseMap.isSuccessful()) {
						throwServiceException(responseMap.errorBody());
					}
					ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
					if (updateRealm && entityId != null) {
						Realm realm = Realm.getDefaultInstance();
						realm.beginTransaction();
						RealmEntity realmEntity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();
						if (realmEntity != null) {
							realmEntity.deleteFromRealm();
						}
						realm.commitTransaction();
						realm.close();
					}
					return response;
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Helpers
	 *--------------------------------------------------------------------------------------------*/

	public void updateRealm(@NotNull ProxibaseResponse response, String queryId, Integer skip) {

		Query realmQuery = null;
		Realm realm = Realm.getDefaultInstance();
		realm.beginTransaction();

		if (queryId != null) {
			realmQuery = realm.where(Query.class).equalTo("id", queryId).findFirst();
			realmQuery.more = (response.more != null && response.more);
			realmQuery.executed = true;
			realmQuery.activityDate = DateTime.nowDate().getTime();

			if (skip != null && skip == 0) {
				realmQuery.entities.clear();
			}
		}

		Location location = LocationManager.getInstance().getLocationLocked();
		/*
		 * Not doing any deletes so entities that have been deleted from the service
		 * will be orphaned in the cache.
		 */
		List<RealmEntity> entities = (List<RealmEntity>) response.data;
		for (RealmEntity entity : entities) {
			RealmEntity realmEntity = realm.copyToRealmOrUpdate(entity);
			if (location != null && realmEntity.getLocation() != null) {
				realmEntity.distance = location.distanceTo(realmEntity.getLocation());  // Cache distance at time of query
			}
			if (realmQuery != null && !realmQuery.entities.contains(realmEntity)) {
				realmQuery.entities.add(realmEntity);
			}
		}

		realm.commitTransaction();
		realm.close();
	}

	private void addSessionParameters(SimpleMap parameters) {
		parameters.put("user", UserManager.userId);
		parameters.put("session", UserManager.sessionKey);
	}

	private void addQueryParameter(SimpleMap parameters, FetchStrategy strategy, String id) {
		if (id != null && strategy != FetchStrategy.IgnoreCache) {
			Realm realm = Realm.getDefaultInstance();
			RealmEntity realmEntity = realm.where(RealmEntity.class).equalTo("id", id).findFirst();
			if (realmEntity != null) {
				Map<String, Object> criteria = realmEntity.criteria(true);
				if (criteria != null) {
					parameters.put("query", criteria);
				}
			}
			realm.close();
		}
	}

	private void addLocationParameter(SimpleMap parameters, Location location) {
		SimpleMap locationMap = new SimpleMap();

		locationMap.put("lat", location.lat);
		locationMap.put("lng", location.lng);

		List<Double> geoarray = new ArrayList<Double>();
		geoarray.add(0, location.lng);
		geoarray.add(1, location.lat);
		locationMap.put("geometry", geoarray);

		if (location.accuracy != null) {
			locationMap.put("accuracy", location.accuracy);
		}

		parameters.put("location", locationMap);
	}

	private boolean updateRequired(ProxibaseResponse response) {
		String packageName = Patchr.applicationContext.getPackageName();
		if (response.clientMinVersions != null && response.clientMinVersions.containsKey(packageName)) {
			Integer clientVersion = Patchr.getVersionCode(Patchr.applicationContext, MainScreen.class);
			Integer clientVersionMin = ((Double) response.clientMinVersions.get(packageName)).intValue();
			if (clientVersionMin > clientVersion) {
				return true;
			}
		}
		return false;
	}

	public static Query getQuery(String queryName, String contextEntityId) {
		QuerySpec querySpec = getQuerySpec(queryName);
		Realm realm = Realm.getDefaultInstance();
		Query query = realm.where(Query.class).equalTo("id", querySpec.getId(contextEntityId)).findFirst();
		if (query == null) {
			realm.beginTransaction();
			Query realmQuery = new Query();
			realmQuery.id = querySpec.getId(contextEntityId);
			query = realm.copyToRealm(realmQuery);
			realm.commitTransaction();
		}
		return query;
	}

	public static QuerySpec getQuerySpec(String queryName) {
		QuerySpec querySpec = QuerySpec.Factory(queryName);
		return querySpec;
	}

	public void throwServiceException(ResponseBody errorBody) {
		try {
			String bodyString = errorBody.string();
			SimpleMap bodyMap = Patchr.gson.fromJson(bodyString, SimpleMap.class);
			ProxibaseError error = ProxibaseError.setPropertiesFromMap(new ProxibaseError(), (Map<String, Object>) bodyMap.get("error"));
			throw error.asServiceException();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
