package com.aircandi.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;

import com.aircandi.Patchr;
import com.aircandi.utilities.Reporting;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Listener;

import java.io.IOException;

/*
 * Bitmap memory management
 *
 * Uploads:
 *
 * Bitmaps are sized so that max dimension = 1280 pixels (4.9MB max without alpha channel). If OOM error
 * then we try to create the bitmap again at 640 pixels max (1.2 max).
 */

@SuppressWarnings("ucd")
public class DownloadManager {

	private static       Picasso instance                = null;
	private static final Paint   DEBUG_PAINT             = new Paint();
	private static final String  logBitmapTarget         = "Bitmap target:  context = %1$s, height = %2$s, width = %3$s, size = %4$s";
	private static final String  logBitmapCreated        = "Bitmap created: context = %1$s, height = %2$s, width = %3$s, size = %4$s, config = %5$s";
	public static final  String  PHOTO_GROUP_TAG_DEFAULT = "photo";

	public static Picasso getInstance() {
		return instance;
	}

	public static Picasso with(Context context) {

		if (instance == null) {

			/* So we can sniff failures */
			Listener listener = new Listener() {

				@Override
				public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
					/*
					 * 404 Not Found: if image not found.
					 * 400 Bad Request: if invalid hostname.
					 * 403 Forbidden: most likely image requires authentication.
					 * 502 Bad Gateway: means somethings wrong with web server or the service provider. We might be
					 *     using the Google resizer and it got an invalid response from the image providing server. The
					 *     Google resizing service throws it back as an invalid response.
					 * 504 Unsatisfiable request (only-if-cached). This is returned by Picasso when the network
					 * isn't available and the image isn't cached.
					 *
					 * Thrown by OkHttpDownloader.load() as ResponseException.
					 * Also catching MalformedURLException.
					 */
					boolean expected = (e.getMessage().contains("404")
							|| e.getMessage().contains("400")
							|| e.getMessage().contains("403")
							|| e.getMessage().contains("504")
							|| e.getMessage().contains("502"));
					if (!expected) {
						String message = "Picasso image load failed with unexpected code: " + (uri != null ? uri.toString() : "No uri");
						Reporting.logMessage(message);
						Reporting.logException(new IOException(message, e));
					}
					Logger.w(instance, "Image load failed: " + e.getClass().getSimpleName());
					Logger.w(instance, "Image load failed: " + e.getMessage());
					Logger.w(instance, "Image load failed: " + (uri != null ? uri.toString() : "No uri"));
				}
			};

			/* Automatically uses okhttp if library is included */
			instance = new Picasso.Builder(Patchr.applicationContext)
					.listener(listener)
					.build();
		}
		return instance;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private static int getBytesPerPixel(Bitmap.Config config) {
		/*
		 * A helper function to return the byte usage per pixel of a bitmap based on its configuration.
		 */
		if (config == Bitmap.Config.ARGB_8888) {
			return 4;
		}
		else if (config == Bitmap.Config.RGB_565) {
			return 2;
		}
		else if (config == Bitmap.Config.ARGB_4444) {
			return 2;
		}
		else if (config == Bitmap.Config.ALPHA_8) {
			return 1;
		}
		return 1;
	}
}
