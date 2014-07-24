package com.aircandi.events;

import com.aircandi.service.ServiceResponse;

@SuppressWarnings("ucd")
public class UserInitializedEvent {
	
	public final ServiceResponse	serviceResponse;

	public UserInitializedEvent(ServiceResponse serviceResponse) {
		this.serviceResponse = serviceResponse;
	}
}
