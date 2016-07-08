package com.patchr.objects;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

@SuppressWarnings("ucd")
public class LocationOld extends ServiceObject {

	private static final long                      serialVersionUID = 455904759787968585L;
	private static final android.location.Location fromLocation     = new android.location.Location("from");
	private static final android.location.Location toLocation       = new android.location.Location("to");

	public Number lat;
	public Number lng;
	public Number altitude;
	public Number accuracy;
	public Number bearing;
	public Number speed;
	public String provider;

	public LocationOld() {}

	public LocationOld(@NonNull Number lat, @NonNull Number lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public static LocationOld setPropertiesFromMap(LocationOld location, Map map) {
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
		return location;
	}

	public Float distanceTo(@NonNull LocationOld location) {

		if (this.lat == null || this.lng == null) {
			throw new IllegalArgumentException("Attempted to call distanceTo on location without lat/lng");
		}

		if (location.lat == null || location.lng == null) {
			throw new IllegalArgumentException("Attempted to call distanceTo using location without lat/lng");
		}

		fromLocation.setLatitude(this.lat.doubleValue());
		fromLocation.setLongitude(this.lng.doubleValue());
		toLocation.setLatitude(location.lat.doubleValue());
		toLocation.setLongitude(location.lng.doubleValue());

		return fromLocation.distanceTo(toLocation);
	}

	public LatLng asLatLng() {
		return new LatLng(this.lat.doubleValue(), this.lng.doubleValue());
	}

	@Override public LocationOld clone() {
		try {
			final LocationOld clone = (LocationOld) super.clone();
			return clone;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}
}