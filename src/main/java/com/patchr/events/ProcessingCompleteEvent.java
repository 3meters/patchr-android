package com.patchr.events;

@SuppressWarnings("ucd")
public class ProcessingCompleteEvent {

	public Object tag;          // Uniquely identifies the event source

	public ProcessingCompleteEvent() {}

	public ProcessingCompleteEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
