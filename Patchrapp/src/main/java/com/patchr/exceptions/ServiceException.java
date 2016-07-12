package com.patchr.exceptions;

import java.util.List;

public class ServiceException extends IllegalStateException {
	private static final long serialVersionUID = -713258658623599999L;

	public String       name;
	public Number       code;           // Status code from the service (200, 403, etc.)
	public Number       status;         // Status code from the network stack (403.11, etc)
	public String       message;        // Technical error message
	public String       description;    // Curated message to show user
	public String       errors;
	public List<String> stack;
	public List<String> appStack;       // optional

	public ServiceException() {}

	public ServiceException(String detailMessage) {
		super(detailMessage);
	}
}