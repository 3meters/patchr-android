package com.aircandi.events;

import com.aircandi.utilities.Errors.ErrorResponse;

@SuppressWarnings("ucd")
public class DataErrorEvent extends DataEventBase {

	public ErrorResponse errorResponse;

	public DataErrorEvent(ErrorResponse errorResponse) {
		this.errorResponse = errorResponse;
	}
}
