package com.aircandi.service;

import android.text.TextUtils;

import com.aircandi.Aircandi;
import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.objects.ServiceData;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Json;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;

public class OkHttpUrlConnection extends BaseConnection {
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

	private OkHttpClient client;

	public OkHttpUrlConnection() {

		client = new OkHttpClient();
		client.setFollowSslRedirects(true);
		client.setConnectTimeout(ServiceConstants.TIMEOUT_CONNECTION, TimeUnit.MILLISECONDS);
		client.setReadTimeout(ServiceConstants.TIMEOUT_SOCKET_READ, TimeUnit.MILLISECONDS);
		/*
		 * Hack to deal with ssl context conflict that blows up any other
		 * software we are using that also works with ssl.
		 */
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, null, null);
		}
		catch (GeneralSecurityException e) {
			throw new AssertionError(); // The system has no TLS. Just give up.
		}
		client.setSslSocketFactory(sslContext.getSocketFactory());
	}

	@Override
	public ServiceResponse request(ServiceRequest serviceRequest) {

		ServiceResponse serviceResponse = new ServiceResponse();
		serviceResponse.activityName = serviceRequest.getActivityName();
		HttpURLConnection connection = null;
		InputStream inputStream = null;

		try {

			AirHttpRequest request = BaseConnection.buildHttpRequest(serviceRequest);
			URL url = new URL(request.uri);
			connection = new OkUrlFactory(client).open(url);

			if (request.requestType == RequestType.GET) {
				connection.setRequestMethod("GET");
			}
			else if (request.requestType == RequestType.INSERT) {
				connection.setRequestMethod("POST");
			}
			else if (serviceRequest.getRequestType() == RequestType.UPDATE) {
				connection.setRequestMethod("POST");
			}
			else if (serviceRequest.getRequestType() == RequestType.DELETE) {
				connection.setRequestMethod("DELETE");
			}
			else if (serviceRequest.getRequestType() == RequestType.METHOD) {
				connection.setRequestMethod("POST");
			}

			for (AirHttpRequest.Header header : request.headers) {
				connection.addRequestProperty(header.key, header.value);
			}

			/*
			 * Execute the request - can throw InterruptedException
			 */
			if (request.requestType == RequestType.GET) {
				inputStream = get(connection, request, serviceResponse);
			}
			else {
				inputStream = post(connection, request.requestBody, request, serviceResponse);
			}

			if (serviceRequest.getStopwatch() != null) {
				serviceRequest.getStopwatch().segmentTime("Http service: request execute completed");
			}

			serviceResponse.statusCode = connection.getResponseCode();
			serviceResponse.statusMessage = connection.getResponseMessage();

			if (connection.getResponseCode() / 100 == HttpURLConnection.HTTP_OK / 100) {
				/*
				 * Any 2.XX status code is considered success.
				 */
				if (inputStream != null) {
					/*
					 * Can throw InterruptedException
					 */
					Object response = handleResponse(inputStream, request, serviceResponse);

					if (serviceRequest.getStopwatch() != null) {
						serviceRequest.getStopwatch().segmentTime("Http service: response content captured");
					}

					/* Check for valid client version even if the call was successful */
					if (request.responseFormat == ResponseFormat.JSON && !serviceRequest.getIgnoreResponseData()) {
						/*
						 * We think anything json is coming from the Aircandi service (except Bing)
						 */
						ServiceData serviceData = (ServiceData) Json.jsonToObject((String) response, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);

						if (serviceRequest.getStopwatch() != null) {
							serviceRequest.getStopwatch().segmentTime("Http service: response content json ("
									+ String.valueOf(((String) response).length())
									+ " bytes) decoded to object");
						}

						if (serviceData != null
								&& serviceData.clientMinVersions != null
								&& serviceData.clientMinVersions.containsKey(Aircandi.applicationContext.getPackageName())) {

							Integer clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
							if ((Integer) serviceData.clientMinVersions.get(Aircandi.applicationContext.getPackageName()) > clientVersionCode)
								return new ServiceResponse(ResponseCode.FAILED, null, new ClientVersionException());
						}
					}

					serviceResponse.data = response;
				}

				return serviceResponse;
			}
			else {
				/*
				 * We got a non-success http status code so break it down.
				 */
				if (inputStream != null) {

					final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
					final StringBuilder stringBuilder = new StringBuilder(); // $codepro.audit.disable defineInitialCapacity

					String line;
					while ((line = bufferedReader.readLine()) != null) {
						stringBuilder.append(line + System.getProperty("line.separator"));
					}
					bufferedReader.close();

					String responseContent = stringBuilder.toString();
					Logger.d(this, responseContent);

					if (request.responseFormat == ResponseFormat.JSON) {
						/*
						 * We think anything json is coming from the Aircandi service.
						 */
						ServiceData serviceData = (ServiceData) Json.jsonToObject(responseContent, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);

						if (serviceData != null
								&& serviceData.clientMinVersions != null
								&& serviceData.clientMinVersions.containsKey(Aircandi.applicationContext.getPackageName())) {

							Integer clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
							if ((Integer) serviceData.clientMinVersions.get(Aircandi.applicationContext.getPackageName()) > clientVersionCode)
								return new ServiceResponse(ResponseCode.FAILED, null, new ClientVersionException());
							else if (serviceData.error != null && serviceData.error.code != null) {
								serviceResponse.statusCodeService = serviceData.error.code.floatValue();
							}
						}
					}
				}

				serviceResponse.responseCode = ResponseCode.FAILED;
				return serviceResponse;
			}
		}
		catch (InterruptedException exception) {
			/*
			 * We eat the exception and shut the flag off.
			 */
			Thread.interrupted();
			return new ServiceResponse(ResponseCode.INTERRUPTED, null, exception);
		}
		catch (IOException exception) {
			try {
				if (connection != null) {
					serviceResponse.statusCode = connection.getResponseCode();
					serviceResponse.statusMessage = connection.getResponseMessage();
					serviceResponse.exception = exception;
					serviceResponse.responseCode = ResponseCode.FAILED;
					return serviceResponse;
				}
				else
					return new ServiceResponse(ResponseCode.FAILED, null, exception);
			}
			catch (IOException secondException) {
				return new ServiceResponse(ResponseCode.FAILED, null, exception);
			}
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException exception) {
					return new ServiceResponse(ResponseCode.FAILED, null, exception);
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private Object handleResponse(InputStream inputStream, AirHttpRequest airHttpRequest, ServiceResponse serviceResponse)
			throws IOException, InterruptedException {

		if (isContentType(serviceResponse.contentType, ContentType.IMAGE)) {

			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

			// this dynamically extends to take the bytes you read
			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

			// this is storage overwritten on each iteration with bytes
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			// we need to know how may bytes were read to write them to the byteBuffer
			int len;
			while ((len = bufferedInputStream.read(buffer)) != -1 && !Thread.currentThread().isInterrupted()) {
				byteBuffer.write(buffer, 0, len);
			}
			bufferedInputStream.close();

			if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

			// and then we can return your byte array.
			serviceResponse.contentLength = (long) byteBuffer.size();
			return byteBuffer.toByteArray();
		}
		else if (isContentType(serviceResponse.contentType, ContentType.TEXT) || isContentType(serviceResponse.contentType, ContentType.JSON)) {

			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			final StringBuilder stringBuilder = new StringBuilder(); // $codepro.audit.disable defineInitialCapacity

			String line;
			while ((line = bufferedReader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				stringBuilder.append(line + System.getProperty("line.separator"));
			}
			bufferedReader.close();

			if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

			serviceResponse.contentLength = (long) stringBuilder.length();

			return stringBuilder.toString();
		}
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Post
	 *--------------------------------------------------------------------------------------------*/

	private InputStream post(HttpURLConnection connection, String string, AirHttpRequest request, ServiceResponse serviceResponse)
			throws IOException, InterruptedException {

		OutputStream outputStream = null;
		InputStream inputStream;

		try {

			connection.setRequestProperty("Accept-Encoding", "gzip");

			/*
			 * Some posts like DELETE send an empty body so the string is null/empty
			 */
			if (!TextUtils.isEmpty(string)) {
				byte[] data = string.getBytes();

				connection.setDoOutput(true);
				connection.setFixedLengthStreamingMode(data.length);

				// Set the output stream
				outputStream = connection.getOutputStream();
				outputStream.write(data);
				outputStream.flush();
				outputStream.close();
			}
			else {
				connection.connect();
			}

			if (connection.getResponseCode() / 100 == HttpURLConnection.HTTP_OK / 100) {
				inputStream = connection.getInputStream();
			}
			else {
				inputStream = connection.getErrorStream();
			}

			if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

			serviceResponse.contentType = getContentType(connection, request);
			if (connection.getContentEncoding() != null && connection.getContentEncoding().equals("gzip")) {
				serviceResponse.contentEncoding = "gzip";
				return new GZIPInputStream(inputStream);
			}
			else {
				serviceResponse.contentEncoding = "none";
				return inputStream;
			}
		}
		finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				}
				catch (IOException ignore) {}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Get
	 *--------------------------------------------------------------------------------------------*/

	private InputStream get(HttpURLConnection connection, AirHttpRequest request, ServiceResponse serviceResponse)
			throws IOException, InterruptedException {

		InputStream inputStream = connection.getInputStream();

		if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

		serviceResponse.contentType = getContentType(connection, request);
		if (connection.getContentEncoding() != null && connection.getContentEncoding().equals("gzip")) {
			serviceResponse.contentEncoding = "gzip";
			return new GZIPInputStream(inputStream);
		}
		else {
			serviceResponse.contentEncoding = "none";
			return inputStream;
		}
	}

	public OkHttpClient getClient() {
		return client;
	}

	public void setClient(OkHttpClient client) {
		this.client = client;
	}

}
