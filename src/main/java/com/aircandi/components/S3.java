package com.aircandi.components;

import android.graphics.Bitmap;

import com.aircandi.Patchr;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.ProcessingCanceledEvent;
import com.aircandi.events.ProcessingProgressEvent;
import com.aircandi.service.ServiceResponse;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.squareup.otto.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;

public class S3 {

	private TransferManager mManager;
	private Upload          mUpload;

	private S3() {
		mManager = new TransferManager(Patchr.awsCredentials);
		try {
			Dispatcher.getInstance().register(this);
		}
		catch (IllegalArgumentException ignore) { /* ignore */ }
	}

	private static class S3Holder {
		public static final S3 instance = new S3();
	}

	public static S3 getInstance() {
		return S3Holder.instance;
	}

	public ServiceResponse putImage(final String imageKey, Bitmap bitmap, Integer quality) {

		try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

			final byte[] bitmapBytes = outputStream.toByteArray();
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
			final ObjectMetadata metadata = new ObjectMetadata();

			metadata.setContentLength(bitmapBytes.length);
			metadata.setContentType("image/jpeg");
			outputStream.close();

			mUpload = mManager.upload("aircandi-images", imageKey, inputStream, metadata);

			mUpload.addProgressListener(new ProgressListener() {
				public void progressChanged(ProgressEvent progressEvent) {
					if (mUpload == null) return;
					if (mUpload.getProgress().getPercentTransferred() <= 95) {
						Dispatcher.getInstance().post(new ProcessingProgressEvent(mUpload.getProgress().getPercentTransferred()));
					}
				}
			});

			mUpload.waitForCompletion();
			mManager.getAmazonS3Client().setObjectAcl("aircandi-images", imageKey, CannedAccessControlList.PublicRead);

			return new ServiceResponse();
		}
		catch (final AmazonServiceException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
		catch (final AmazonClientException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
		catch (InterruptedException exception) {
			return new ServiceResponse(ResponseCode.INTERRUPTED, null, exception);
		}
		catch (CancellationException exception) {
			return new ServiceResponse(ResponseCode.INTERRUPTED, null, exception);
		}
		catch (IOException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
	}

	@Subscribe
	public void onCancelEvent(ProcessingCanceledEvent event) {
		if (mUpload != null) {
			mUpload.abort();
			Logger.v(this, "Image upload aborted");
		}
	}
}
