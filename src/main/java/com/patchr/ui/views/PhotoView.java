package com.patchr.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoSizeCategory;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.utilities.UI;
import com.squareup.picasso.Target;

@SuppressWarnings("ucd")
public class PhotoView extends FrameLayout {

	private static final float  DEFAULT_ASPECT_RATIO = 0f;
	private static final String androidNamespace     = "http://schemas.android.com/apk/res/android";

	private   Photo  mPhoto;
	protected String mUriBound;

	protected AppCompatImageView mImageView;
	protected AirProgressBar     mProgressBar;

	protected Target            mTarget;
	protected float             mAspectRatio;
	protected PhotoSizeCategory mSizeCategory;
	protected String mShape = "auto";    // auto, square, round
	protected ScaleType mScaleType;
	protected String    mGroupTag;

	protected boolean mShowBusy = true;

	protected Bitmap.Config mConfig = Bitmap.Config.RGB_565;  // Used by picasso

	public PhotoView(Context context) {
		this(context, null);
	}

	public PhotoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoView(Context context, AttributeSet attributes, int defStyle) {
		super(context, attributes, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.PhotoView, defStyle, 0);

		mConfig = Bitmap.Config.values()[ta.getInteger(R.styleable.PhotoView_config, Bitmap.Config.ARGB_8888.ordinal())];
		mSizeCategory = PhotoSizeCategory.values()[ta.getInteger(R.styleable.PhotoView_sizeCategory, PhotoSizeCategory.THUMBNAIL.ordinal())];
		mShowBusy = ta.getBoolean(R.styleable.PhotoView_showBusy, true);
		mAspectRatio = ta.getFloat(R.styleable.PhotoView_aspectRatio, DEFAULT_ASPECT_RATIO);
		if (ta.hasValue(R.styleable.PhotoView_shape)) {
			mShape = ta.getString(R.styleable.PhotoView_shape);
		}

		ta.recycle();

		if (!isInEditMode()) {
			final int scaleTypeValue = attributes.getAttributeIntValue(androidNamespace, "scaleType", ScaleType.CENTER_CROP.ordinal());
			if (scaleTypeValue >= 0) {
				mScaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize(context);
	}

	protected void initialize(Context context) {

		/* Placeholder */
		this.setBackgroundResource(UI.getResIdForAttribute(getContext(), R.attr.backgroundPlaceholder));

		/* Image - subclass could have provide it instead */
		if (mImageView == null) {
			mImageView = new AppCompatImageView(getContext());
			mImageView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			mImageView.setScaleType(mScaleType);
			addView(mImageView);
		}

		/* Activity indicator - last added is in front */
		mProgressBar = new AirProgressBar(getContext(), null, android.R.attr.progressBarStyle);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(72, 72);
		params.gravity = Gravity.CENTER;
		mProgressBar.setLayoutParams(params);
		mProgressBar.hide();
		addView(mProgressBar);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (mAspectRatio != 0) {

			int w = MeasureSpec.getSize(widthMeasureSpec);
			int h = (int) ((float) w * mAspectRatio);
			setMeasuredDimension(w, h);

			/* We have to enforce the sizing on the child imageview */
			int widthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
			mImageView.measure(widthSpec, heightSpec);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void showLoading(final Boolean visible) {
		Patchr.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (mProgressBar != null) {
					if (visible) {
						mProgressBar.show();
					}
					else {
						mProgressBar.hide();
					}
				}
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public ImageView getImageView() {
		return mImageView;
	}

	public PhotoView setImageView(AppCompatImageView imageView) {
		mImageView = imageView;
		return this;
	}

	public String getGroupTag() {
		return mGroupTag;
	}

	public PhotoView setGroupTag(String group) {
		mGroupTag = group;
		return this;
	}

	public ScaleType getScaleType() {
		return mScaleType;
	}

	public PhotoView setScaleType(ScaleType scaleType) {
		mScaleType = scaleType;
		return this;
	}

	public Bitmap.Config getConfig() {
		return mConfig;
	}

	public PhotoView setConfig(Bitmap.Config config) {
		mConfig = config;
		return this;
	}

	public Photo getPhoto() {
		return mPhoto;
	}

	public PhotoView setPhoto(Photo photo) {
		mPhoto = photo;
		return this;
	}

	public Target getTarget() {
		return mTarget;
	}

	public PhotoView setTarget(Target target) {
		mTarget = target;
		return this;
	}

	public Float getAspectRatio() {
		return mAspectRatio;
	}

	public PhotoView setAspectRatio(Float aspectRatio) {
		this.mAspectRatio = aspectRatio;
		requestLayout();
		return this;
	}

	public PhotoSizeCategory getSizeCategory() {
		return mSizeCategory;
	}

	public PhotoView setSizeCategory(PhotoSizeCategory sizeCategory) {
		mSizeCategory = sizeCategory;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private static final ScaleType[] sScaleTypeArray = {
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