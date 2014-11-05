package com.aircandi.objects;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class AirLocation extends ServiceObject implements Cloneable, Serializable {

	private static final long                      serialVersionUID = 455904759787968585L;
	private static final android.location.Location fromLocation     = new android.location.Location("from");
	private static final android.location.Location toLocation       = new android.location.Location("to");

	@Expose
	public Number lat;
	@Expose
	public Number lng;
	@Expose
	public Number altitude;
	@Expose
	public Number accuracy;
	@Expose
	public Number bearing;
	@Expose
	public Number speed;
	@Expose
	public String provider;

	/*
	 * Client only
	 */
	public Boolean zombie = false;

	public AirLocation() {
	}

	public AirLocation(Number lat, Number lng) {
		this.lat = lat;
		this.lng = lng;
	}

	@Override
	public AirLocation clone() {
		try {
			final AirLocation clone = (AirLocation) super.clone();
			return clone;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static AirLocation setPropertiesFromMap(AirLocation location, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		location.lat = (Number) map.get("lat");
		location.lng = (Number) map.get("lng");
		location.altitude = (Number) map.get("altitude");
		location.accuracy = (Number) map.get("accuracy");
		location.bearing = (Number) map.get("bearing");
		location.speed = (Number) map.get("speed");
		location.provider = (String) map.get("provider");
		location.zombie = (Boolean) map.get("zombie");
		return location;
	}

	public Float distanceTo(AirLocation location) {

		fromLocation.setLatitude(this.lat.doubleValue());
		fromLocation.setLongitude(this.lng.doubleValue());
		toLocation.setLatitude(location.lat.doubleValue());
		toLocation.setLongitude(location.lng.doubleValue());

		return fromLocation.distanceTo(toLocation);
	}
}