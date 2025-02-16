package com.patchr.model;

import java.util.Map;

import io.realm.annotations.PrimaryKey;

public class Link {

	/* Service persisted fields */
	@PrimaryKey
	public String  id;
	public String  type;
	public String  fromId;
	public String  fromSchema;
	public String  toId;
	public String  toSchema;
	public Boolean enabled;
	public Boolean mute;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Link setPropertiesFromMap(Link link, Map map) {

		link.id = (String) (map.get("_id") != null ? map.get("_id") : map.get("id"));
		link.type = (String) map.get("type");
		link.fromId = (String) (map.get("_from") != null ? map.get("_from") : map.get("fromId"));
		link.fromSchema = (String) map.get("fromSchema");
		link.toId = (String) (map.get("_to") != null ? map.get("_to") : map.get("toId"));
		link.toSchema = (String) map.get("toSchema");
		link.enabled = (Boolean) map.get("enabled");
		link.mute = (Boolean) map.get("mute");

		return link;
	}
}
