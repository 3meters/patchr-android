package com.aircandi.objects;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Proximity extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 455904759787968585L;

	@Expose
	public Boolean primary;
	@Expose
	public Number  signal;

	@Override
	public Proximity clone() {
		try {
			final Proximity object = (Proximity) super.clone();
			return object;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Proximity setPropertiesFromMap(Proximity object, Map map, Boolean nameMapping) {
		object.primary = (Boolean) map.get("primary");
		object.signal = (Number) map.get("signal");
		return object;
	}
}