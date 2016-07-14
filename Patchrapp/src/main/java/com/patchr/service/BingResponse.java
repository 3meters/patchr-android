package com.patchr.service;

import com.patchr.objects.ImageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class BingResponse {

	public List<ImageResult> data;
	public Number            count;
	public Boolean           more;
	public Number httpCode = 200;
	public String httpMessage;

	public static BingResponse setPropertiesFromMap(BingResponse response, Response<Map<String, Object>> responseMap) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		Map<String, Object> map = responseMap.body();
		response.httpCode = responseMap.code();
		response.httpMessage = responseMap.message();

		if (map.get("d") == null) {
			response.count = 0;
			return response;
		}
		else if (map.get("d") instanceof Map) {
			Map<String, Object> data_map = (Map<String, Object>) map.get("d");
			List<Map<String, Object>> imageMaps = (List<Map<String, Object>>) data_map.get("results");
			List<ImageResult> imageResults = new ArrayList<ImageResult>();

			if (imageMaps.size() == 0) {
				response.count = 0;
				return response;
			}

			for (Map<String, Object> imageMap : imageMaps) {
				ImageResult imageResult = ImageResult.setPropertiesFromMap(new ImageResult(), imageMap);
				imageResults.add(imageResult);
			}

			response.data = imageResults;
			response.count = imageResults.size();
		}

		return response;
	}
}