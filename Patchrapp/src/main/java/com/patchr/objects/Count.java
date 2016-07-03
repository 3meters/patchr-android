package com.patchr.objects;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
public class Count extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 455904759787968585L;

	public String  type;
	public String  schema;
	public Boolean enabled;
	public Number  count;

	public Count() {}

	public Count(String type, String schema, Boolean enabled, Number count) {
		this.type = type;
		this.schema = schema;
		this.enabled = enabled;
		this.count = count;
	}

	public static Count setPropertiesFromMap(Count stat, Map map) {

		stat.type = (String) map.get("type");
		if (stat.type == null && map.get("event") != null) {
			stat.type = (String) map.get("event");
		}
		stat.schema = (String) map.get("schema");
		stat.enabled = (Boolean) map.get("enabled");
		stat.count = (Number) map.get("count");
		if (stat.count == null && map.get("countBy") != null) {
			stat.count = (Number) map.get("countBy");
		}
		return stat;
	}

	@Override public Count clone() {
		try {
			final Count count = (Count) super.clone();
			return count;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}
}