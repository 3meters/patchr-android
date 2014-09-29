package com.aircandi.objects;

import com.aircandi.service.Expose;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ServiceData {

	@Expose
	public Object  data;
	@Expose
	public Object  d;						/* for Bing */
	@Expose
	public Number  date;
	@Expose
	public Number  count;
	@Expose
	public Boolean more;
	@Expose
	public String  info;

	@Expose
	public User                user;
	@Expose
	public ServiceError        error;
	@Expose
	public Session             session;
	@Expose
	public Number              time;
	@Expose
	public Map<String, Object> clientMinVersions;

	public static ServiceData setPropertiesFromMap(ServiceData serviceData, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		serviceData.data = map.get("data");
		serviceData.d = map.get("d");
		serviceData.date = (Number) map.get("date");
		serviceData.count = (Number) map.get("count");
		serviceData.more = (Boolean) map.get("more");
		serviceData.info = (String) map.get("info");
		if (map.get("user") != null) {
			serviceData.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}
		if (map.get("error") != null) {
			serviceData.error = ServiceError.setPropertiesFromMap(new ServiceError(), (HashMap<String, Object>) map.get("error"), nameMapping);
		}
		if (map.get("session") != null) {
			serviceData.session = Session.setPropertiesFromMap(new Session(), (HashMap<String, Object>) map.get("session"), nameMapping);
		}
		serviceData.time = (Number) map.get("time");
		serviceData.clientMinVersions = (HashMap<String, Object>) map.get("clientMinVersions");

		return serviceData;
	}

}