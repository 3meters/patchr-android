package com.aircandi.components;

import android.graphics.Bitmap;

import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.CancelEvent;
import com.aircandi.objects.Photo.PhotoType;
import com.aircandi.service.ServiceResponse;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.squareup.otto.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;

public class S3 {

	private TransferManager mManager;
	private Upload          mUpload;

	private S3() {
		mManager = new TransferManager(Patch.awsCredentials);
		BusProvider.getInstance().register(this);
	}

	private static class S3Holder {
		public static final S3 instance = new S3();
	}

	public static S3 getInstance() {
		return S3Holder.instance;
	}

	public ServiceResponse putImage(final String imageKey, Bitmap bitmap, Integer quality, final PhotoType photoType) {

		try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
			final byte[] bitmapBytes = outputStream.toByteArray();
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
			final ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(bitmapBytes.length);
			metadata.setContentType("image/jpeg");
			outputStream.close();

			mUpload = mManager.upload(getBucketForPhotoType(photoType), imageKey, inputStream, metadata);

			mUpload.addProgressListener(new ProgressListener() {
				public void progressChanged(ProgressEvent progressEvent) {
					if (mUpload == null) return;
					BusProvider.getInstance().post(new com.aircandi.events.ProgressEvent(mUpload.getProgress().getPercentTransferred()));
				}
			});

			mUpload.waitForCompletion();
			mManager.getAmazonS3Client().setObjectAcl(getBucketForPhotoType(photoType), imageKey, CannedAccessControlList.PublicRead);

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
	public void onCancelEvent(CancelEvent event) {
		if (mUpload != null
				&& (mUpload.getState() == Transfer.TransferState.InProgress
				|| mUpload.getState() == Transfer.TransferState.Waiting)) {
			mUpload.abort();
			Logger.v(this, "Image upload aborted");
		}
	}

	public static String getBucketForPhotoType(PhotoType photoType) {
		if (photoType == PhotoType.GENERAL) {
			return StringManager.getString(R.string.s3_bucket_images);
		}
		else if (photoType == PhotoType.USER) {
			return StringManager.getString(R.string.s3_bucket_users);
		}
		else if (photoType == PhotoType.THUMBNAIL) {
			return StringManager.getString(R.string.s3_bucket_thumbnails);
		}
		return StringManager.getString(R.string.s3_bucket_images);
	}
}
