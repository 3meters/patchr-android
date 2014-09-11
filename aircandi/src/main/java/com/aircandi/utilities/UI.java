package com.aircandi.utilities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.Logger;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirImageView.SizeType;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.squareup.picasso.RequestCreator;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

@SuppressWarnings("ucd")
public class UI {

	/*--------------------------------------------------------------------------------------------
	 * Photos
	 *--------------------------------------------------------------------------------------------*/

	public static void drawPhoto(final AirImageView photoView, final Photo photo) {
	    /*
	     * There are only a few places that don't use this code to handle images:
		 * - Notification icons - can't use AirImageView
		 * - Actionbar icons - can't use AirImageView (shortcutpicker, placeform)
		 * - Photo detail - can't use AirImageView, using ImageViewTouch
		 */

		photoView.getImageView().setImageDrawable(null);
		photoView.setPhoto(photo);
		aircandi(photoView, photo);
		/*
		 * Special color treatment if enabled.
		 */
		if (photo.colorize != null && photo.colorize) {
			if (photo.color != null) {
				photoView.getImageView().setColorFilter(photo.color, PorterDuff.Mode.SRC_ATOP);
				photoView.getImageView().setBackgroundResource(0);
				if (photoView.findViewById(R.id.color_layer) != null) {
					(photoView.findViewById(R.id.color_layer)).setBackgroundResource(0);
					(photoView.findViewById(R.id.color_layer)).setVisibility(View.GONE);
					(photoView.findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
				}
			}
			else {
				final int color = Place.getCategoryColor(photo.colorizeKey);
				photoView.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

				Integer colorResId = Place.getCategoryColorResId(photo.colorizeKey);
				if (photoView.findViewById(R.id.color_layer) != null) {
					(photoView.findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
					(photoView.findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
					(photoView.findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
				}
				else {
					photoView.getImageView().setBackgroundResource(colorResId);
				}
			}
		}
		else {
			photoView.getImageView().clearColorFilter();
			photoView.getImageView().setBackgroundResource(0);
			if (photoView.findViewById(R.id.color_layer) != null) {
				(photoView.findViewById(R.id.color_layer)).setBackgroundResource(0);
				(photoView.findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(photoView.findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
			}
		}
	}

	public static void aircandi(final AirImageView photoView, final Photo photo) {

		photo.setProxy(false);
		if (photoView.getSizeType() != SizeType.FULLSIZE) {
			photo.setProxy(true, photoView.getSizeType());
		}
		else {
			/*
			 * We even cap fullsize if the device has minimal memory.
			 */
			if (Aircandi.memoryClass < 48) {
				Logger.i(UI.class, "Screen pixels: "
						+ getScreenWidthRawPixels(Aircandi.applicationContext)
						+ " x " + getScreenHeightRawPixels(Aircandi.applicationContext));
				photo.setProxy(true, SizeType.FULLSIZE_CAPPED);
			}
		}

		String imageUri = photo.getUriWrapped();

		if (Photo.isDrawable(imageUri)) {
			Integer drawableId = Photo.getResourceIdFromUri(photoView.getContext(), imageUri);
			if (drawableId != null) {
				DownloadManager.with(Aircandi.applicationContext)
				               .load(drawableId)
				               .placeholder(null)
						.resize(photoView.getSizeHint(), photoView.getSizeHint())    // Memory size
						.into(photoView);
			}
		}
		else {
			RequestCreator request = DownloadManager.with(Aircandi.applicationContext)
			                                        .load(imageUri)
			                                        .config(Config.RGB_565)
			                                        .placeholder(null);

			if (photoView.getCenterCrop()) {
				request.centerCrop();
				request.resize(photoView.getSizeHint(), photoView.getSizeHint());
			}

			request.into(photoView);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Utilities
	 *--------------------------------------------------------------------------------------------*/

	public static int getRawPixelsForDisplayPixels(Float displayPixels) {
		final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		final int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, displayPixels, metrics);
		return pixels;
	}

	public static int getRawPixelsForScaledPixels(Float scaledPixels) {
		final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		final int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, scaledPixels, metrics);
		return pixels;
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

	public static Drawable colorDrawable(Integer resId, Mode mode) {
		Drawable drawable = Aircandi.applicationContext.getResources().getDrawable(resId);
		return colorDrawable(drawable, mode);
	}

	public static Drawable colorDrawable(Drawable drawable, Mode mode) {
		drawable.setColorFilter(Colors.getColor(R.color.brand_primary), mode);
		return drawable;
	}

	public static Drawable colorDrawable(Drawable drawable, Integer colorResId, Mode mode) {
		drawable.setColorFilter(Colors.getColor(colorResId), mode);
		return drawable;
	}

	public static Drawable getDrawableForAttribute(Context context, Integer attr) {
		TypedValue a = new TypedValue();
		context.getTheme().resolveAttribute(attr, a, true);
		return Aircandi.applicationContext.getResources().getDrawable(a.resourceId);
	}

	public static Integer getResIdForAttribute(Context context, Integer attr) {
		TypedValue a = new TypedValue();
		context.getTheme().resolveAttribute(attr, a, true);
		return a.resourceId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Display
	 *--------------------------------------------------------------------------------------------*/

	public static void showToastNotification(final String message, final int duration) {
		showToastNotification(message, duration, 0);
	}

	public static void showToastNotification(final String message, final int duration, final int gravity) {
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				final CharSequence text = message;
				final Toast toast = Toast.makeText(Aircandi.applicationContext, text, duration);
				if (gravity != 0) {
					toast.setGravity(gravity, 0, 0);
				}
				toast.show();
			}
		});
	}

	public static void showImageInImageView(final Bitmap bitmap, final ImageView imageView, final boolean animate, final Animation animation) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				imageView.setImageBitmap(bitmap);
				if (animate) {
					animation.setFillEnabled(true);
					animation.setFillAfter(true);
					animation.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationStart(Animation animation) {
						}

						@Override
						public void onAnimationEnd(Animation animation) {
							imageView.clearAnimation();
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}
					});
					imageView.startAnimation(animation);
				}
				imageView.postInvalidate();
			}
		});

	}

	public static void animateView(final View view, final Boolean visible, final Boolean clickable, final Integer duration) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (visible && ViewHelper.getAlpha(view) == 0) {
					if (clickable) view.setClickable(true);
					ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
					anim.setDuration((duration == null) ? AnimationManager.DURATION_MEDIUM : duration);
					anim.start();
				}
				else if (!visible && ViewHelper.getAlpha(view) == 1) {
					if (clickable) view.setClickable(false);
					ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
					anim.setDuration((duration == null) ? AnimationManager.DURATION_MEDIUM : duration);
					anim.start();
				}
			}
		});
	}

	public static void clearImageInImageView(final ImageView imageView, final boolean animate, final Animation animation) {
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (animate) {
					animation.setFillEnabled(true);
					animation.setFillAfter(true);
					imageView.startAnimation(animation);
				}
				else {
					imageView.setAnimation(null);
					imageView.setImageBitmap(null);
				}
			}
		});
	}

	public static void showDrawableInImageView(final Drawable drawable, final ImageView imageView, final boolean animate, final Animation animation) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (imageView != null) {
					imageView.setImageDrawable(drawable);
					imageView.clearAnimation();
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

	public static void showDrawableInImageView(final Drawable drawable, final ImageViewTouch imageView, final float minZoom, final float maxZoom,
	                                           final boolean animate, final Animation animation) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (imageView != null) {
					imageView.setImageDrawable(drawable, null, minZoom, maxZoom);
					if (animate) {
						animation.setFillEnabled(true);
						animation.setFillAfter(true);
						imageView.startAnimation(animation);
					}
					imageView.postInvalidate();
				}
			}
		});
	}

	public static void setImageBitmapWithFade(final ImageView imageView, final Bitmap bitmap) {
		Resources resources = imageView.getResources();
		BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
		setImageDrawableWithFade(imageView, bitmapDrawable);
	}

	public static void setImageDrawableWithFade(final ImageView imageView, final Drawable drawable) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				Drawable currentDrawable = imageView.getDrawable();
				if (currentDrawable != null) {
					Drawable[] arrayDrawable = new Drawable[2];
					arrayDrawable[0] = currentDrawable;
					arrayDrawable[1] = drawable;
					TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
					transitionDrawable.setCrossFadeEnabled(true);
					imageView.setImageDrawable(transitionDrawable);
					transitionDrawable.startTransition(2000);
				}
				else {
					imageView.setImageDrawable(drawable);
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

	//	public static void hideSoftInput(Context context, IBinder windowToken) {
	//		InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
	//		inputManager.hideSoftInputFromWindow(windowToken, 0);
	//	}

	public static void showSoftInput(View view) {
		InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
	}

	public static int showScreenSize() {
		int screenSize = Aircandi.applicationContext.getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK;

		switch (screenSize) {
			case Configuration.SCREENLAYOUT_SIZE_XLARGE:
				showToastNotification("Extra large screen", Toast.LENGTH_LONG);
				break;
			case Configuration.SCREENLAYOUT_SIZE_LARGE:
				showToastNotification("Large screen", Toast.LENGTH_LONG);
				break;
			case Configuration.SCREENLAYOUT_SIZE_NORMAL:
				showToastNotification("Normal screen", Toast.LENGTH_LONG);
				break;
			case Configuration.SCREENLAYOUT_SIZE_SMALL:
				showToastNotification("Small screen", Toast.LENGTH_LONG);
				break;
			default:
				showToastNotification("Screen size is neither xlarge, large, normal or small", Toast.LENGTH_LONG);
		}
		return screenSize;
	}

}