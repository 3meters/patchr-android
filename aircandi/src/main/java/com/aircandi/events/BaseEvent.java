package com.aircandi.events;


@SuppressWarnings("ucd")
public abstract class BaseEvent {

	public final String			source;

	protected BaseEvent(String source) {
		this.source = source;
	}
}
