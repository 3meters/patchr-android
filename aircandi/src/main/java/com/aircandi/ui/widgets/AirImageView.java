package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.objects.Photo;
import com.aircandi.utilities.UI;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

@SuppressWarnings("ucd")
public class AirImageView extends FrameLayout implements Target {

	private ImageView   mImageMain;
	private ImageView   mImageZoom;
	private ProgressBar mProgressBar;
	private TextView    mMissingMessage;

	private Photo mPhoto;
	private final Handler mThreadHandler = new Handler();
	private Target mTarget;

	private Integer  mSizeHint;
	private SizeType mSizeType;

	private boolean   mShowBusy   = true;
	private ScaleType mScaleType  = ScaleType.CENTER_CROP;
	private Boolean   mCenterCrop = true;
	private Integer mLayoutId;
	private float   mAspectRatio;
	private boolean mAspectRatioEnabled;
	private int     mDominantMeasurement;

	public static final int MEASUREMENT_WIDTH  = 0;
	public static final int MEASUREMENT_HEIGHT = 1;

	private static final float   DEFAULT_ASPECT_RATIO         = 1f;
	private static final boolean DEFAULT_ASPECT_RATIO_ENABLED = false;
	private static final int     DEFAULT_DOMINANT_MEASUREMENT = MEASUREMENT_WIDTH;

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

	public AirImageView(Context context) {
		this(context, null);
	}

	public AirImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirImageView(Context context, AttributeSet attributes, int defStyle) {
		super(context, attributes, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.AirImageView, defStyle, 0);

		mSizeHint = ta.getDimensionPixelSize(R.styleable.AirImageView_sizeHint, Integer.MAX_VALUE);
		mSizeType = SizeType.values()[ta.getInteger(R.styleable.AirImageView_sizeType, SizeType.FULLSIZE.ordinal())];
		mShowBusy = ta.getBoolean(R.styleable.AirImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.AirImageView_layout, R.layout.widget_webimageview);
		mAspectRatio = ta.getFloat(R.styleable.AirImageView_aspectRatio, DEFAULT_ASPECT_RATIO);
		mAspectRatioEnabled = ta.getBoolean(R.styleable.AirImageView_aspectRatioEnabled,
				DEFAULT_ASPECT_RATIO_ENABLED);
		mDominantMeasurement = ta.getInt(R.styleable.AirImageView_dominantMeasurement,
				DEFAULT_DOMINANT_MEASUREMENT);
		ta.recycle();

		if (!isInEditMode()) {
			final int scaleTypeValue = attributes.getAttributeIntValue(androidNamespace, "scaleType", 6);
			if (scaleTypeValue >= 0) {
				mScaleType = sScaleTypeArray[scaleTypeValue];
			}
		}
		initialize(context);
	}

