package com.patchr.events;

@SuppressWarnings("ucd")
public class ProcessingProgressEvent {
	public double percent;

	public ProcessingProgressEvent(double percent) {
		this.percent = percent;
	}
}
