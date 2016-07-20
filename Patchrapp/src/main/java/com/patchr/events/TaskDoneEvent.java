package com.patchr.events;

import com.patchr.objects.enums.ResponseCode;

@SuppressWarnings("ucd")
public class TaskDoneEvent {
	public final String       tag;
	public final ResponseCode result;

	public TaskDoneEvent(String tag, ResponseCode result) {
		this.tag = tag;
		this.result = result;
	}
}
