package com.aircandi.events;

import com.aircandi.objects.CacheStamp;

@SuppressWarnings("ucd")
public class EntityRequestEvent extends DataRequestEventBase {

	public Integer    linkProfile;
	public CacheStamp cacheStamp;

	public EntityRequestEvent() {}

	public EntityRequestEvent setLinkProfile(Integer linkProfile) {
		this.linkProfile = linkProfile;
		return this;
	}

	public EntityRequestEvent setCacheStamp(CacheStamp cacheStamp) {
		this.cacheStamp = cacheStamp;
		return this;
	}
}
