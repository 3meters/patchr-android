package com.aircandi.events;

import com.aircandi.components.NetworkManager.ResponseCode;

@SuppressWarnings("ucd")
public class ProcessingFinishedEvent {

	public ResponseCode responseCode = ResponseCode.SUCCESS;

	public ProcessingFinishedEvent(ResponseCode responseCode) {
		this.responseCode = responseCode;
	}
}
