package com.patchr.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Query extends RealmObject {

	@PrimaryKey
	public String                 id;
	public boolean                executed;
	public boolean                more;
	public long                   activityDate;
	public RealmList<RealmEntity> entities;
}
