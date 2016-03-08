package com.patchr.ui.widgets;

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
import com.patchr.utilities.UI;
import com.squareup.picasso.Target;

@SuppressWarnings("ucd")
public class AirPhotoView extends FrameLayout {

	private static final float  DEFAULT_ASPECT_RATIO = 0f;
	private static final String androidNamespace     = "http://schemas.android.com/apk/res/android";

	private   Photo  mPhoto;
	protected String mUriBound;

	protected AppCompatImageView mImageMain;
	protected AirProgressBar     mProgressBar;

	protected Target            mTarget;
	protected float             mAspectRatio;
	protected PhotoSizeCategory mSizeCategory;
	protected String mShape = "auto";    // auto, square, round
	protected ScaleType mScaleType;
	protected String    mGroupTag;

	protected boolean mShowBusy = true;

	protected Bitmap.Config mConfig = Bitmap.Config.RGB_565;  // Used by picasso

	public AirPhotoView(Context context) {
		this(context, null);
	}

	public AirPhotoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirPhotoView(Context context, AttributeSet attributes, int defStyle) {
		super(context, attributes, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.AirPhotoView, defStyle, 0);

		mConfig = Bitmap.Config.values()[ta.getInteger(R.styleable.AirPhotoView_config, Bitmap.Config.ARGB_8888.ordinal())];
		mSizeCategory = PhotoSizeCategory.values()[ta.getInteger(R.styleable.AirPhotoView_sizeCategory, PhotoSizeCategory.THUMBNAIL.ordinal())];
		mShowBusy = ta.getBoolean(R.styleable.AirPhotoView_showBusy, true);
		mAspectRatio = ta.getFloat(R.styleable.AirPhotoView_aspectRatio, DEFAULT_ASPECT_RATIO);
		if (ta.hasValue(R.styleable.AirPhotoView_shape)) {
			mShape = ta.getString(R.styleable.AirPhotoView_shape);
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
		if (mImageMain == null) {
			mImageMain = new AppCompatImageView(getContext());
			mImageMain.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			mImageMain.setScaleType(mScaleType);
			addView(mImageMain);
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
			mImageMain.measure(widthSpec, heightSpec);
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
		return mImageMain;
	}

	public AirPhotoView setImageView(AppCompatImageView imageView) {
		mImageMain = imageView;
		return this;
	}

	public String getGroupTag() {
		return mGroupTag;
	}

	public AirPhotoView setGroupTag(String group) {
		mGroupTag = group;
		return this;
	}

	public ScaleType getScaleType() {
		return mScaleType;
	}

	public AirPhotoView setScaleType(ScaleType scaleType) {
		mScaleType = scaleType;
		return this;
	}

	public Bitmap.Config getConfig() {
		return mConfig;
	}

	public AirPhotoView setConfig(Bitmap.Config config) {
		mConfig = config;
		return this;
	}

	public Photo getPhoto() {
		return mPhoto;
	}

	public AirPhotoView setPhoto(Photo photo) {
		mPhoto = photo;
		return this;
	}

	public Target getTarget() {
		return mTarget;
	}

	public AirPhotoView setTarget(Target target) {
		mTarget = target;
		return this;
	}

	public Float getAspectRatio() {
		return mAspectRatio;
	}

	public AirPhotoView setAspectRatio(Float aspectRatio) {
		this.mAspectRatio = aspectRatio;
		requestLayout();
		return this;
	}

	public PhotoSizeCategory getSizeCategory() {
		return mSizeCategory;
	}

	public AirPhotoView setSizeCategory(PhotoSizeCategory sizeCategory) {
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