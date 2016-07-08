package com.patchr.model;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Query extends RealmObject {

	@PrimaryKey
	public String id;
	public boolean executed = false;
	public boolean more     = false;
	public RealmList<RealmEntity> entities;

	public static Query copyToRealmOrUpdate(Query query) {
		Realm realm = Realm.getDefaultInstance();
		realm.beginTransaction();
		Query realmQuery = realm.copyToRealmOrUpdate(query);
		realm.commitTransaction();
		realm.close();
		return realmQuery;
	}
}
