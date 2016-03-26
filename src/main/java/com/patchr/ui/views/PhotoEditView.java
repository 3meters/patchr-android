package com.patchr.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;
import com.patchr.utilities.UI;
import com.squareup.picasso.Callback;

@SuppressWarnings("ucd")
public class PhotoEditView extends FrameLayout implements Callback {

	private static final Object lock = new Object();

	protected Photo    photo;
	protected BaseView base;
	protected Integer  layoutResId;
	protected boolean  collapseIfEmpty;

	public    PhotoCategory       category;
	protected float               aspectRatio;
	protected ImageView.ScaleType scaleType;
	public    Bitmap.Config       bitmapConfig;  // Used by picasso
	protected boolean             showBusy;
	protected String              shape;    // auto, square, round, rounded
	protected Integer             radius;

	protected ViewGroup    layout;
	public    ImageLayout  photoView;
	protected View         setButton;
	protected View         editButton;
	protected View         deleteButton;
	protected ViewAnimator photoAnimator;

	public PhotoEditView(Context context) {
		this(context, null, 0);
	}

	public PhotoEditView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoEditView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_photo_edit;

		this.scaleType = ImageView.ScaleType.CENTER_CROP;
		this.shape = "auto";
		this.radius = 8;

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ImageLayout, defStyle, 0);

		this.bitmapConfig = Bitmap.Config.values()[ta.getInteger(R.styleable.ImageLayout_config, Bitmap.Config.RGB_565.ordinal())];
		this.category = PhotoCategory.values()[ta.getInteger(R.styleable.ImageLayout_category, PhotoCategory.THUMBNAIL.ordinal())];
		this.showBusy = ta.getBoolean(R.styleable.ImageLayout_showBusy, true);
		this.aspectRatio = ta.getFloat(R.styleable.ImageLayout_aspectRatio, 0f);
		this.radius = ta.getInteger(R.styleable.ImageLayout_radius, 0);

		if (ta.hasValue(R.styleable.ImageLayout_shape)) {
			this.shape = ta.getString(R.styleable.ImageLayout_shape);
		}

		ta.recycle();

		if (!isInEditMode()) {
			final int scaleTypeValue = attrs.getAttributeIntValue(androidNamespace, "scaleType", ImageView.ScaleType.CENTER_CROP.ordinal());
			if (scaleTypeValue >= 0) {
				this.scaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize();
	}

	public PhotoEditView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.photoView = (ImageLayout) layout.findViewById(R.id.photo);
		this.setButton = (View) layout.findViewById(R.id.photo_set_button);
		this.editButton = (View) layout.findViewById(R.id.photo_edit_button);
		this.deleteButton = (View) layout.findViewById(R.id.photo_delete_button);
		this.photoAnimator = (ViewAnimator) layout.findViewById(R.id.photo_animator);

		this.photoView.category = this.category;
		this.photoView.shape = this.shape;
		this.photoView.aspectRatio = this.aspectRatio;
		this.photoView.radius = this.radius;
		this.photoView.showBusy = this.showBusy;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onSuccess() {
		if (collapseIfEmpty) {
			photoAnimator.setInAnimation(getContext(), R.anim.slide_in_bottom_long);
			photoAnimator.requestLayout();
			photoAnimator.setDisplayedChild(1);
			photoAnimator.setInAnimation(getContext(), R.anim.fade_in_medium);
		}
	}

	@Override public void onError() {
		UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(Photo photo) {

		synchronized (lock) {

			this.photo = photo;

			UI.setVisibility(setButton, GONE);
			UI.setVisibility(editButton, GONE);
			UI.setVisibility(deleteButton, GONE);

			if (photo == null) {
				photoView.imageView.setImageDrawable(null);
				if (collapseIfEmpty) {
					photoAnimator.setDisplayedChild(0);
				}
				else {
					photoAnimator.setDisplayedChild(1);
					UI.setVisibility(setButton, VISIBLE);
				}
			}
			else {
				photoAnimator.setDisplayedChild(1);
				photoView.setImageWithPhoto(photo, this);
				UI.setVisibility(this.editButton, VISIBLE);
				UI.setVisibility(this.deleteButton, VISIBLE);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private static final String                androidNamespace = "http://schemas.android.com/apk/res/android";
	private static final ImageView.ScaleType[] sScaleTypeArray  = {
			ImageView.ScaleType.MATRIX,
			ImageView.ScaleType.FIT_XY,
			ImageView.ScaleType.FIT_START,
			ImageView.ScaleType.FIT_CENTER,
			ImageView.ScaleType.FIT_END,
			ImageView.ScaleType.CENTER,
			ImageView.ScaleType.CENTER_CROP,
			ImageView.ScaleType.CENTER_INSIDE
	};
}