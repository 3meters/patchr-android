package com.aircandi.objects;

import java.util.List;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ServiceError {

	@Expose
	public String		name;
	@Expose
	public Number		code;
	@Expose
	public String		message;
	@Expose
	public String		errors;
	@Expose
	public List<String>	appStack;	// optional
	@Expose
	public String		stack;

	

	public static ServiceError setPropertiesFromMap(ServiceError serviceError, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		serviceError.name = (String) map.get("name");
		serviceError.code = (Number) map.get("code");
		serviceError.message = (String) map.get("message");
		serviceError.stack = (String) map.get("stack");

		if (map.get("appStack") != null) {
			serviceError.appStack = (List<String>) map.get("appStack");
		}

		return serviceError;
	}
}