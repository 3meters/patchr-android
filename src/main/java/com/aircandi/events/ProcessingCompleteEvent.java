package com.aircandi.events;

import com.aircandi.components.NetworkManager.ResponseCode;

@SuppressWarnings("ucd")
public class ProcessingCompleteEvent {

	public ResponseCode responseCode = ResponseCode.SUCCESS;

	public ProcessingCompleteEvent(ResponseCode responseCode) {
		this.responseCode = responseCode;
	}
}
