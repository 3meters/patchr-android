package com.patchr.service;

import com.patchr.exceptions.ServiceException;

import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ProxibaseError {

	public String       name;
	public Number       code;           // Status code from the service (403.11, etc)
	public Number       status;         // Status code from the network (200, 403, etc.)
	public String       message;        // Technical error message
	public String       description;    // Curated message to show user
	public String       errors;
	public List<String> stack;
	public List<String> appStack;       // optional

	public static ProxibaseError setPropertiesFromMap(ProxibaseError error, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		error.name = (String) map.get("name");
		error.code = (Number) map.get("code");
		error.status = (Number) map.get("status");
		error.message = (String) map.get("message");

		if (map.get("stack") != null) {
			error.stack = (List<String>) map.get("stack");
		}
		if (map.get("appStack") != null) {
			error.appStack = (List<String>) map.get("appStack");
		}

		return error;
	}

	public ServiceException asServiceException() {
		ServiceException exception = new ServiceException(this.message);
		exception.name = this.name;
		exception.code = this.code;
		exception.status = this.status;
		exception.message = this.message;
		exception.description = this.description;
		return exception;
	}
}