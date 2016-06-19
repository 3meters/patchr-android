package com.patchr.events;

import android.location.Location;

@SuppressWarnings("ucd")
public class LocationUpdatedEvent {
	public final Location location;

	public LocationUpdatedEvent(Location location) {
		this.location = location;
	}
}
