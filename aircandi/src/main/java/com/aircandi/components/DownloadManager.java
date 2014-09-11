package com.aircandi.components;

import android.content.Context;
import android.net.Uri;

import com.aircandi.Aircandi;
import com.aircandi.utilities.Utilities;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Listener;

@SuppressWarnings("ucd")
public class DownloadManager {

	private static Picasso instance = null;
	public static LruCache mPicassoMemoryCache;

	public static Picasso getInstance() {
		return instance;
	}

	public static Picasso with(Context context) {

		if (instance == null) {

			/* Picasso uses 15% default but we stretch it to ~25% */
			Integer cacheSize = Utilities.calculateMemoryCacheSize(Aircandi.applicationContext);
			mPicassoMemoryCache = new LruCache(cacheSize);

			/* So we can sniff failures */
			Listener listener = new Listener() {
				@Override
				public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
					Logger.w(instance, "Image load failed: " + e.getClass().getSimpleName());
					Logger.w(instance, "Image load failed: " + e.getMessage());
					Logger.w(instance, "Image load failed: " + uri.toString());
				}
			};

			/* Automatically uses okhttp if library is included */
			instance = new Picasso.Builder(Aircandi.applicationContext)
					.listener(listener)
					.memoryCache(mPicassoMemoryCache)
					.build();
		}
		return instance;
	}
}
