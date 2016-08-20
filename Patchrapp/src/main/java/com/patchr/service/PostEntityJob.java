package com.patchr.service;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.patchr.Constants;
import com.patchr.components.Logger;
import com.patchr.components.S3;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.ResponseCode;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.io.IOException;
import java.util.Map;

import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Response;

public class PostEntityJob extends Job {

	public static int PRIORITY = 500;
	public String    path;
	public SimpleMap data;
	public String    entityId;

	public PostEntityJob(String path, SimpleMap data, String entityId, String groupId) {
		super(new Params(PRIORITY).requireNetwork().setGroupId(groupId).persist());
		this.path = path;
		this.data = data;
		this.entityId = entityId;
	}

	@Override public void onAdded() {
		/* Has been persisted */
		Logger.d(this, "Job persisted to job queue");
	}

	@Override public void onRun() throws Throwable {
		Logger.d(this, "Job running");
		Realm realm = Realm.getDefaultInstance();
		RealmEntity entity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();

		if (entity != null && entity.pending && !entity.posting) {
			realm.executeTransaction(whocares -> entity.posting = true);
			RealmEntity entityCopy = realm.copyFromRealm(entity);
			Photo photo = entityCopy.getPhoto();

			/* Convert local file photos */
			if (photo != null && photo.isFile()) {
				Photo photoFinal = postPhotoToS3(photo);
				entityCopy.setPhoto(photoFinal);
				realm.executeTransaction(whocares -> entity.setPhoto(photoFinal));
			}

			Call<Map<String, Object>> call = RestClient.getInstance().postEntityCall(path, entityCopy, data);
			Response<Map<String, Object>> responseMap = call.execute();

			if (!responseMap.isSuccessful()) {
				realm.close();
				RestClient.getInstance().throwServiceException(responseMap.errorBody());
			}
			else {
				realm.executeTransaction(whocares -> {
					entity.pending = false;
					entity.posting = false;
				});
				realm.close();
			}
		}
		else {
			realm.close();
		}
	}

	@Override protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
		Realm realm = Realm.getDefaultInstance();
		RealmEntity entity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();
		if (entity != null) {
			realm.executeTransaction(whocares -> entity.posting = false);
		}
		realm.close();
	}

	@Override
	protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
		Logger.d(this, String.format("Job throws: %1$s", throwable.getMessage()));
		if (throwable instanceof IOException) {
			return RetryConstraint.RETRY;
		}
		return RetryConstraint.CANCEL;
	}

	protected Photo postPhotoToS3(Photo photo) {

		Bitmap bitmap = Photo.getBitmapForPhoto(photo);
		if (bitmap == null) {
			return null;
		}
		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = UI.ensureBitmapScaleForS3(bitmap);

		/* Push it to S3. It is always formatted/compressed as a jpeg. */
		String imageKey = Utils.createImageKey(); // User id at root to avoid collisions
		ServiceResponse serviceResponse = S3.getInstance().putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			return new Photo(imageKey, bitmap.getWidth(), bitmap.getHeight(), Photo.PhotoSource.aircandi_images);
		}

		return null;
	}
}