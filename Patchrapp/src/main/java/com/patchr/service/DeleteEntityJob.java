package com.patchr.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.patchr.components.Logger;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class DeleteEntityJob extends Job {

	public static int PRIORITY = 500;
	public String path;

	public DeleteEntityJob(String path) {
		super(new Params(PRIORITY).requireNetwork().persist());
		this.path = path;
	}

	public DeleteEntityJob(String path, String groupId) {
		super(new Params(PRIORITY).requireNetwork().setGroupId(groupId).persist());
		this.path = path;
	}

	@Override public void onAdded() {
		/* Has been persisted */
		Logger.d(this, "Job persisted to job queue");
	}

	@Override public void onRun() throws Throwable {
		Logger.d(this, String.format("Job running"));

		Call<Map<String, Object>> call = RestClient.getInstance().deleteEntityCall(path);
		Response<Map<String, Object>> responseMap = call.execute();

		if (!responseMap.isSuccessful()) {
			RestClient.getInstance().throwServiceException(responseMap.errorBody());
		}
	}

	@Override protected void onCancel(int cancelReason, @Nullable Throwable throwable) {}

	@Override
	protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
		Logger.d(this, String.format("Job throws: %1$s", throwable.getMessage()));
		if (throwable instanceof IOException) {
			return RetryConstraint.RETRY;
		}
		return RetryConstraint.CANCEL;
	}
}