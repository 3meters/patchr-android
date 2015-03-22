package com.aircandi.events;

@SuppressWarnings("ucd")
public class EntityRequestEvent extends DataRequestEventBase {

	public Integer    linkProfile;

	public EntityRequestEvent() {}

	public EntityRequestEvent setLinkProfile(Integer linkProfile) {
		this.linkProfile = linkProfile;
		return this;
	}
}
