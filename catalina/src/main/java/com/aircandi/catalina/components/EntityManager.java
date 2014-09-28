package com.aircandi.catalina.components;

// import static java.util.Arrays.asList;

import java.util.List;

import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.ServiceConstants;
import com.aircandi.catalina.Constants;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Links;
import com.aircandi.objects.ServiceData;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.utilities.Json;

public class EntityManager extends com.aircandi.components.EntityManager {

	public synchronized ModelResult loadMessages(String entityId, Links linkOptions, Cursor cursor) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getMessages")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Aircandi.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			result.serviceResponse.data = serviceData;
			result.data = loadedEntities;
		}

		return result;
	}

	public synchronized ModelResult loadAlerts(String entityId, Cursor cursor) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getAlerts")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (!Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			serviceRequest.setSession(Aircandi.getInstance().getCurrentUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			result.serviceResponse.data = serviceData;
			result.data = loadedEntities;
		}

		return result;
	}

	public ModelResult deleteMessage(String entityId, Boolean cacheOnly, String seedParentId) {
		/*
		 * We sequence calls to delete the message and if the message is a seed then
		 * we add a second call to remove any links from replies to the patch.
		 */
		ModelResult result = deleteEntity(entityId, cacheOnly);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS && seedParentId != null) {
			result = removeLinks(entityId, seedParentId, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, "remove");
		}
		return result;
	}
}