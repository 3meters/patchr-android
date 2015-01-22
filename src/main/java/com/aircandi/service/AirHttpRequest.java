package com.aircandi.service;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class AirHttpRequest {

	public RequestType    requestType;
	public String         requestBody;
	public String         uri;
	public ResponseFormat responseFormat;
	@NonNull
	public List<Header> headers = new ArrayList<Header>();

	public static class Header {
		public String key;
		public String value;

		public Header(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}