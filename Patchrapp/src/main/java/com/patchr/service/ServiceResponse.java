package com.patchr.service;

import android.support.annotation.NonNull;

import com.patchr.objects.enums.ResponseCode;

@SuppressWarnings("ucd")
public class ServiceResponse {

	public Object data;
	public ResponseCode responseCode = ResponseCode.SUCCESS;
	public Integer statusCode;
	public Float   statusCodeService;
	public String  statusMessage;
	public String  contentType         = "none";
	@NonNull
	public String  contentEncoding     = "none";
	public Long    contentLength       = 0L;
	@NonNull
	public Long    contentLengthScaled = 0L;
	@NonNull
	public Integer contentHeight       = 0;
	@NonNull
	public Integer contentWidth        = 0;
	@NonNull
	public Integer contentHeightScaled = 0;
	@NonNull
	public Integer contentWidthScaled  = 0;
	public String        activityName;
	public Object        tag;

	public Exception exception;

	public ServiceResponse() {
	}

	public ServiceResponse(ResponseCode resultCode, Object data, Exception exception) {
		this.responseCode = resultCode;
		this.data = data;
		this.exception = exception;
	}
}