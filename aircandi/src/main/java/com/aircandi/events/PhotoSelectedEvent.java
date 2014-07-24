package com.aircandi.events;

import com.aircandi.objects.Photo;

@SuppressWarnings("ucd")
public class PhotoSelectedEvent {

	public Photo	photo;

	public PhotoSelectedEvent(Photo photo) {
		this.photo = photo;
	}
}
