package com.patchr.service;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.UserManager;
import com.patchr.model.Location;
import com.patchr.model.Query;
import com.patchr.model.RealmEntity;
import com.patchr.objects.FetchStrategy;
import com.patchr.objects.QueryName;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.Session;
import com.patchr.objects.SimpleMap;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RestClient {

	private static ProxibaseApi proxiApi;
	private static RestClient instance = new RestClient();

	public static RestClient getInstance() {
		return instance;
	}

	private RestClient() {
		try {
			/* Configure http client */
			OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder().addNetworkInterceptor(new StethoInterceptor());

			Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("https://api.aircandi.com/v1/")
				.client(httpClient.build())
				.addConverterFactory(GsonConverterFactory.create())
				.addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
				.build();

			proxiApi = retrofit.create(ProxibaseApi.class);
		}
		catch (IllegalArgumentException ignore) {
			/* ignore */
			Logger.w(this, ignore.getLocalizedMessage());
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * User and install
	 *--------------------------------------------------------------------------------------------*/

	public Observable<ProxibaseResponse> logout(String userId, String sessionKey) {

		return RestClient.proxiApi.logout(userId, sessionKey)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.map(responseMap -> ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap));
	}

	public Observable<ProxibaseResponse> login(String email, String password) {

		final String installId = Patchr.getInstance().getinstallId();
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("email", email);
		parameters.put("password", password);
		parameters.put("installId", installId);

		return RestClient.proxiApi.login(parameters)
			.map(responseMap -> {
				return ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
			})
			.flatMap(response -> {
				String userId = response.user.shortcutForId;
				String sessionKey = response.session.key;
				return fetchEntity(userId, FetchStrategy.IgnoreCache, userId, sessionKey);
			})
			.doOnNext(response -> {
				RealmEntity user = response.data.get(0);
				Session session = response.session;
				UserManager.shared().setCurrentUser(user, session, false);
			})
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	public Observable<ProxibaseResponse> findByEmail(String email) {

		return RestClient.proxiApi.findByEmail(email)
			.map(responseMap -> ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap))
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	/*--------------------------------------------------------------------------------------------
	 * Fetch
	 *--------------------------------------------------------------------------------------------*/

	public Observable<ProxibaseResponse> fetchEntity(String id, FetchStrategy strategy) {
		if (UserManager.shared().authenticated()) {
			return fetchEntity(id, strategy, UserManager.userId, UserManager.sessionKey);
		}
		else {
			return fetchEntity(id, strategy, null, null);
		}
	}

	public Observable<ProxibaseResponse> fetchEntity(String id, FetchStrategy strategy, final String usingUserId, final String usingSession) {

		String schema = RealmEntity.getSchemaForId(id);
		String collection = RealmEntity.getCollectionForSchema(schema);

		SimpleMap parameters = new SimpleMap();
		RealmEntity.extras(schema, parameters);
		if (usingUserId != null) {
			parameters.put("user", usingUserId);
			parameters.put("session", usingSession);
		}
		addQueryParameter(parameters, strategy, id);

		return RestClient.proxiApi.findById(collection, id, parameters)
			.map(responseMap -> {
				ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
				/* Cherrypick session info so it's available downstream */
				if (usingSession != null) {
					response.session = new Session();
					response.session.key = usingSession;
				}
				if (!response.noop && response.data.size() > 0) {
					RealmEntity user = (RealmEntity) response.data.get(0);
					RealmEntity.copyToRealmOrUpdate(user);
				}
				return response;
			})
			.doOnError(throwable -> {
				Logger.w(this, throwable.toString());
			})
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
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

		return fetch(String.format("find/messages/%1$s", messageId), parameters, queryId, null, skip);
	}

	private Observable<ProxibaseResponse> fetchMembersForPatch(final FetchStrategy strategy, final QuerySpec querySpec, final String patchId, final Integer skip) {

		final String queryId = querySpec.getId(patchId);

		SimpleMap linked = new SimpleMap();
		linked.put("from", "users");
		linked.put("type", "watch");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_USER, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, patchId);

		return fetch(String.format("find/patches/%1$s", patchId), parameters, queryId, null, skip);
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

		return fetch(String.format("find/users/%1$s", userId), parameters, queryId, null, skip);
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

		return fetch(String.format("find/patches/%1$s", patchId), parameters, queryId, null, skip);
	}

	private Observable<ProxibaseResponse> fetchNotificationsForUser(final QuerySpec querySpec, final Integer skip) {

		final String queryId = querySpec.getId(null);

		SimpleMap parameters = new SimpleMap();
		parameters.put("limit", Constants.PAGE_SIZE);
		parameters.put("skip", skip);
		parameters.put("more", true);
		addSessionParameters(parameters);   // Notifications keys on the authenticated user id

		return fetch("user/getNotifications", parameters, queryId, null, skip);
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

		return fetch("patches/near", parameters, queryId, location, skip);
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

		return fetch(String.format("find/users/%1$s", userId), parameters, queryId, null, skip);
	}

	private Observable<ProxibaseResponse> fetchPatchesToExplore(final QuerySpec querySpec, final Integer skip) {

		final String queryId = querySpec.getId(null);

		SimpleMap parameters = new SimpleMap();
		parameters.put("limit", 50);
		parameters.put("skip", skip);
		parameters.put("more", true);
		RealmEntity.extras(Constants.SCHEMA_ENTITY_PATCH, parameters);
		addSessionParameters(parameters);

		return fetch("patches/interesting", parameters, queryId, null, skip);
	}

	private Observable<ProxibaseResponse> fetchPatchesUserMemberOf(final FetchStrategy strategy, final QuerySpec querySpec, final String userId, final Integer skip) {

		final String queryId = querySpec.getId(userId);

		SimpleMap linked = new SimpleMap();
		linked.put("to", "patches");
		linked.put("type", "watch");
		linked.put("limit", Constants.PAGE_SIZE);
		linked.put("skip", skip);
		linked.put("more", true);

		RealmEntity.extras(Constants.SCHEMA_ENTITY_PATCH, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, userId);

		return fetch(String.format("find/users/%1$s", userId), parameters, queryId, null, skip);
	}

	private Observable<ProxibaseResponse> fetch(final String path, final SimpleMap parameters, final String queryId, final Location location, final Integer skip) {

		return RestClient.proxiApi.fetch(path, parameters)
			.map(responseMap -> {
				ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
				if (!response.noop) {
					updateRealm(response, queryId, location, skip);
				}
				return response;
			})
			.doOnError(throwable -> {
				Logger.w(this, throwable.toString());
			})
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	/*--------------------------------------------------------------------------------------------
	 * Modify
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Helpers
	 *--------------------------------------------------------------------------------------------*/

	private void updateRealm(ProxibaseResponse response, String queryId, Location locationx, int skip) {

		Realm realm = Realm.getDefaultInstance();
		realm.beginTransaction();

		@NotNull Query realmQuery = realm.where(Query.class).equalTo("id", queryId).findFirst();
		realmQuery.more = (response.more != null && response.more);
		realmQuery.executed = true;

		if (skip == 0) {
			realmQuery.entities.clear();
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
			if (!realmQuery.entities.contains(realmEntity)) {
				realmQuery.entities.add(realmEntity);
			}
		}

		realm.commitTransaction();
		realm.close();
	}

	private void addSessionParameters(SimpleMap parameters) {
		if (UserManager.shared().authenticated()) {
			parameters.put("user", UserManager.userId);
			parameters.put("session", UserManager.sessionKey);
		}
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
		parameters.put("location", locationMap);
	}
}
