package com.aircandi.events;

@SuppressWarnings("ucd")
public class RegisterInstallEvent {
	public Boolean force = false;

	public RegisterInstallEvent(Boolean force) {
		this.force = force;
	}
}
