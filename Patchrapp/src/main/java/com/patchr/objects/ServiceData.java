package com.patchr.objects;

import com.patchr.service.Expose;

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
	public Entity              entity;
	@Expose
	public ProxibaseError      error;
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
		if (map.get("entity") != null) {
			serviceData.entity = User.setPropertiesFromMap(new SimpleEntity(), (Map<String, Object>) map.get("entity"));
		}
		if (map.get("user") != null) {
			serviceData.user = User.setPropertiesFromMap(new User(), (Map<String, Object>) map.get("user"));
		}
		if (map.get("error") != null) {
			serviceData.error = ProxibaseError.setPropertiesFromMap(new ProxibaseError(), (Map<String, Object>) map.get("error"));
		}
		if (map.get("session") != null) {
			serviceData.session = Session.setPropertiesFromMap(new Session(), (Map<String, Object>) map.get("session"));
		}
		serviceData.time = (Number) map.get("time");
		serviceData.clientMinVersions = (Map<String, Object>) map.get("clientMinVersions");

		return serviceData;
	}
}