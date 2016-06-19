package com.patchr.events;

@SuppressWarnings("ucd")
public class LinkDeleteEvent extends DataEventBase {

	public String  fromId;
	public String  toId;
	public String  type;
	public Boolean enabled;
	public String  schema;
	public String  actionEvent;
}
