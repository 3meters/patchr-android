package com.patchr.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

@SuppressWarnings("ucd")
public class Location {

	private static final long                      serialVersionUID = 455904759787968585L;
	private static final android.location.Location fromLocation     = new android.location.Location("from");
	private static final android.location.Location toLocation       = new android.location.Location("to");

	public Double  lat;
	public Double  lng;
	public Double  altitude;
	public Float  accuracy;
	public Float  bearing;
	public Float  speed;
	public String provider;

	public static Location setPropertiesFromMap(Location location, Map map) {

		location.lat = (Double) map.get("lat");
		location.lng = (Double) map.get("lng");
		location.altitude = (Double) map.get("altitude");
		location.accuracy = map.get("accuracy") != null ? ((Double) map.get("accuracy")).floatValue() : null;
		location.bearing = map.get("bearing") != null ? ((Double) map.get("bearing")).floatValue() : null;
		location.speed = map.get("speed") != null ? ((Double) map.get("speed")).floatValue() : null;
		location.provider = (String) map.get("provider");

		return location;
	}

	public Float distanceTo(Location location) {

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

	public boolean sameAs(Object obj) {
		if (obj == null) return false;
		if (!((Object) this).getClass().equals(obj.getClass())) return false;
		final Location other = (Location) obj;
		return (this.lat.doubleValue() == other.lat.doubleValue()
			&& this.lng.doubleValue() == other.lng.doubleValue());
	}

	public LatLng asLatLng() {
		return new LatLng(this.lat, this.lng);
	}
}