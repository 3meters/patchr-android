package com.aircandi.service;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.aircandi.components.Stopwatch;
import com.aircandi.objects.Session;
import com.amazonaws.util.Base64;

/**
 * Here is the typical code to construct a service request:
 * <p/>
 * <pre>
 *
 *
 * {
 * 	&#064;code
 * 	ServiceRequest serviceRequest = new ServiceRequest();
 * 	serviceRequest.setUri(mServiceUri + getCollection());
 * 	serviceRequest.setRequestType(RequestType.INSERT);
 * 	serviceRequest.setRequestBody(ProxibaseService.convertObjectToJsonSmart(this, true));
 * 	serviceRequest.setResponseFormat(ResponseFormat.JSON);
 * 	serviceRequest.setRequestListener(listener);
 * 	NetworkManager.getInstance().requestAsync(serviceRequest);
 * }
 * </pre>
 *
 * @author Jayma
 */

public class ServiceRequest {

	private String         mUri;
	private String         mRequestBody;
	private String         mActivityName;
	private Bundle         mParameters;
	private RequestType    mRequestType;
	private ResponseFormat mResponseFormat;
	private Session        mSession;
	private String         mUserName;
	private String         mPassword;
	private Object         mTag;
	private Stopwatch      mStopwatch;
	private Boolean  mErrorCheck               = true;
	private AuthType mAuthType                 = AuthType.NONE;
	private Boolean  mUseSecret                = false;
	private Boolean  mIgnoreResponseData       = false;
	private boolean  mSuppressUI               = false;
	private Boolean  mRetryOnConnectionFailure = true;

	@SuppressWarnings("ucd")
	public enum AuthType {
		NONE,
		BASIC,
		OAUTH
	}

	public ServiceRequest() {}

	public ServiceRequest(String uri, RequestType requestType, ResponseFormat responseFormat) {

		mUri = uri;
		mRequestType = requestType;
		mResponseFormat = responseFormat;
	}

	@NonNull
	public ServiceRequest setUri(String uri) {

		mUri = uri;
		return this;
	}

	public String getUri() {

		return mUri;
	}

	public String getUriWithQuery() {

		String uri = mUri;
		String sessionInfo = sessionInfo();
		if (!TextUtils.isEmpty(sessionInfo)) {
			if (uri.contains("?")) {
				uri += "&" + sessionInfo;
			}
			else {
				uri += "?" + sessionInfo;
			}
		}
		return uri;
	}

	@NonNull
	private String sessionInfo() {

		String sessionInfo = "";
		if (mSession != null) {
			sessionInfo = "user=" + mSession.ownerId + "&";
			sessionInfo += "session=" + mSession.key;
		}
		return sessionInfo;
	}

	public RequestType getRequestType() {

		return mRequestType;
	}

	public ResponseFormat getResponseFormat() {

		return mResponseFormat;
	}

	public String getRequestBody() {

		return mRequestBody;
	}

	public Bundle getParameters() {

		return mParameters;
	}

	public boolean isSuppressUI() {

		return mSuppressUI;
	}

	public Session getSession() {

		return mSession;
	}

	public String getUserName() {

		return mUserName;
	}

	public String getPassword() {

		return mPassword;
	}

	@NonNull
	public String getPasswordBase64() {

		final byte[] accountKeyBytes = Base64.encode((mPassword + ":" + mPassword).getBytes());
		final String accountKeyEnc = new String(accountKeyBytes);
		return accountKeyEnc;
	}

	public AuthType getAuthType() {

		return mAuthType;
	}

	public Boolean getUseSecret() {

		return mUseSecret;
	}

	public Boolean getIgnoreResponseData() {

		return mIgnoreResponseData;
	}

	public String getActivityName() {

		return mActivityName;
	}

	public Object getTag() {

		return mTag;
	}

	public Boolean getErrorCheck() {

		return mErrorCheck;
	}

	public Stopwatch getStopwatch() {

		return mStopwatch;
	}

	public Boolean getRetryOnConnectionFailure() {

		return mRetryOnConnectionFailure;
	}

	@NonNull
	public ServiceRequest setRequestType(RequestType requestType) {

		mRequestType = requestType;
		return this;
	}

	@NonNull
	public ServiceRequest setResponseFormat(ResponseFormat responseFormat) {

		mResponseFormat = responseFormat;
		return this;
	}

	@NonNull
	public ServiceRequest setRequestBody(String requestBody) {

		mRequestBody = requestBody;
		return this;
	}

	@NonNull
	public ServiceRequest setParameters(Bundle parameters) {

		mParameters = parameters;
		return this;
	}

	@NonNull
	public ServiceRequest setSuppressUI(boolean suppressUI) {

		mSuppressUI = suppressUI;
		return this;
	}

	@NonNull
	public ServiceRequest setSession(Session session) {

		mSession = session;
		return this;
	}

	@NonNull
	public ServiceRequest setUserName(String userName) {

		mUserName = userName;
		return this;
	}

	@NonNull
	public ServiceRequest setPassword(String password) {

		mPassword = password;
		return this;
	}

	@NonNull
	public ServiceRequest setAuthType(AuthType authType) {

		mAuthType = authType;
		return this;
	}

	@NonNull
	public ServiceRequest setUseSecret(Boolean useSecret) {

		mUseSecret = useSecret;
		return this;
	}

	@NonNull
	public ServiceRequest setIgnoreResponseData(Boolean ignoreResponseData) {

		mIgnoreResponseData = ignoreResponseData;
		return this;
	}

	@NonNull
	public ServiceRequest setActivityName(String activityName) {

		mActivityName = activityName;
		return this;
	}

	@NonNull
	public ServiceRequest setTag(Object tag) {

		mTag = tag;
		return this;
	}

	@NonNull
	public ServiceRequest setErrorCheck(Boolean errorCheck) {

		mErrorCheck = errorCheck;
		return this;
	}

	@NonNull
	public ServiceRequest setStopwatch(Stopwatch stopwatch) {

		mStopwatch = stopwatch;
		return this;
	}

	@NonNull
	public ServiceRequest setRetryOnConnectionFailure(Boolean retryOnConnectionFailure) {

		mRetryOnConnectionFailure = retryOnConnectionFailure;
		return this;
	}
}
