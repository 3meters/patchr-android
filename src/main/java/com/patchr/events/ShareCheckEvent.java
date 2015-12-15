package com.patchr.events;

@SuppressWarnings("ucd")
public class ShareCheckEvent extends DataEventBase {

	public String entityId;
	public String userId;

	public ShareCheckEvent() {}

	public ShareCheckEvent setEntityId(String entityId) {
		this.entityId = entityId;
		return this;
	}

	public ShareCheckEvent setUserId(String userId) {
		this.userId = userId;
		return this;
	}
}
