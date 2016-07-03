package com.patchr.objects;

import java.util.Map;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class LinkCount {

	public String  to;
	public String  from;
	public String  type;
	public Boolean enabled;
	public Integer count;

	public LinkCount() {}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static LinkCount setPropertiesFromMap(LinkCount linkCount, Map map) {

		linkCount.from = (String) map.get("from");
		linkCount.to = (String) map.get("to");
		linkCount.type = (String) map.get("type");
		linkCount.enabled = (Boolean) map.get("enabled");
		linkCount.count = map.get("count") != null ? ((Double) map.get("count")).intValue() : null;;

		return linkCount;
	}
}
