package com.patchr.objects;

import com.patchr.service.Expose;

import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ServiceError {

	@Expose
	public String       name;
	@Expose
	public Number       code;
	@Expose
	public Number       status;
	@Expose
	public String       message;
	@Expose
	public String       errors;
	@Expose
	public List<String> stack;
	@Expose
	public List<String> appStack;    // optional

	public static ServiceError setPropertiesFromMap(ServiceError serviceError, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		serviceError.name = (String) map.get("name");
		serviceError.code = (Number) map.get("code");
		serviceError.status = (Number) map.get("status");
		serviceError.message = (String) map.get("message");

		if (map.get("stack") != null) {
			serviceError.stack = (List<String>) map.get("stack");
		}
		if (map.get("appStack") != null) {
			serviceError.appStack = (List<String>) map.get("appStack");
		}

		return serviceError;
	}
}