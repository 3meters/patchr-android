package com.patchr.service;

import com.patchr.objects.SimpleMap;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import rx.Observable;

public interface ProxibaseApi {

	/* Observables */

	@POST("{path}")
	Observable<Response<Map<String, Object>>> post(@Path(value = "path", encoded = true) String path, @Body SimpleMap parameters);

	@GET("{path}")
	Observable<Response<Map<String, Object>>> get(@Path(value = "path", encoded = true) String path, @QueryMap SimpleMap parameters);

	@DELETE("{path}")
	Observable<Response<Map<String, Object>>> delete(@Path(value = "path", encoded = true) String path, @QueryMap SimpleMap parameters);

	/* Calls */

	@POST("{path}")
	Call<Map<String, Object>> postCall(@Path(value = "path", encoded = true) String path, @Body SimpleMap parameters);

	@GET("{path}")
	Call<Map<String, Object>> getCall(@Path(value = "path", encoded = true) String path, @QueryMap SimpleMap parameters);

	@DELETE("{path}")
	Call<Map<String, Object>> deleteCall(@Path(value = "path", encoded = true) String path, @QueryMap SimpleMap parameters);
}