package com.aircandi.utilities;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DownloadManager;
import com.aircandi.objects.Photo;
import com.aircandi.ui.widgets.AirImageView;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

@SuppressWarnings("ucd")
public class UI {

	/*--------------------------------------------------------------------------------------------
	 * Photos
	 *--------------------------------------------------------------------------------------------*/

	public static void drawPhoto(final AirImageView photoView, final Photo photo) {
		drawPhoto(photoView, photo, null);
	}

	public static void drawPhoto(final AirImageView photoView, final Photo photo, final Transformation transform) {
	    /*
	     * There are only a few places that don't use this code to display images:
		 * - Notification icons - can't use AirImageView
		 * - Actionbar icons - can't use AirImageView (shortcutpicker, placeform)
		 */
		photoView.getImageView().setImageDrawable(null);
		photoView.setPhoto(photo);

		if (photoView.getFitType() == AirImageView.FitType.NONE
				|| photoView.getFitType() == AirImageView.FitType.FIXED) {
			loadView(photoView, photo, transform);
		}
		else if (photoView.getFitType() == AirImageView.FitType.AUTO) {
			if (photoView.getImageView().getWidth() != 0) {
				loadView(photoView, photo, transform);
			}
			else {
				photoView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

					@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
					@SuppressWarnings("deprecation")
					@Override
					public void onGlobalLayout() {
						loadView(photoView, photo, transform);
						if (Constants.SUPPORTS_JELLY_BEAN) {
							photoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						}
						else {
							photoView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						}
					}
				});
			}
		}
	}

	public static void loadView(final AirImageView photoView, final Photo photo, final Transformation transform) {
		/*
		 * This is the only patch in the code that turns on proxy handling.
		 * SizeHint on AirImageView is used when target size is fixed and known before view layout.
		 * Fit on photo is used when target size is desired and known only after view layout.
		 */
		int width = photoView.getImageView().getWidth();
		int height = photoView.getImageView().getHeight();

		if (photoView.getFitType() == AirImageView.FitType.NONE) {
			width = Constants.IMAGE_DIMENSION_MAX;
			height = Constants.IMAGE_DIMENSION_MAX;
		}
		else if (photoView.getFitType() == AirImageView.FitType.FIXED) {
			width = photoView.getSizeHint();
			height = photoView.getSizeHint();
		}

		if (photo.source.equals(Photo.PhotoSource.resource)) {

			Integer drawableId = photo.getResId();
			if (drawableId != null) {
				RequestCreator creator = DownloadManager
						.with(Patchr.applicationContext)
						.load(drawableId)
						.centerCrop()   // Needed so resize() keeps aspect ratio
						.resize(width, height)
						.tag(photoView.getGroupTag() != null ? photoView.getGroupTag() : DownloadManager.GROUP_TAG_DEFAULT)
						.config(photoView.getConfig() != null ? photoView.getConfig() : Config.RGB_565);
				if (transform != null) {
					creator.transform(transform);
				}
				creator.into(photoView);
			}
		}
		else if (photo.source.equals(Photo.PhotoSource.file)) {
			RequestCreator creator = DownloadManager
					.with(Patchr.applicationContext)
					.load(photo.getUri())
					.centerCrop()   // Needed so resize() keeps aspect ratio
					.resize(width, height)
					.tag(photoView.getGroupTag() != null ? photoView.getGroupTag() : DownloadManager.GROUP_TAG_DEFAULT)
					.config(photoView.getConfig() != null ? photoView.getConfig() : Config.RGB_565);
			if (transform != null) {
				creator.transform(transform);
			}
			creator.into(photoView);
		}
		else {

			photo.setProxy(true, height, width);
			RequestCreator creator = DownloadManager
					.with(Patchr.applicationContext)
					.load(photo.getUriWrapped())
					.placeholder(UI.getResIdForAttribute(photoView.getContext(), R.attr.backgroundPlaceholder))
					.tag(photoView.getGroupTag() != null ? photoView.getGroupTag() : DownloadManager.GROUP_TAG_DEFAULT)
					.config(photoView.getConfig() != null ? photoView.getConfig() : Config.RGB_565);
			if (transform != null) {
				creator.transform(transform);
			}
			creator.into(photoView);
		}

		/* Final step */
		photoView.getImageView().setBackgroundResource(0);
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

	public static float getScreenWidthDisplayPixels(Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.widthPixels / metrics.density;
	}

	public static float getScreenWidthRawPixels(Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.widthPixels;
	}

	public static float getScreenHeightRawPixels(Context context) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.heightPixels;
	}

	public static int getImageMemorySize(int height, int width, boolean hasAlpha) {
		return height * width * (hasAlpha ? 4 : 3);
	}

	public static Bitmap ensureBitmapScaleForS3(Bitmap bitmap) {
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

	public static Drawable getDrawableForAttribute(Context context, Integer attr) {
		TypedValue a = new TypedValue();
		context.getTheme().resolveAttribute(attr, a, true);
		return Patchr.applicationContext.getResources().getDrawable(a.resourceId);
	}

	public static Integer getResIdForAttribute(Context context, Integer attr) {
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
					imageView.setImageDrawable(drawable);
					imageView.invalidate();

					if (animate) {
						ObjectAnimator anim = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f);
						anim.setDuration(AnimationManager.DURATION_MEDIUM);
						anim.start();
					}
				}
			}
		});
	}

	public static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	public static void setEnabled(View view, boolean enabled) {
		view.setEnabled(enabled);
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				setEnabled(group.getChildAt(i), enabled);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Input
   	 *--------------------------------------------------------------------------------------------*/

	public static void hideSoftInput(View view) {
		InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static void showSoftInput(View view) {
		InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
	}
}