package com.aircandi.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.graphics.Bitmap;
import android.util.Log;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.objects.Photo.PhotoType;
import com.aircandi.service.ServiceResponse;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3 {

	static {
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
		try {
			@SuppressWarnings("unused")
			final XMLReader reader = XMLReaderFactory.createXMLReader(); // $codepro.audit.disable variableUsage
		}
		catch (SAXException e) {
			Log.e("SAXException", e.getMessage());
		}
	}

	private static class AmazonS3Holder {
		public static final AmazonS3	instance	= new AmazonS3Client(Aircandi.awsCredentials);
	}

	public static AmazonS3 getInstance() {
		return AmazonS3Holder.instance;
	}

	public static ServiceResponse putImage(String imageKey, Bitmap bitmap, Integer quality, PhotoType photoType) {

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
		final byte[] bitmapBytes = outputStream.toByteArray();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
		final ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bitmapBytes.length);
		metadata.setContentType("image/jpeg");

		try {
			S3.getInstance().putObject(getBucketForPhotoType(photoType), imageKey, inputStream, metadata);
			S3.getInstance().setObjectAcl(getBucketForPhotoType(photoType), imageKey, CannedAccessControlList.PublicRead);
			return new ServiceResponse();
		}
		catch (final AmazonServiceException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
		catch (final AmazonClientException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
		finally {
			try {
				outputStream.close();
				inputStream.close();
			}
			catch (IOException exception) {
				return new ServiceResponse(ResponseCode.FAILED, null, exception);
			}
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
