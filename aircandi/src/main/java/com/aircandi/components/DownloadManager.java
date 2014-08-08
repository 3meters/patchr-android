package com.aircandi.components;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import android.content.Context;
import android.net.Uri;

import com.aircandi.Aircandi;
import com.aircandi.utilities.Utilities;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
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
		/*
		 * Mimicking Picasso's new OkHttpLoader(context), but with our custom OkHttpClient
		 */
		if (instance == null) {

			/*
			 * Default read timeout = 20s
			 * Default connect timeout = 15s
			 */
			OkHttpClient client = createClient();
			try {
				client.setCache(createResponseCache(context));
			}
			catch (IOException ignore) {}

			/* Picasso uses 15% default but we stretch it to ~25% */
			Integer cacheSize = Utilities.calculateMemoryCacheSize(Aircandi.applicationContext);

			mPicassoMemoryCache = new LruCache(cacheSize);
			Listener listener = new Listener() {

				@Override
				public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
					Logger.w(instance, "Image load failed: " + e.getClass().getSimpleName());
					Logger.w(instance, "Image load failed: " + e.getMessage());
					Logger.w(instance, "Image load failed: " + uri.toString());
				}
			};

			instance = new Picasso.Builder(Aircandi.applicationContext)
					.downloader(new OkHttpDownloader(client))
					.listener(listener)
					.memoryCache(mPicassoMemoryCache)
					.build();
		}
		return instance;
	}

	private static OkHttpClient createClient() {

		OkHttpClient client = new OkHttpClient();
		/*
		 * Working around the libssl crash: https://github.com/square/okhttp/issues/184
		 */
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, null, null);
		}
		catch (GeneralSecurityException e) {
			throw new AssertionError(); // The system has no TLS. Just give up.
		}

		client.setSslSocketFactory(sslContext.getSocketFactory());
		return client;
	}

	private static File createDefaultCacheDir(Context context) {
		/*
		 * Using reflection to call into picasso utility method.
		 */
		try {
			final Class<?> clazz = Class.forName("com.squareup.picasso.Utils");
			final Method method = clazz.getDeclaredMethod("createDefaultCacheDir", Context.class);
			method.setAccessible(true);
			return (File) method.invoke(null, context);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
	}

	private static long calculateDiskCacheSize(File dir) {
		/*
		 * Using reflection to call into picasso utility method.
		 */
		try {
			final Class<?> clazz = Class.forName("com.squareup.picasso.Utils");
			final Method method = clazz.getDeclaredMethod("calculateDiskCacheSize", File.class);
			method.setAccessible(true);
			return (Long) method.invoke(null, dir);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
	}

	private static Cache createResponseCache(Context context) throws IOException {
		File cacheDir = createDefaultCacheDir(context);
		long maxSize = calculateDiskCacheSize(cacheDir);
		return new Cache(cacheDir, maxSize);
	}
}
