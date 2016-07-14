package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.PhotoCategory;
import com.patchr.ui.components.CircleTransform;
import com.patchr.ui.components.RoundedCornersTransformation;
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

	public  AppCompatImageView imageView;
	private AirProgressBar     progressBar;
	private TextView           nameView;

	private String uri;
	private String uriBound;

	public PhotoCategory category;
	public float         aspectRatio;
	public ScaleType     scaleType;
	public Bitmap.Config bitmapConfig;  // Used by picasso
	public boolean       showBusy;
	public String        shape;    // auto, square, round, rounded
	public Integer       radius;

	public ImageWidget(Context context) {
		this(context, null);
	}

	public ImageWidget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		this.scaleType = ScaleType.CENTER_CROP;
		this.shape = "auto";
		this.radius = 8;

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ImageWidget, defStyle, 0);

		this.bitmapConfig = Bitmap.Config.values()[ta.getInteger(R.styleable.ImageWidget_config, Bitmap.Config.RGB_565.ordinal())];
		this.category = PhotoCategory.values()[ta.getInteger(R.styleable.ImageWidget_category, PhotoCategory.THUMBNAIL.ordinal())];
		this.showBusy = ta.getBoolean(R.styleable.ImageWidget_showBusy, true);
		this.aspectRatio = ta.getFloat(R.styleable.ImageWidget_aspectRatio, 0f);
		this.radius = ta.getInteger(R.styleable.ImageWidget_radius, 0);

		if (ta.hasValue(R.styleable.ImageWidget_shape)) {
			this.shape = ta.getString(R.styleable.ImageWidget_shape);
		}

		ta.recycle();

		if (!isInEditMode()) {
			final int scaleTypeValue = attrs.getAttributeIntValue(androidNamespace, "scaleType", ScaleType.CENTER_CROP.ordinal());
			if (scaleTypeValue >= 0) {
				this.scaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (this.aspectRatio != 0) {

			int w = MeasureSpec.getSize(widthMeasureSpec);
			int h = (int) ((float) w * this.aspectRatio);
			setMeasuredDimension(w, h);

			/* We have to enforce the sizing on the child imageview */
			int widthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
			if (this.imageView != null) {
				this.imageView.measure(widthSpec, heightSpec);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		/* Placeholder */
		if (this.shape.equals("round")) {
			this.setBackgroundResource(UI.getResIdForAttribute(getContext(), R.attr.backgroundRoundPlaceholder));
		}
		else if (this.shape.equals("auto")) {
			this.setBackgroundResource(UI.getResIdForAttribute(getContext(), R.attr.backgroundPlaceholder));
		}

		/* Image - subclass could have provide it instead */
		if (this.imageView == null) {
			this.imageView = new AppCompatImageView(getContext());
			this.imageView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			this.imageView.setScaleType(this.scaleType);
			addView(this.imageView);
		}

		/* Text overlay */
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		this.nameView = new TextView(getContext());
		this.nameView.setLayoutParams(params);
		this.nameView.setVisibility(GONE);
		if (!isInEditMode()) {
			this.nameView.setTextColor(Colors.getColor(R.color.white));
		}
		this.nameView.setGravity(Gravity.CENTER);
		addView(this.nameView);

		/* Activity indicator - last added is in front */
		if (showBusy) {
			params = new FrameLayout.LayoutParams(72, 72);
			params.gravity = Gravity.CENTER;
			this.progressBar = new AirProgressBar(getContext(), null, android.R.attr.progressBarStyle);
			this.progressBar.setLayoutParams(params);
			this.progressBar.hide();
			addView(this.progressBar);
		}

		if (isInEditMode()) {
			Drawable dummy = ContextCompat.getDrawable(getContext(), R.drawable.img_dummy);
			this.imageView.setImageDrawable(dummy);
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
		if (photo.isUri() && this.uriBound != null && this.imageView.getDrawable() != null) {
			String uri = photo.uriNative();
			if (uri.equals(this.uriBound)) return;
		}

		if (this.getBackground() != null) {
			this.getBackground().clearColorFilter();
		}
		this.imageView.setImageDrawable(null);
		this.imageView.setVisibility(VISIBLE);
		this.nameView.setVisibility(GONE);

		if (!showBusy) {
			showLoading(false);
		}

		if (shape.equals("round")) {
			loadImageView(photo, new CircleTransform(), callback);
		}
		else if (shape.equals("rounded")) {
			int displayRadius = UI.getRawPixelsForDisplayPixels((float) this.radius);
			loadImageView(photo, new RoundedCornersTransformation(displayRadius, 0), callback);
		}
		else {
			loadImageView(photo, null, callback);
		}
	}

	public void setImageWithText(String name, Boolean showText) {

		if (this.getBackground() != null) {
			this.getBackground().clearColorFilter();
		}
		this.imageView.setImageDrawable(null);
		this.imageView.setVisibility(GONE);
		this.nameView.setText(null);

		if (!TextUtils.isEmpty(name) && this.getBackground() != null) {
			long seed = Utils.numberFromName(name);
			Integer color = Utils.randomColor(seed);
			this.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

			if (showText) {
				String initials = Utils.initialsFromName(name);
				this.nameView.setText(initials);
				this.nameView.setVisibility(VISIBLE);
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

		creator.into(this.imageView);
	}

	public void showLoading(final Boolean visible) {

		Patchr.mainThreadHandler.post(() -> {
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
					.config(this.bitmapConfig);

				if (transform != null) {
					creator.transform(transform);
					if (this.shape.equals("rounded")) {
						creator.centerCrop();
						creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
					}
				}
				creator.into(this.imageView);
			}
		}
		else if (photo.isFile()) {

			RequestCreator creator = Picasso
				.with(Patchr.applicationContext)
				.load(photo.uriNative())
				.centerCrop()   // Needed so resize() keeps aspect ratio
				.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
				.config(this.bitmapConfig);

			if (transform != null) {
				creator.transform(transform);
				if (this.shape.equals("rounded")) {
					creator.centerCrop();
					creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
				}
			}
			creator.into(this.imageView, callback);
		}
		else {  /* url */

			final String uri = UI.uri(photo.prefix, photo.source, this.category);
			RequestCreator creator = Picasso
				.with(Patchr.applicationContext)
				.load(uri)
				.noFade()
				.networkPolicy(NetworkPolicy.OFFLINE)
				.config(this.bitmapConfig);

			if (transform != null) {
				creator.transform(transform);
				if (this.shape.equals("rounded")) {
					creator.centerCrop();
					creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
				}
			}

			if (callback != null) {
				creator.into(this.imageView, callback);
			}
			else {
				creator.into(this.imageView, new Callback() {

					@Override public void onSuccess() {}

					@Override public void onError() {
						RequestCreator creator = Picasso
							.with(Patchr.applicationContext)
							.load(uri)
							.config(bitmapConfig);

						if (transform != null) {
							creator.transform(transform);
							if (shape.equals("rounded")) {
								creator.centerCrop();
								creator.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX);
							}
						}

						creator.into(imageView);
					}
				});
			}
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