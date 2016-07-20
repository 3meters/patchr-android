package com.patchr.ui.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.StringManager;
import com.patchr.model.Photo;
import com.patchr.objects.enums.PhotoCategory;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.picasso.Callback;

@SuppressWarnings("ucd")
public class PhotoEditWidget extends FrameLayout implements Callback {

	private static final Object lock = new Object();

	public    Photo   photo;
	protected Integer layoutResId;
	public    boolean dirty;

	public    PhotoCategory       category;
	protected float               aspectRatio;
	protected ImageView.ScaleType scaleType;
	public    Bitmap.Config       bitmapConfig;  // Used by picasso
	protected boolean             showBusy;
	protected String              shape;    // auto, square, round, rounded
	protected Integer             radius;

	protected ViewGroup   layout;
	public    ImageWidget imageWidget;
	protected View        setButton;
	protected View        editButton;
	protected View        deleteButton;
	protected ViewGroup   photoGroup;

	public PhotoEditWidget(Context context) {
		this(context, null, 0);
	}

	public PhotoEditWidget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoEditWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		this.layoutResId = R.layout.view_photo_edit;

		this.scaleType = ImageView.ScaleType.CENTER_CROP;
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
			final int scaleTypeValue = attrs.getAttributeIntValue(androidNamespace, "scaleType", ImageView.ScaleType.CENTER_CROP.ordinal());
			if (scaleTypeValue >= 0) {
				this.scaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onSuccess() {
		ObjectAnimator anim = ObjectAnimator.ofFloat(imageWidget.imageView, "alpha", 0f, 1f);
		anim.setDuration(300);
		anim.start();
		UI.setVisibility(this.editButton, VISIBLE);
		UI.setVisibility(this.deleteButton, VISIBLE);
	}

	@Override public void onError() {
		/* When handling callbacks, need to add offline/fetch cascade */
		UI.toast(StringManager.getString(R.string.label_photo_missing));
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.imageWidget = (ImageWidget) layout.findViewById(R.id.photo);
		this.setButton = (View) layout.findViewById(R.id.photo_set_button);
		this.editButton = (View) layout.findViewById(R.id.photo_edit_button);
		this.deleteButton = (View) layout.findViewById(R.id.photo_delete_button);
		this.photoGroup = (ViewGroup) layout.findViewById(R.id.photo_group);

		this.deleteButton.setBackground(UI.setTint(this.deleteButton.getBackground(), R.color.white_80_pcnt));
		this.editButton.setBackground(UI.setTint(this.deleteButton.getBackground(), R.color.white_80_pcnt));
		this.setButton.setBackground(UI.setTint(this.deleteButton.getBackground(), R.color.white_80_pcnt));

		this.imageWidget.category = this.category;
		this.imageWidget.shape = this.shape;
		this.imageWidget.aspectRatio = this.aspectRatio;
		this.imageWidget.radius = this.radius;
		this.imageWidget.showBusy = this.showBusy;
		this.imageWidget.imageView.setAlpha(0.0f);

		/* Photo edit view always provides the correct placeholder */
		this.imageWidget.setBackgroundResource(0);
	}

	public void bind(Photo photo) {

		synchronized (lock) {

			UI.setVisibility(setButton, GONE);
			UI.setVisibility(editButton, GONE);
			UI.setVisibility(deleteButton, GONE);

			if (photo == null) {
				if (imageWidget.imageView.getDrawable() != null) {
					ObjectAnimator anim = ObjectAnimator.ofFloat(imageWidget.imageView, "alpha", 1f, 0f);
					anim.setDuration(300);
					anim.start();
				}
				UI.setVisibility(setButton, VISIBLE);
			}
			else {
				/* Convert external uri to file uri */
				if (photo.source.equals(Photo.PhotoSource.generic)) {
					/* Download image and save to file */
					AsyncTask.execute(() -> {
						Bitmap bitmap = Photo.getBitmapForPhoto(photo);
						if (bitmap == null) {
							Logger.w(this, "Failed to download bitmap from the network");
						}
						else {
							String filename = Utils.md5(photo.prefix) + ".jpg";
							if (MediaManager.copyBitmapToInternalStorage(getContext(), bitmap, filename)) {
								String path = String.format("file://%1$s/%2$s", getContext().getFilesDir(), filename);
								Photo photoSaved = new Photo(path, Photo.PhotoSource.file);
								this.photo = photoSaved;
								Patchr.mainThreadHandler.post(() -> {
									imageWidget.setImageWithPhoto(photoSaved, null, this);
								});
								return;
							}
							Logger.w(this, "Failed to save bitmap to internal storage");
						}
					});

				}
				else {
					this.photo = photo;
					imageWidget.setImageWithPhoto(photo, null, this);
				}
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