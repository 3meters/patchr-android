package com.aircandi.events;

@SuppressWarnings("ucd")
public class LinkDeleteEvent extends DataEventBase {

	public String  fromId;
	public String  toId;
	public String  type;
	public Boolean enabled;
	public String  schema;
	public String  actionEvent;

	public LinkDeleteEvent() {}

	public LinkDeleteEvent setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	public LinkDeleteEvent setFromId(String fromId) {
		this.fromId = fromId;
		return this;
	}

	public LinkDeleteEvent setToId(String toId) {
		this.toId = toId;
		return this;
	}

	public LinkDeleteEvent setType(String type) {
		this.type = type;
		return this;
	}

	public LinkDeleteEvent setEnabled(Boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public LinkDeleteEvent setActionEvent(String actionEvent) {
		this.actionEvent = actionEvent;
		return this;
	}
}
