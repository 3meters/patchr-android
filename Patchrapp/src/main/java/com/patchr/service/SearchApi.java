package com.patchr.service;

import com.patchr.objects.SimpleMap;

import java.util.Map;

import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import rx.Observable;

public interface SearchApi {

	@GET("{path}")
	Observable<Response<Map<String, Object>>> get(@Header("Ocp-Apim-Subscription-Key") String authorization
		, @Path(value = "path", encoded = true) String path
		, @QueryMap SimpleMap parameters);
}