package com.aircandi.objects;

import android.support.annotation.NonNull;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
public class Count extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 455904759787968585L;

	@Expose
	public String  type;
	@Expose
	public String  schema;
	@Expose
	public Boolean enabled;
	@Expose
	public Number  count;

	public Count() {
	}

	public Count(@NonNull String type, @NonNull String schema, Boolean enabled, @NonNull Number count) {
		this.type = type;
		this.schema = schema;
		this.enabled = enabled;
		this.count = count;
	}

	@Override
	public Count clone() {
		try {
			final Count count = (Count) super.clone();
			return count;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Count setPropertiesFromMap(Count stat, Map map, Boolean nameMapping) {
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
}