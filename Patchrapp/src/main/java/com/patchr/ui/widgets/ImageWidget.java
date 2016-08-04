package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.PhotoCategory;
import com.patchr.ui.components.CircleTransform;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

@SuppressWarnings("ucd")
public class ImageWidget extends FrameLayout {

	public  RoundedImageView imageView;
	private AirProgressBar   progressBar;
	private TextView         nameView;

	private String uri;
	private String uriBound;

	public PhotoCategory category;
	public float         aspectRatio;
	public ScaleType     scaleType;
	public Bitmap.Config bitmapConfig;  // Used by picasso
	public boolean       showBusy;
	public String        shape;    // auto, square, round, rounded
	public float         radius;

	public ImageWidget(Context context) {
		this(context, null);
	}

	public ImageWidget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		scaleType = ScaleType.CENTER_CROP;
		shape = "auto";

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ImageWidget, defStyle, 0);

		bitmapConfig = Bitmap.Config.values()[ta.getInteger(R.styleable.ImageWidget_config, Bitmap.Config.RGB_565.ordinal())];
		category = PhotoCategory.values()[ta.getInteger(R.styleable.ImageWidget_category, PhotoCategory.THUMBNAIL.ordinal())];
		showBusy = ta.getBoolean(R.styleable.ImageWidget_showBusy, true);
		aspectRatio = ta.getFloat(R.styleable.ImageWidget_aspectRatio, 0f);
		radius = ta.getInteger(R.styleable.ImageWidget_radius, 0);

		if (ta.hasValue(R.styleable.ImageWidget_shape)) {
			shape = ta.getString(R.styleable.ImageWidget_shape);
		}

		ta.recycle();

		if (!isInEditMode()) {
			final int scaleTypeValue = attrs.getAttributeIntValue(androidNamespace, "scaleType", ScaleType.CENTER_CROP.ordinal());
			if (scaleTypeValue >= 0) {
				scaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (aspectRatio != 0) {

			int w = MeasureSpec.getSize(widthMeasureSpec);
			int h = (int) ((float) w * aspectRatio);
			setMeasuredDimension(w, h);

			/* We have to enforce the sizing on the child imageview */
			int widthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
			if (imageView != null) {
				imageView.measure(widthSpec, heightSpec);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		/* Placeholder */
		if (shape.equals("round")) {
			setBackgroundResource(UI.getResIdForAttribute(getContext(), R.attr.backgroundRoundPlaceholder));
		}
		else if (shape.equals("auto")) {
			setBackgroundResource(UI.getResIdForAttribute(getContext(), R.attr.backgroundPlaceholder));
		}

		/* Image - subclass could have provide it instead */
		if (imageView == null) {
			imageView = new RoundedImageView(getContext());
			imageView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			imageView.setScaleType(ScaleType.CENTER_CROP);
			imageView.setCornerRadius(UI.getRawPixelsForDisplayPixels(radius));
			addView(imageView);
		}

		/* Text overlay */
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		nameView = new TextView(getContext());
		nameView.setLayoutParams(params);
		nameView.setVisibility(GONE);
		if (!isInEditMode()) {
			nameView.setTextColor(Colors.getColor(R.color.white));
		}
		nameView.setGravity(Gravity.CENTER);
		addView(nameView);

		/* Activity indicator - last added is in front */
		if (showBusy) {
			params = new FrameLayout.LayoutParams(72, 72);
			params.gravity = Gravity.CENTER;
			progressBar = new AirProgressBar(getContext(), null, android.R.attr.progressBarStyle);
			progressBar.setLayoutParams(params);
			progressBar.hide();
			addView(progressBar);
		}

		if (isInEditMode()) {
			Drawable dummy = ContextCompat.getDrawable(getContext(), R.drawable.img_dummy);
			imageView.setImageDrawable(dummy);
		}
	}

	public void setImageWithEntity(RealmEntity entity, Callback callback) {
		if (entity.getPhoto() != null) {
			setImageWithPhoto(entity.getPhoto(), null, callback);
		}
		else {
			setImageWithText(entity.name, entity.schema.equals(Constants.SCHEMA_ENTITY_USER));
		}
	}

	public void setImageWithPhoto(Photo photo, String name, Callback callback) {
		if (photo != null) {
			setImageWithPhoto(photo, callback);
		}
		else {
			setImageWithText(name, true);
		}
	}

	private void setImageWithPhoto(Photo photo, Callback callback) {

		/* Optimize if we already have the image */
		if (photo.isUri() && uriBound != null && imageView.getDrawable() != null) {
			String uri = photo.uriNative();
			if (uri.equals(uriBound)) return;
		}

		if (getBackground() != null) {
			getBackground().clearColorFilter();
		}
		imageView.setImageDrawable(null);
		imageView.setVisibility(VISIBLE);
		nameView.setVisibility(GONE);

		if (!showBusy) {
			showLoading(false);
		}

		switch (shape) {
			case "round":
				loadImageView(photo, new CircleTransform(), callback);
				break;
			case "rounded":
				loadImageView(photo, null, callback);
				break;
			default:
				loadImageView(photo, null, callback);
				break;
		}
	}

	public void setImageWithText(String name, Boolean showText) {

		if (getBackground() != null) {
			getBackground().clearColorFilter();
		}
		imageView.setImageDrawable(null);
		imageView.setVisibility(GONE);
		nameView.setText(null);

		if (!TextUtils.isEmpty(name) && getBackground() != null) {
			long seed = Utils.numberFromName(name);
			Integer color = Utils.randomColor(seed);
			getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

			if (showText) {
				String initials = Utils.initialsFromName(name);
				nameView.setText(initials);
				nameView.setVisibility(VISIBLE);
			}
		}
	}

	public void setImageWithResource(Integer resId, Transformation transform) {
		RequestCreator creator = Picasso
			.with(getContext())
			.load(resId);

		if (transform != null) {
			creator.transform(transform);
		}

		creator.into(imageView);
	}

	public void showLoading(final Boolean visible) {

		Patchr.mainThread.post(() -> {
			if (progressBar != null) {
				if (visible) {
					progressBar.show();
				}
				else {
					progressBar.hide();
				}
			}
		});
	}

	public void setAspectRatio(Float aspectRatio) {
		this.aspectRatio = aspectRatio;
		requestLayout();
	}

	private void loadImageView(@NonNull final Photo photo, final Transformation transform, Callback callback) {
		/*
		 * This is the only patch in the code that turns on proxy handling.
		 * SizeHint on AirImageView is used when target size is fixed and known before view layout.
		 * Fit on photo is used when target size is desired and known only after view layout.
		 */
		if (photo.isResource()) {

			Integer drawableId = photo.getResId();
			if (drawableId != null) {
				RequestCreator creator = Picasso
					.with(Patchr.applicationContext)
					.load(drawableId)
					.centerCrop()   // Needed so resize() keeps aspect ratio
					.resize(getWidth(), getHeight())
					.config(bitmapConfig);

				if (transform != null) {
					creator.transform(transform);
					creator.fit();
				}
				else {
					creator.centerCrop(); // Needed so resize() keeps aspect ratio
					creator.resize(getWidth(), getHeight());
				}
				creator.into(imageView);
			}
		}
		else if (photo.isFile()) {

			RequestCreator creator = Picasso
				.with(Patchr.applicationContext)
				.load(photo.uriNative())
				.config(bitmapConfig);

			if (transform != null) {
				creator.transform(transform);
				creator.fit();
			}
			else {
				creator.centerCrop(); // Needed so resize() keeps aspect ratio
				creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
			}
			creator.into(imageView, callback);
		}
		else {  /* url */

			final String uri = UI.uri(photo.prefix, photo.source, category);
			RequestCreator creator = Picasso
				.with(Patchr.applicationContext)
				.load(uri)
				.config(bitmapConfig);

			if (transform != null) {
				//				creator.fit();
				creator.transform(transform);
			}
			//			else {
			//				creator.centerCrop(); // Needed so resize() keeps aspect ratio
			//				creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
			//			}

			creator.networkPolicy(NetworkPolicy.OFFLINE);
			creator.into(imageView, new Callback() {

				@Override public void onSuccess() {
					if (callback != null) {
						callback.onSuccess();
					}
				}

				@Override public void onError() {
					RequestCreator creator = Picasso
						.with(Patchr.applicationContext)
						.load(uri)
						.config(bitmapConfig);

					if (transform != null) {
						creator.transform(transform);
						//						creator.fit();
					}
					//					else {
					//						creator.centerCrop(); // Needed so resize() keeps aspect ratio
					//						creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
					//					}

					creator.into(imageView, new Callback() {
						@Override public void onSuccess() {
							if (callback != null) {
								callback.onSuccess();
							}
						}

						@Override public void onError() {
							if (callback != null) {
								callback.onError();
							}
						}
					});
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private static final String      androidNamespace = "http://schemas.android.com/apk/res/android";
	private static final ScaleType[] sScaleTypeArray  = {
		ScaleType.MATRIX,
		ScaleType.FIT_XY,
		ScaleType.FIT_START,
		ScaleType.FIT_CENTER,
		ScaleType.FIT_END,
		ScaleType.CENTER,
		ScaleType.CENTER_CROP,
		ScaleType.CENTER_INSIDE
	};
}