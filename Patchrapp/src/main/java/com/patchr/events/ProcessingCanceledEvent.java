package com.patchr.events;

@SuppressWarnings("ucd")
public class ProcessingCanceledEvent {
	public Boolean force;

	public ProcessingCanceledEvent(Boolean force) {
		this.force = force;
	}
}
