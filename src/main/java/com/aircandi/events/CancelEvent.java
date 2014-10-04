package com.aircandi.events;

@SuppressWarnings("ucd")
public class CancelEvent {
	public Boolean force;

	public CancelEvent(Boolean force) {
		this.force = force;
	}
}
