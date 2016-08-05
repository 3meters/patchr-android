package com.patchr.service;

import com.patchr.objects.ImageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class CognitiveResponse {

	public List<ImageResult> data;
	public Number            count;
	public boolean           more;
	public Number httpCode = 200;
	public String httpMessage;

	public static CognitiveResponse setPropertiesFromMap(CognitiveResponse response, Response<Map<String, Object>> responseMap) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		Map<String, Object> map = responseMap.body();
		response.httpCode = responseMap.code();
		response.httpMessage = responseMap.message();

		if (map.get("value") == null) {
			response.count = 0;
			return response;
		}
		else {
			List<Map<String, Object>> imageMaps = (List<Map<String, Object>>) map.get("value");
			List<ImageResult> imageResults = new ArrayList<>();

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