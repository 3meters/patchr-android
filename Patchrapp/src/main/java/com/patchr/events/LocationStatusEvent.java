package com.patchr.events;

import android.location.Location;

import com.patchr.objects.enums.LocationStatus;

@SuppressWarnings("ucd")
public class LocationStatusEvent {
	public final Location       location;
	public final LocationStatus status;

	public LocationStatusEvent(LocationStatus status, Location location) {
		this.location = location;
		this.status = status;
	}
}
