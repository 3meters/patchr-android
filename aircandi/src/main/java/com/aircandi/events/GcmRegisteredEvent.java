package com.aircandi.events;

import com.aircandi.service.ServiceResponse;

@SuppressWarnings("ucd")
public class GcmRegisteredEvent {

	public final ServiceResponse serviceResponse;

	public GcmRegisteredEvent(ServiceResponse serviceResponse) {
		this.serviceResponse = serviceResponse;
	}
}
