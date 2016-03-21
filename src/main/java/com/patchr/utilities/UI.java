package com.patchr.utilities;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.FontManager;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;

@SuppressWarnings("ucd")
public class UI {

	/*--------------------------------------------------------------------------------------------
	 * Photos
	 *--------------------------------------------------------------------------------------------*/

	public static String uri(String prefix, String source, PhotoCategory category) {
		/*
		 * If category is null then will return a straight conversion of prefix.
		 */
		String path = prefix;

		if (category != null) {

			Integer quality = 75;
			if (Constants.PIXEL_SCALE >= 3) {
				quality = 25;
			}
			else if (Constants.PIXEL_SCALE >= 2) {
				quality = 50;
			}

			if (source.equals(Photo.PhotoSource.aircandi_images)) {
				Integer width = (category == PhotoCategory.STANDARD) ? 400 : 100;
				if (category == PhotoCategory.NONE) {
					path = "http://aircandi-images.s3.amazonaws.com/" + prefix;
				}
				else if (category == PhotoCategory.PROFILE) {
					path = "https://3meters-images.imgix.net/" + prefix
							+ "?w=" + String.valueOf(width)
							+ "&dpr=" + String.valueOf(Constants.PIXEL_SCALE)
							+ "&q=" + String.valueOf(quality)
							+ "&h=" + String.valueOf(width)
							+ "&fit=min&trim=auto";
				}
				else {
					path = "https://3meters-images.imgix.net/" + prefix
							+ "?w=" + String.valueOf(width)
							+ "&dpr=" + String.valueOf(Constants.PIXEL_SCALE)
							+ "&q=" + String.valueOf(quality);
				}
			}
			else if (source.equals(Photo.PhotoSource.google)) {
				Integer width = Constants.IMAGE_DIMENSION_MAX * Constants.PIXEL_SCALE;
				if (prefix.contains("?")) {
					path = prefix + "&maxwidth=" + String.valueOf(width);
				}
				else {
					path = prefix + "?maxwidth=" + String.valueOf(width);
				}
			}
			else { /* source == file */
				path = prefix;
			}
		}

		return path;
	}

	/*--------------------------------------------------------------------------------------------
	 * Utilities
	 *--------------------------------------------------------------------------------------------*/

	public static int getRawPixelsForDisplayPixels(Float displayPixels) {
		final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		final int rawPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, displayPixels, metrics);
		return rawPixels;
	}

	public static int getRawPixelsForScaledPixels(Float scaledPixels) {
		final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		final int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, scaledPixels, metrics);
		return pixels;
	}

	public static int getDisplayPixelsForRawPixels(Float rawPixels) {
		final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		float displayPixels = rawPixels / (metrics.densityDpi / 160f);
		return (int) displayPixels;
	}

	public static float getScreenWidthDisplayPixels(@NonNull Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.widthPixels / metrics.density;
	}

	public static float getScreenHeightDisplayPixels(@NonNull Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.heightPixels / metrics.density;
	}

	public static float getScreenWidthRawPixels(@NonNull Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.widthPixels;
	}

	public static float getScreenHeightRawPixels(@NonNull Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.heightPixels;
	}

	public static int getImageMemorySize(int height, int width, boolean hasAlpha) {
		return height * width * (hasAlpha ? 4 : 3);
	}

	public static Bitmap ensureBitmapScaleForS3(@NonNull Bitmap bitmap) {
		Bitmap bitmapScaled = bitmap;
		final Boolean scalingNeeded = (bitmap.getWidth() > Constants.IMAGE_DIMENSION_MAX && bitmap.getHeight() > Constants.IMAGE_DIMENSION_MAX);
		if (scalingNeeded) {

			final Matrix matrix = new Matrix();
			final float scalingRatio = Math.max((float) Constants.IMAGE_DIMENSION_MAX / (float) bitmap.getWidth(), (float) Constants.IMAGE_DIMENSION_MAX
					/ (float) bitmap.getHeight());
			matrix.postScale(scalingRatio, scalingRatio);
			/*
			 * Create a new bitmap from the original using the matrix to transform the result.
			 * Potential for OM condition because if the garbage collector is behind, we could
			 * have several large bitmaps in memory at the same time.
			 */
			bitmapScaled = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmapScaled;
	}

	public static Drawable getDrawableForAttribute(@NonNull Context context, Integer attr) {
		TypedValue a = new TypedValue();
		context.getTheme().resolveAttribute(attr, a, true);
		//noinspection deprecation
		return Patchr.applicationContext.getResources().getDrawable(a.resourceId);
	}

	public static Integer getResIdForAttribute(@NonNull Context context, Integer attr) {
		TypedValue a = new TypedValue();
		context.getTheme().resolveAttribute(attr, a, true);
		return a.resourceId;
	}

	public static Integer getDimension(Integer dimenResId) {
		return Patchr.applicationContext.getResources().getDimensionPixelSize(dimenResId);
	}

	/*--------------------------------------------------------------------------------------------
	 * Display
	 *--------------------------------------------------------------------------------------------*/

	public static void showToastNotification(final String message, final int duration) {
		showToastNotification(message, duration, 0);
	}

	public static void showToastNotification(final String message, final int duration, final int gravity) {
		Patchr.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				final CharSequence text = message;
				final Toast toast = Toast.makeText(Patchr.applicationContext, text, duration);
				toast.getView().setBackgroundResource(R.drawable.bg_toast);
				TextView view = (TextView) toast.getView().findViewById(android.R.id.message);
				view.setTextColor(Colors.getColor(R.color.black));
				view.setShadowLayer(0, 0, 0, 0);
				FontManager.getInstance().setTypefaceLight(view);
				if (gravity != 0) {
					toast.setGravity(gravity, 0, 0);
				}
				toast.show();
			}
		});
	}

	public static void showDrawableInImageView(final Drawable drawable, final ImageView imageView, final boolean animate) {
		/*
		 * Make sure this on the main thread
		 */
		Patchr.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (imageView != null) {
					if (animate) {
						ObjectAnimator anim = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f);
						anim.setDuration(300);
						anim.start();
					}
					imageView.setImageDrawable(drawable);
					//imageView.invalidate();
				}
			}
		});
	}

	public static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	public static void setTextView(View view, String text) {
		if (view != null) {
			((TextView) view).setText(text);
		}
	}

	public static void setEnabled(@NonNull View view, boolean enabled) {
		view.setEnabled(enabled);
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				setEnabled(group.getChildAt(i), enabled);
			}
		}
	}

	public static void setClickListener(View view, View.OnClickListener listener) {
		if (view != null) {
			view.setOnClickListener(listener);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Input
   	 *--------------------------------------------------------------------------------------------*/

	public static void hideSoftInput(@NonNull View view) {
		InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static void showSoftInput(@NonNull View view) {
		InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
	}
}