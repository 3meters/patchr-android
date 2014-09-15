package com.aircandi.exceptions;

public class ServiceException extends IllegalStateException {
	private static final long serialVersionUID = -713258658623599999L;

	public ServiceException() {
	}

	public ServiceException(String detailMessage) {
		super(detailMessage);
	}
}