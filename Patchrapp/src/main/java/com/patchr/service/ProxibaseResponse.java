package com.patchr.service;

import com.patchr.model.RealmEntity;
import com.patchr.objects.Session;
import com.patchr.utilities.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class ProxibaseResponse {

	public List<RealmEntity>   data;
	public Number              date;
	public Number              count;
	public Boolean             more;
	public Boolean             noop;
	public String              info;
	public Number              time;
	public RealmEntity         user;
	public RealmEntity         entity;
	public ProxibaseError      error;
	public Session             session;
	public Map<String, Object> clientMinVersions;

	public Number httpCode = 200;
	public String httpMessage;
	public Number serviceCode = 200;
	public String serviceMessage;

	public static ProxibaseResponse setPropertiesFromMap(ProxibaseResponse response, Response<Map<String, Object>> responseMap) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		Map<String, Object> map = responseMap.body();
		response.date = (Number) map.get("date");
		response.count = (Number) map.get("count");
		response.more = (Boolean) map.get("more");
		response.info = (String) map.get("info");
		response.time = (Number) map.get("time");
		response.noop = false;

		Boolean canEdit = (Boolean) map.get("canEdit");

		if (map.get("data") == null) {
			response.noop = true;
		}
		else if (map.get("data") instanceof List) {
			List<RealmEntity> entities = new ArrayList<RealmEntity>();
			List<Map<String, Object>> data_maps = (List<Map<String, Object>>) map.get("data");
			if (data_maps.size() == 0 && !Type.isTrue(canEdit)) {
				response.noop = true;
			}
			if (!response.noop) {
				for (Map<String, Object> data_map : data_maps) {
					String schema = (String) data_map.get("schema");
					if (schema != null) {
						RealmEntity entity = RealmEntity.setPropertiesFromMap(new RealmEntity(), data_map);
						entities.add(entity);
					}
				}
				response.data = entities;
			}
		}
		else if (map.get("data") instanceof Map) {
			List<RealmEntity> entities = new ArrayList<RealmEntity>();
			Map<String, Object> data_map = (Map<String, Object>) map.get("data");
			String schema = (String) data_map.get("schema");
			if (schema != null) {
				RealmEntity entity = RealmEntity.setPropertiesFromMap(new RealmEntity(), data_map);
				entities.add(entity);
				response.data = entities;
			}
		}

		response.httpCode = responseMap.code();
		response.httpMessage = responseMap.message();
		response.serviceCode = responseMap.code();
		response.serviceMessage = responseMap.message();

		if (map.get("entity") != null) {    // Not a fully decorated entity
			response.entity = RealmEntity.setPropertiesFromMap(new RealmEntity(), (Map<String, Object>) map.get("entity"), true);
		}
		if (map.get("user") != null) {      // Not a fully decorated user
			response.user = RealmEntity.setPropertiesFromMap(new RealmEntity(), (Map<String, Object>) map.get("user"), true);
		}
		if (map.get("error") != null) {
			response.error = ProxibaseError.setPropertiesFromMap(new ProxibaseError(), (Map<String, Object>) map.get("error"));
			if (response.error != null) {
				response.serviceCode = response.error.code;
				response.serviceMessage = response.error.message;
			}
		}
		if (map.get("session") != null) {
			response.session = Session.setPropertiesFromMap(new Session(), (Map<String, Object>) map.get("session"));
		}
		if (map.get("clientMinVersions") != null) {
			response.clientMinVersions = (Map<String, Object>) map.get("clientMinVersions");
		}

		response.httpCode = responseMap.code();
		response.httpMessage = responseMap.message();

		return response;
	}

	public boolean isSuccessful() {
		return httpCode.intValue() >= 200 && httpCode.intValue() < 300;
	}
}