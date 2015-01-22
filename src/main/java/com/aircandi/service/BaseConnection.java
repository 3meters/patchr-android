package com.aircandi.service;

import android.support.annotation.NonNull;

import com.aircandi.interfaces.IConnection;
import com.aircandi.service.AirHttpRequest.Header;
import com.aircandi.service.ServiceRequest.AuthType;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseConnection implements IConnection {

	@NonNull
	@Override
	public abstract ServiceResponse request(ServiceRequest serviceRequest);

	@NonNull
	protected static AirHttpRequest buildHttpRequest(@NonNull final ServiceRequest serviceRequest) {

		AirHttpRequest airHttpRequest = new AirHttpRequest();
		airHttpRequest.uri = serviceRequest.getUriWithQuery();
		airHttpRequest.responseFormat = serviceRequest.getResponseFormat();
		addHeaders(airHttpRequest, serviceRequest);

		/* Construct the request */
		airHttpRequest.requestType = serviceRequest.getRequestType();
		if (serviceRequest.getRequestType() == RequestType.GET || serviceRequest.getRequestType() == RequestType.DELETE) {
			return airHttpRequest;
		}

		StringBuilder requestBody = new StringBuilder(5000);

		requestBody.append('{');

		if (serviceRequest.getRequestType() == RequestType.INSERT) {
			if (serviceRequest.getRequestBody() != null) {
				requestBody.append("\"data\":" + serviceRequest.getRequestBody() + ",");
			}
		}
		else if (serviceRequest.getRequestType() == RequestType.UPDATE) {
			if (serviceRequest.getRequestBody() != null) {
				requestBody.append("\"data\":" + serviceRequest.getRequestBody() + ",");
			}
		}

		/* Method parameters */
		if (serviceRequest.getParameters() != null && serviceRequest.getParameters().size() != 0) {

			for (String key : serviceRequest.getParameters().keySet()) {
				if (serviceRequest.getParameters().get(key) != null) {

					requestBody.append("\"" + key + "\":");
					
					/* String arrays */
					if (serviceRequest.getParameters().get(key) instanceof ArrayList<?>) {

						List<String> items = serviceRequest.getParameters().getStringArrayList(key);
						if (items.size() == 0) {
							requestBody.append("[],");
						}
						else {
							requestBody.append('[');
							for (String itemString : items) {
								if (itemString.startsWith("object:")) {
									requestBody.append(itemString.substring(7) + ",");
								}
								else {
									requestBody.append("\"" + itemString + "\",");
								}
							}
							requestBody.replace(requestBody.length() - 1, requestBody.length(), "],");
						}
					}

					/* Strings and objects */
					else if (serviceRequest.getParameters().get(key) instanceof String) {
						String value = serviceRequest.getParameters().get(key).toString();
						if (value.startsWith("object:")) {
							requestBody.append(serviceRequest.getParameters().get(key).toString().substring(7) + ",");
						}
						else {
							requestBody.append("\"" + serviceRequest.getParameters().get(key).toString() + "\",");
						}
					}

					/* Numbers and booleans */
					else {
						requestBody.append(serviceRequest.getParameters().get(key).toString() + ",");
					}
				}
			}
		}
		
		/* We assume there is always a trailing comma */
		requestBody.replace(requestBody.length() - 1, requestBody.length(), "");
		requestBody.append('}');

		airHttpRequest.requestBody = requestBody.toString();

		return airHttpRequest;
	}

	private static void addHeaders(@NonNull AirHttpRequest airHttpRequest, @NonNull ServiceRequest serviceRequest) {
		if (serviceRequest.getRequestType() != RequestType.GET) {
			airHttpRequest.headers.add(new Header("Content-Type", "application/json"));
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.JSON) {
			airHttpRequest.headers.add(new Header("Accept", "application/json"));
		}
		if (serviceRequest.getAuthType() == AuthType.BASIC) {
			airHttpRequest.headers.add(new Header("Authorization", "Basic " + serviceRequest.getPasswordBase64()));
		}
	}

	protected static Boolean isContentType(@NonNull String contentType, @NonNull String target) {
		return contentType.contains(target);
	}

	protected static String getContentType(@NonNull HttpURLConnection connection, @NonNull AirHttpRequest request) {
		String contentType = connection.getContentType();
		/*
		 * Some requests come back without contentType set. Example is images from
		 * foursquare. The request is diverted to a content delivery network which
		 * treats content as typeless blobs. So we try to infer the type based
		 * on what we requested.
		 */
		if (contentType == null) {
			if (request.responseFormat == ResponseFormat.JSON) {
				contentType = "application/json";
			}
			else {
				contentType = "text/*";
			}
		}
		return contentType;
	}

	public static class ContentType {
		@NonNull
		public static String TEXT  = "text";
		@NonNull
		public static String IMAGE = "image";
		@NonNull
		public static String JSON  = "json";
	}
}
