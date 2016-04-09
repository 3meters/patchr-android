package com.patchr.service;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.components.Logger;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.utilities.Reporting;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OkHttp {
	/*
	 * Seems to throw SocketTimeout for everything
	 *
	 * - All the normal reasons for a read timeout while input/output streaming
	 * - Service host reachable but service not running.
	 *
	 * Retry handling
	 *
	 * - Only applies to trying to establish a connection and sending the request body.
	 * - Doesn't retry if there is no connection (service was never reached).
	 * - Doesn't retry if request body == null or request stream isn't retryable.
	 * - Doesn't retry if SSLHandshakeException, CertificateException, ProtocolException.
	 * - Doesn't retry if IOException is during response streaming.
	 */

	public static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain");
	public static final MediaType MEDIA_TYPE_HTML = MediaType.parse("text/html");
	public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

	private OkHttpClient client;

	public OkHttp() {
		/*
		 * If sslSocketFactory is not overridden then creates own ssl context to avoid
		 * global conflicts. This will overwrite global ssl context customizations.
		 */
		client = new OkHttpClient();

		/* Max connections is per host */
		client.setConnectionPool(new ConnectionPool(5, Constants.TIME_FIVE_MINUTES));
		client.setConnectTimeout(Constants.TIMEOUT_CONNECTION, TimeUnit.MILLISECONDS);
		client.setReadTimeout(Constants.TIMEOUT_SOCKET_READ, TimeUnit.MILLISECONDS);
		client.setWriteTimeout(Constants.TIMEOUT_SOCKET_WRITE, TimeUnit.MILLISECONDS);
		client.setRetryOnConnectionFailure(true);
	}

	public Response get(String path, String query) {
		try {
			String uri = path;
			if (query != null) {
				uri = uri + "?" + query;
			}

			Request request = new Request.Builder().url(uri).build();
			Response response = client.newCall(request).execute();
			return response;
		}
		catch (IOException e) { /* ignore */ }
		return null;
	}

	@NonNull
	public ServiceResponse request(@NonNull final ServiceRequest serviceRequest) {

		ServiceResponse serviceResponse = new ServiceResponse();
		Request request = null;
		Response response = null;
		Call call = null;
		AirHttpRequest airRequest = null;
		Request.Builder builder = new Request.Builder().tag(serviceRequest.getTag());

		try {

			/* Build okttp request */

			airRequest = buildHttpRequest(serviceRequest);
			builder.url(airRequest.uri);

			for (AirHttpRequest.Header header : airRequest.headers) {
				builder.addHeader(header.key, header.value);
			}

			if (airRequest.requestType == RequestType.INSERT
					|| airRequest.requestType == RequestType.UPDATE
					|| airRequest.requestType == RequestType.METHOD) {

				RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, airRequest.requestBody);
				builder.post(body);
			}
			else if (airRequest.requestType == RequestType.DELETE) {
				builder.delete();
			}

			request = builder.build();

			/* Execute request */
			OkHttpClient okHttpClient = client;
			if (!serviceRequest.getRetryOnConnectionFailure()) {
				okHttpClient = client.clone();
				okHttpClient.setRetryOnConnectionFailure(false);
			}

			call = okHttpClient.newCall(request);
			response = call.execute();
			if (serviceRequest.getStopwatch() != null) {
				serviceRequest.getStopwatch().segmentTime("Http service: request execute completed");
			}

			/* Process response */

			serviceResponse.statusCode = response.code();
			serviceResponse.statusMessage = response.message();
			serviceResponse.activityName = serviceRequest.getActivityName();
			serviceResponse.tag = serviceRequest.getTag();
			serviceResponse.data = response.body().string();
			serviceResponse.contentType = getContentType(response, airRequest);
			serviceResponse.contentLength = response.body().contentLength();

			if (!response.isSuccessful()) {
				/*
				 * We got an error at the service application layer
				 */
				logErrorResponse(response);
				serviceResponse.responseCode = ResponseCode.FAILED;
			}

			response.body().close();
			return serviceResponse;
		}
		catch (OutOfMemoryError error) {
			if (response != null) {
				if (response.isSuccessful()) {
					String contentType = getContentType(response, airRequest);
					try {
						Long contentLength = response.body().contentLength();
						Reporting.breadcrumb("OutOfMemoryError: success response:"
								+ " contentType: " + contentType
								+ " contentLength: " + String.valueOf(contentLength));
					}
					catch (IOException ignore) {/* Ignore */}
				}
			}
			throw error;
		}
		catch (IOException exception) {
			/*
			 * Called when the request could not be executed due to cancellation, a
			 * connectivity problem or timeout. Because networks can fail during an
			 * exchange, it is possible that the remote server accepted the request
			 * before the failure.
			 */
			if (response != null) {
				serviceResponse.statusCode = response.code();
				serviceResponse.statusMessage = response.message();
				serviceResponse.activityName = serviceRequest.getActivityName();
				serviceResponse.tag = serviceRequest.getTag();
				serviceResponse.exception = exception;
				serviceResponse.responseCode = ResponseCode.FAILED;
			}
			else {
				serviceResponse = new ServiceResponse(ResponseCode.FAILED, null, exception);
			}

			if (exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.US).contains("cancel")) {
				if (call.isCanceled()) {
					Logger.w(this, "Network request cancelled: " + request.tag());
				}
				serviceResponse.responseCode = ResponseCode.INTERRUPTED;
			}
		}
		return serviceResponse;
	}

	private void logErrorResponse(@NonNull Response response) {

		final StringBuilder stringBuilder = new StringBuilder(); // $codepro.audit.disable defineInitialCapacity

		//noinspection EmptyCatchBlock
		try {
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line + System.getProperty("line.separator"));
			}
			bufferedReader.close();
		}
		catch (IOException ignore) { /* Ignore */}

		String responseContent = stringBuilder.toString();
		Logger.d(this, responseContent);
	}

	protected static String getContentType(@NonNull Response response, @NonNull AirHttpRequest request) {
		MediaType contentType = response.body().contentType();
		/*
		 * Some requests come back without contentType set. Example is images from
		 * foursquare. The request is diverted to a content delivery network which
		 * treats content as typeless blobs. So we try to infer the type based
		 * on what we requested.
		 */
		if (contentType == null) {
			if (request.responseFormat == ResponseFormat.JSON) {
				contentType = MEDIA_TYPE_JSON;
			}
			else {
				contentType = MEDIA_TYPE_TEXT;
			}
		}
		return contentType.toString();
	}

	@NonNull protected static AirHttpRequest buildHttpRequest(@NonNull final ServiceRequest serviceRequest) {

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
						if (items == null || items.size() == 0) {
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
						//noinspection ConstantConditions
						String value = serviceRequest.getParameters().get(key).toString();
						if (value.startsWith("object:")) {
							requestBody.append(value.substring(7) + ",");
						}
						else {
							requestBody.append("\"" + value + "\",");
						}
					}

					/* Numbers and booleans */
					else {
						//noinspection ConstantConditions
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
			airHttpRequest.headers.add(new AirHttpRequest.Header("Content-Type", "application/json"));
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.JSON) {
			airHttpRequest.headers.add(new AirHttpRequest.Header("Accept", "application/json"));
		}
		if (serviceRequest.getAuthType() == ServiceRequest.AuthType.BASIC) {
			airHttpRequest.headers.add(new AirHttpRequest.Header("Authorization", "Basic " + serviceRequest.getPasswordBase64()));
		}
	}
}
