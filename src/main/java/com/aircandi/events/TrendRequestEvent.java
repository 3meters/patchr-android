package com.aircandi.events;

@SuppressWarnings("ucd")
public class TrendRequestEvent extends DataRequestEventBase {

	public String toSchema;
	public String fromSchema;
	public String linkType;

	public TrendRequestEvent() {}

	public TrendRequestEvent setToSchema(String toSchema) {
		this.toSchema = toSchema;
		return this;
	}

	public TrendRequestEvent setFromSchema(String fromSchema) {
		this.fromSchema = fromSchema;
		return this;
	}

	public TrendRequestEvent setLinkType(String linkType) {
		this.linkType = linkType;
		return this;
	}
}
