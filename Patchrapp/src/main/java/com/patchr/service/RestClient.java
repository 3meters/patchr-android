package com.patchr.service;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.patchr.Patchr;
import com.patchr.components.Logger;
import com.patchr.components.UserManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.FetchStrategy;
import com.patchr.objects.Query;
import com.patchr.objects.Session;
import com.patchr.objects.SimpleMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
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
				UserManager.shared().setCurrentRealmUser(user, session, false);
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

	public Observable<ProxibaseResponse> fetchEntities(final FetchStrategy strategy, final Query query, final Integer skip) {

		SimpleMap linked = new SimpleMap();
		linked.put("to", query.entityCollection);
		linked.put("type", query.linkType);
		linked.put("limit", query.pageSize);
		linked.put("skip", skip);
		linked.put("more", true);
		RealmEntity.extras(query.schema, linked);

		SimpleMap parameters = new SimpleMap();
		parameters.put("linked", linked);
		parameters.put("promote", "linked");
		addSessionParameters(parameters);
		addQueryParameter(parameters, strategy, query.rootId);

		return RestClient.proxiApi.findById(query.rootCollection, query.rootId, parameters)
			.map(responseMap -> {

				ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
				if (!response.noop) {

					Realm realm = Realm.getDefaultInstance();
					realm.beginTransaction();

					if (skip == 0) {

						/* Clearing old entities is the only way to pickup deletes */
						RealmResults<RealmEntity> results = Realm.getDefaultInstance()
							.where(RealmEntity.class)
							.equalTo(query.filterField, query.filterValue)
							.equalTo("schema", query.schema)
							.findAllSorted(query.sortField, query.sortAscending ? Sort.ASCENDING : Sort.DESCENDING);

						for (RealmEntity entity : results) {
							RealmEntity.deleteNestedObjectsFromRealm(entity);
							entity.deleteFromRealm();
						}
					}

					List<RealmEntity> entities = (List<RealmEntity>) response.data;
					for (RealmEntity entity : entities) {
						RealmEntity.copyToRealmOrUpdate(realm, entity);
					}

					realm.commitTransaction();
					realm.close();
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
}
