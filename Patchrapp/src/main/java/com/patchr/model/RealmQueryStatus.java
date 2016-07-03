package com.patchr.model;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmQueryStatus extends RealmObject {

	@PrimaryKey
	public String id;
	public boolean executed       = false;
	public boolean more           = false;

	public static RealmQueryStatus copyToRealmOrUpdate(RealmQueryStatus query) {
		Realm realm = Realm.getDefaultInstance();
		realm.beginTransaction();
		RealmQueryStatus realmQueryStatus = realm.copyToRealmOrUpdate(query);
		realm.commitTransaction();
		realm.close();
		return realmQueryStatus;
	}
}
