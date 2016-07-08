package com.patchr.service;

import java.util.Map;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface ProxibaseApi {

	@POST("auth/signin")
	Observable<Response<Map<String, Object>>> login(@Body Map<String, Object> parameters);

	@GET("auth/signout")
	Observable<Response<Map<String, Object>>> logout(@Query("user") String userId, @Query("session") String sessionKey);

	@POST("find/{collection}/{id}")
	Observable<Response<Map<String, Object>>> findById(@Path("collection") String collection, @Path("id") String id, @Body Map<String, Object> parameters);

	@GET("find/users")
	Observable<Response<Map<String, Object>>> findByEmail(@Query("q[email]") String email);

	@POST("{path}")
	Observable<Response<Map<String, Object>>> fetch(@Path(value = "path", encoded = true) String path, @Body Map<String, Object> parameters);
}