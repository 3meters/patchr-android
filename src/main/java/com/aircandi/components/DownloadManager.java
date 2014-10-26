package com.aircandi.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.Utilities;
import com.crashlytics.android.Crashlytics;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Listener;

@SuppressWarnings("ucd")
public class DownloadManager {

	private static Picasso instance = null;
	public static LruCache mPicassoMemoryCache;
	private static final Paint DEBUG_PAINT = new Paint();

	public static Picasso getInstance() {
		return instance;
	}

	public static Picasso with(Context context) {

		if (instance == null) {

			/* Picasso uses 15% default but we stretch it to ~25% */
			Integer cacheSize = Utilities.calculateMemoryCacheSize(Patchr.applicationContext);
			mPicassoMemoryCache = new LruCache(cacheSize);

			/* So we can sniff failures */
			Listener listener = new Listener() {
				@Override
				public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
					/*
					 * Getting 404 Not Found: if image not found.
					 * Getting 400 Bad Request: if invalid hostname.
					 * Thrown by OkHttpDownloader.load() as ResponseException.
					 * Also catching MalformedURLException.
					 */
					Reporting.logMessage("Image load failed: " + (uri != null ? uri.toString(): "No uri"));
					Reporting.logException(e);
					Logger.w(instance, "Image load failed: " + e.getClass().getSimpleName());
					Logger.w(instance, "Image load failed: " + e.getMessage());
					Logger.w(instance, "Image load failed: " + (uri != null ? uri.toString(): "No uri"));
				}
			};

			/* Automatically uses okhttp if library is included */
			instance = new Picasso.Builder(Patchr.applicationContext)
					.listener(listener)
					.memoryCache(mPicassoMemoryCache)
					.build();
		}
		return instance;
	}

	public static Bitmap checkDebug(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {

		Bitmap mutableBitmap = bitmap;
		if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_image_debug), false)
				&& Patchr.getInstance().getCurrentUser() != null && Type.isTrue(Patchr.getInstance().getCurrentUser().developer)) {
			if (!mutableBitmap.isMutable()) {
				mutableBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
			}
			Canvas canvas = new Canvas(mutableBitmap);
			DownloadManager.drawDebugIndicator(canvas, loadedFrom);
		}
		return mutableBitmap;
	}

	public static void drawDebugIndicator(Canvas canvas, Picasso.LoadedFrom loadedFrom) {

		float density = Patchr.applicationContext.getResources().getDisplayMetrics().density;
		DEBUG_PAINT.setColor(Colors.getColor(R.color.white));
		Path path = getTrianglePath(new Point(0, 0), (int) (16 * density));
		canvas.drawPath(path, DEBUG_PAINT);

		int color = Colors.getColor(R.color.holo_green_light);
		if (loadedFrom == Picasso.LoadedFrom.DISK) {
			color = Colors.getColor(R.color.holo_orange_light);
		}
		else if (loadedFrom == Picasso.LoadedFrom.NETWORK) {
			color = Colors.getColor(R.color.holo_red_light);
		}

		DEBUG_PAINT.setColor(color);
		path = getTrianglePath(new Point(0, 0), (int) (15 * density));
		canvas.drawPath(path, DEBUG_PAINT);
	}

	private static Path getTrianglePath(Point p1, int width) {
		Point p2 = new Point(p1.x + width, p1.y);
		Point p3 = new Point(p1.x, p1.y + width);

		Path path = new Path();
		path.moveTo(p1.x, p1.y);
		path.lineTo(p2.x, p2.y);
		path.lineTo(p3.x, p3.y);

		return path;
	}
}
