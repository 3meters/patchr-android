package com.patchr.service;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.patchr.Constants;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.S3;
import com.patchr.events.TaskDoneEvent;
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

public class PostEntityJob extends Job {

	public static int PRIORITY = 500;
	public String    path;
	public SimpleMap data;

	public PostEntityJob(String path, SimpleMap data) {
		super(new Params(PRIORITY).requireNetwork().persist());
		this.path = path;
		this.data = data;
	}

	@Override public void onAdded() {
		/* Has been persisted */
		Logger.d(this, "Job persisted to job queue");
	}

	@Override public void onRun() throws Throwable {
		postEntity(path, data);
		Dispatcher.getInstance().post(new TaskDoneEvent(TaskTag.POST_ENTITY, ResponseCode.SUCCESS));
	}

	@Override protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
		Dispatcher.getInstance().post(new TaskDoneEvent(TaskTag.POST_ENTITY, ResponseCode.FAILED));
	}

	@Override
	protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
		if (throwable instanceof IOException) {
			return RetryConstraint.RETRY;
		}
		return RetryConstraint.CANCEL;
	}

	protected void postEntity(String path, SimpleMap data) throws Throwable {

		if (data.containsKey("photo")) {
			Photo photo = (Photo) data.get("photo");
			if (photo != null) {
				Photo photoFinal = postPhotoToS3(photo);
				data.put("photo", photoFinal);
			}
		}

		Call<Response<Map<String, Object>>> call = RestClient.getInstance().postEntityCall(path, data);
		Response<Response<Map<String, Object>>> responseMap = call.execute();

		if (!responseMap.isSuccessful()) {
			ServiceException exception = new ServiceException();
			exception.code = responseMap.code();
			exception.message = responseMap.message();
			throw exception;
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