	private void initialize(Context context) {
		final View view = LayoutInflater.from(context).inflate(mLayoutId, this, true);

		mImageMain = (ImageView) view.findViewById(R.id.image_main);

		if (!isInEditMode()) {
			mImageZoom = (ImageView) view.findViewById(R.id.image_zoom);
			mProgressBar = (ProgressBar) view.findViewById(R.id.image_progress);
			mMissingMessage = (TextView) view.findViewById(R.id.image_missing);
		}

		if (mImageMain != null) {
			if (!(mImageMain instanceof ImageViewTouch)) {
				mImageMain.setScaleType(mScaleType);
			}
			if (isInEditMode()) {
				mImageMain.setImageResource(R.drawable.img_placeholder_logo);
			}
			mImageMain.invalidate();
		}

		if (mProgressBar != null) {
			if (!mShowBusy) {
				mProgressBar.setVisibility(View.GONE);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mImageMain != null) {
			mImageMain.layout(l, t, r, b);
			if (mMissingMessage != null) {
				mMissingMessage.layout(l, t, r, b);
			}
		}
		super.onLayout(changed, l, t, r, b);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!mAspectRatioEnabled) return;

		int newWidth;
		int newHeight;
		switch (mDominantMeasurement) {
			case MEASUREMENT_WIDTH:
				newWidth = getMeasuredWidth();
				newHeight = (int) (newWidth * mAspectRatio);
				break;

			case MEASUREMENT_HEIGHT:
				newHeight = getMeasuredHeight();
				newWidth = (int) (newHeight * mAspectRatio);
				break;

			default:
				throw new IllegalStateException("Unknown measurement with ID " + mDominantMeasurement);
		}

		setMeasuredDimension(newWidth, newHeight);

		if (mImageMain != null) {

			int widthSpec = MeasureSpec.makeMeasureSpec(newWidth, View.MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(newHeight, View.MeasureSpec.EXACTLY);
			mImageMain.measure(widthSpec, heightSpec);

			if (mMissingMessage != null) {
				mMissingMessage.measure(widthSpec, heightSpec);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mImageMain instanceof ImageViewTouch)
			return mImageMain.onTouchEvent(event);
		else
			return super.onTouchEvent(event);
	}

	@Override
	public void onBitmapFailed(Drawable drawable) {
		if (mTarget != null) {
			mTarget.onBitmapFailed(drawable);
		}
		else {
			showMissing(true);
			if (mShowBusy) {
				showLoading(false);
			}
		}
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, LoadedFrom loadedFrom) {
		if (mTarget != null) {
			mTarget.onBitmapLoaded(bitmap, loadedFrom);
		}
		else {
			final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
			UI.showDrawableInImageView(bitmapDrawable, mImageMain, true, AnimationManager.fadeInMedium());
			showMissing(false);
			if (mShowBusy) {
				showLoading(false);
			}
		}
	}

	@Override
	public void onPrepareLoad(Drawable drawable) {
		if (mTarget != null) {
			mTarget.onPrepareLoad(drawable);
		}
		else {
			showMissing(false);
			if (mShowBusy) {
				showLoading(true);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public void showLoading(final Boolean visible) {
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		});
	}

	public void showMissing(final Boolean visible) {

		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (mSizeType != SizeType.THUMBNAIL) {
					mMissingMessage.setVisibility(visible ? View.VISIBLE : View.GONE);
				}
				else {
					if (visible) {
						Integer resId = Aircandi.themeTone.equals(ThemeTone.LIGHT) ? R.drawable.img_broken_light : R.drawable.img_broken_dark;
						Drawable drawable = getResources().getDrawable(resId);
						UI.showDrawableInImageView(drawable, mImageMain, true, AnimationManager.fadeInMedium());
					}
				}
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
	public ImageView getImageView() {
		return mImageMain;
	}

	public AirImageView setImageView(ImageView imageView) {
		mImageMain = imageView;
		return this;
	}

	public ImageView getImageZoom() {
		return mImageZoom;
	}

	/**
	 * @return Desired width in pixels.
	 */
	public Integer getSizeHint() {
		return mSizeHint;
	}

	public AirImageView setSizeHint(Integer sizeHint) {
		mSizeHint = sizeHint;
		return this;
	}

	public Photo getPhoto() {
		return mPhoto;
	}

	public AirImageView setPhoto(Photo photo) {
		mPhoto = photo;
		return this;
	}

	/**
	 * @return Desired download width in pixels.
	 */
	public SizeType getSizeType() {
		return mSizeType;
	}

	public AirImageView setSizeType(SizeType sizeType) {
		mSizeType = sizeType;
		return this;
	}

	public Boolean getCenterCrop() {
		return mCenterCrop;
	}

	public AirImageView setCenterCrop(Boolean centerCrop) {
		mCenterCrop = centerCrop;
		return this;
	}

	public TextView getMissingMessage() {
		return mMissingMessage;
	}

	public void setMissingMessage(TextView missingMessage) {
		mMissingMessage = missingMessage;
	}

	public Target getTarget() {
		return mTarget;
	}

	public AirImageView setTarget(Target target) {
		mTarget = target;
		return this;
	}

	/**
	 * Get the aspect ratio for this image view.
	 */
	public float getAspectRatio() {
		return mAspectRatio;
	}

	/**
	 * Set the aspect ratio for this image view. This will update the view instantly.
	 */
	public void setAspectRatio(float aspectRatio) {
		this.mAspectRatio = aspectRatio;
		if (mAspectRatioEnabled) {
			requestLayout();
		}
	}

	/**
	 * Get whether or not forcing the aspect ratio is enabled.
	 */
	public boolean getAspectRatioEnabled() {
		return mAspectRatioEnabled;
	}

	/**
	 * set whether or not forcing the aspect ratio is enabled. This will re-layout the view.
	 */
	public void setAspectRatioEnabled(boolean aspectRatioEnabled) {
		this.mAspectRatioEnabled = aspectRatioEnabled;
		requestLayout();
	}

	/**
	 * Get the dominant measurement for the aspect ratio.
	 */
	public int getDominantMeasurement() {
		return mDominantMeasurement;
	}

	/**
	 * Set the dominant measurement for the aspect ratio.
	 *
	 * @see #MEASUREMENT_WIDTH
	 * @see #MEASUREMENT_HEIGHT
	 */
	public void setDominantMeasurement(int dominantMeasurement) {
		if (dominantMeasurement != MEASUREMENT_HEIGHT && dominantMeasurement != MEASUREMENT_WIDTH) {
			throw new IllegalArgumentException("Invalid measurement type.");
		}
		this.mDominantMeasurement = dominantMeasurement;
		requestLayout();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public enum SizeType {
		/*
		 * Always append new enum items because there is a
		 * dependency on ordering for persistence.
		 */
		FULLSIZE,
		PREVIEW,
		THUMBNAIL,
		FULLSIZE_CAPPED,
		PREVIEW_LARGE
	}
}
