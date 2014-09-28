package com.aircandi.service;

import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

public class OkHttp extends BaseConnection {
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

	public static final MediaType MEDIA_TYPE_TEXT  = MediaType.parse("text/plain");
	public static final MediaType MEDIA_TYPE_HTML  = MediaType.parse("text/html");
	public static final MediaType MEDIA_TYPE_JSON  = MediaType.parse("application/json");

	private OkHttpClient client;

	public OkHttp() {
		/*
		 * If sslSocketFactory is not overridden then creates own ssl context to avoid
		 * global conflicts. This will overwrite global ssl context customizations.
		 */
		client = new OkHttpClient();
		client.setConnectTimeout(ServiceConstants.TIMEOUT_CONNECTION, TimeUnit.MILLISECONDS);
		client.setReadTimeout(ServiceConstants.TIMEOUT_SOCKET_READ, TimeUnit.MILLISECONDS);
		client.setWriteTimeout(ServiceConstants.TIMEOUT_SOCKET_WRITE, TimeUnit.MILLISECONDS);
	}

	@Override
	public ServiceResponse request(final ServiceRequest serviceRequest) {

		final ServiceResponse serviceResponse = new ServiceResponse();
		Response response = null;
		Request.Builder builder = new Request.Builder().tag(serviceRequest.getTag());

		try {

			/* Build okttp request */

			final AirHttpRequest airRequest = BaseConnection.buildHttpRequest(serviceRequest);
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

			/* Execute request */

			response = client.newCall(builder.build()).execute();
			if (serviceRequest.getStopwatch() != null) {
				serviceRequest.getStopwatch().segmentTime("Http service: request execute completed");
			}

			/* Process response */

			serviceResponse.statusCode = response.code();
			serviceResponse.statusMessage = response.message();
			serviceResponse.activityName = serviceRequest.getActivityName();
			serviceResponse.tag = serviceRequest.getTag();

			if (!response.isSuccessful()) {
				/*
				 * We got an error at the service application layer
				 */
				logErrorResponse(response);
				serviceResponse.responseCode = ResponseCode.FAILED;
				return serviceResponse;
			}
			else {
				serviceResponse.data = response.body().string();
				serviceResponse.contentType = getContentType(response, airRequest);
				serviceResponse.contentLength = response.body().contentLength();
				return serviceResponse;
			}
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
				return serviceResponse;
			}
			else {
				return new ServiceResponse(ResponseCode.FAILED, null, exception);
			}
		}
	}

	public void requestAsync(final ServiceRequest serviceRequest) throws MalformedURLException {

		final ServiceResponse serviceResponse = new ServiceResponse();
		serviceResponse.activityName = serviceRequest.getActivityName();
		Request.Builder builder = new Request.Builder().tag(serviceRequest.getTag());

		final AirHttpRequest airRequest = BaseConnection.buildHttpRequest(serviceRequest);
		builder.url(airRequest.uri);

		for (AirHttpRequest.Header header : airRequest.headers) {
			builder.addHeader(header.key, header.value);
		}

		if (airRequest.requestType == RequestType.INSERT
				|| airRequest.requestType == RequestType.UPDATE
				|| airRequest.requestType == RequestType.METHOD) {
			RequestBody body = RequestBody.create(MEDIA_TYPE_TEXT, airRequest.requestBody);
			builder.post(body);
		}
		else if (airRequest.requestType == RequestType.DELETE) {
			builder.delete();
		}

		Request request = builder.build();
		client.newCall(request).enqueue(new Callback() {

			@Override
			public void onFailure(Request request, IOException e) {
					/*
					 * Called when the request could not be executed due to cancellation, a
					 * connectivity problem or timeout. Because networks can fail during an
					 * exchange, it is possible that the remote server accepted the request
					 * before the failure.
					 */
			}

			@Override
			public void onResponse(Response response) throws IOException {
					/*
					 * Called when the HTTP response was successfully returned by the remote
					 * server. The callback may proceed to read the response body. The response
					 * is still live until its response body is closed with response.body().close().
					 * The recipient of the callback may even consume the response body on another thread.
					 *
					 * Note that transport-layer success (receiving a HTTP response code,
					 * headers and body) does not necessarily indicate application-layer
					 * success: response may still indicate an unhappy HTTP response
					 * code like 404 or 500.
					 */
				if (!response.isSuccessful()) {
					throw new IOException("Unexpected code " + response);
				}
				else {

					if (serviceRequest.getStopwatch() != null) {
						serviceRequest.getStopwatch().segmentTime("Http service: request execute completed");
					}

					serviceResponse.statusCode = response.code();
					serviceResponse.statusMessage = response.message();
					serviceResponse.tag = serviceRequest.getTag();
					serviceResponse.data = response.body().string();
					serviceResponse.contentType = getContentType(response, airRequest);
					serviceResponse.contentLength = response.body().contentLength();
				}
			}
		});

	}

	private void logErrorResponse(Response response) {

		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
		final StringBuilder stringBuilder = new StringBuilder(); // $codepro.audit.disable defineInitialCapacity

		try {

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line + System.getProperty("line.separator"));
			}
			bufferedReader.close();
		}
		catch (IOException e) {
		}

		String responseContent = stringBuilder.toString();
		Logger.d(this, responseContent);
	}

	protected static String getContentType(Response response, AirHttpRequest request) {
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

	public OkHttpClient getClient() {
		return client;
	}
}
