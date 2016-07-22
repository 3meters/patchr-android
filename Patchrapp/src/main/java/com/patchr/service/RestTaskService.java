package com.patchr.service;

import android.graphics.Bitmap;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.S3;
import com.patchr.exceptions.ServiceException;
import com.patchr.model.Photo;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.ResponseCode;
import com.patchr.objects.enums.TaskTag;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class RestTaskService extends GcmTaskService {

	public static final String ACTION_DONE  = "RestTaskService#ACTION_DONE";
	public static final String EXTRA_TAG    = "extra_tag";
	public static final String EXTRA_RESULT = "extra_result";

	@Override public int onRunTask(TaskParams taskParams) {

		int result = GcmNetworkManager.RESULT_SUCCESS;

		String tag = taskParams.getTag();

		if (TaskTag.POST_ENTITY.equals(tag)) {
			String path = taskParams.getExtras().getString("path");
			String paramsJson = taskParams.getExtras().getString("paramsJson");
			SimpleMap parameters = Patchr.gson.fromJson(paramsJson, SimpleMap.class);
			result = postEntity(path, parameters);
		}

		return result;
	}

	protected int postEntity(String path, SimpleMap data) {

		if (data.containsKey("photo")) {
			Photo photo = (Photo) data.get("photo");
			if (photo != null) {
				Photo photoFinal = postPhotoToS3(photo);
				data.put("photo", photoFinal);
			}
		}

		try {
			Call<Map<String, Object>> call = RestClient.getInstance().postEntityCall(path, data);
			Response<Map<String, Object>> responseMap = call.execute();

			if (!responseMap.isSuccessful()) {
				ServiceException exception = new ServiceException();
				exception.code = responseMap.code();
				exception.message = responseMap.message();
				throw exception;
			}
			ProxibaseResponse response = ProxibaseResponse.setPropertiesFromMap(new ProxibaseResponse(), responseMap);
			if (response.error != null) {
				throw response.error.asServiceException();
			}
			else if (!response.noop && response.data.size() > 0) {
				RestClient.getInstance().updateRealm(response, null, null);
			}

			return GcmNetworkManager.RESULT_SUCCESS;
		}
		catch (IOException e) {
			return GcmNetworkManager.RESULT_RESCHEDULE;
		}
		catch (Exception e) {
			return GcmNetworkManager.RESULT_FAILURE;
		}
	}

	protected Photo postPhotoToS3(Photo photo) {

		Bitmap bitmap = Photo.getBitmapForPhoto(photo);
		if (bitmap == null) {
			return null;
		}
		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = UI.ensureBitmapScaleForS3(bitmap);

		/* Push it to S3. It is always formatted/compressed as a jpeg. */
		String imageKey = Utils.getImageKey(); // User id at root to avoid collisions
		ServiceResponse serviceResponse = S3.getInstance().putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Photo photoFinal = new Photo(imageKey, bitmap.getWidth(), bitmap.getHeight(), Photo.PhotoSource.aircandi_images);
			return photoFinal;
		}

		return null;
	}
}
