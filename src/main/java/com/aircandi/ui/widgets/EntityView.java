package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.R;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.utilities.UI;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

@SuppressWarnings("ucd")
public class EntityView extends LinearLayout implements Target {

	private ViewGroup    mBoundView;
	private AirImageView mPhotoView;
	private TextView     mName;
	private TextView     mSubtitle;
	private TextView     mLabel;
	private ImageView    mButtonDelete;
	private Entity       mEntity;
	private Integer      mLabelResId;
	private Integer      mLayoutResId;
	private View         mParentView;
	private Boolean mAnimateDisabled = false;

	public EntityView(Context context) {
		this(context, null);
	}

	public EntityView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public EntityView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

		if (attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EntityView, defStyle, 0);
			mLayoutResId = ta.getResourceId(R.styleable.EntityView_layoutId, R.layout.widget_entity_view);
			mLabelResId = ta.getResourceId(R.styleable.EntityView_label, 0);

			ta.recycle();
			initialize();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {

		if (mLayoutResId != null) {
			removeAllViews();
			mBoundView = (ViewGroup) LayoutInflater.from(getContext()).inflate(mLayoutResId, this, true);
			mPhotoView = (AirImageView) mBoundView.findViewById(R.id.entity_view_photo);
			mName = (TextView) mBoundView.findViewById(R.id.entity_view_name);
			mSubtitle = (TextView) mBoundView.findViewById(R.id.entity_view_subtitle);
			mLabel = (TextView) mBoundView.findViewById(R.id.entity_view_label);
			mButtonDelete = (ImageView) mBoundView.findViewById(R.id.button_delete_entity);
		}
	}

	public void databind(Entity entity) {
		mEntity = entity;
		this.setTag(entity);
		draw();
	}

	private void draw() {
		if (mEntity != null) {
			UI.setVisibility(mLabel, View.GONE);
			if (mLabel != null) {
				if (mLabelResId != 0) {
					mLabel.setText(StringManager.getString(mLabelResId));
					UI.setVisibility(mLabel, View.VISIBLE);
				}
			}

			UI.setVisibility(mName, View.GONE);
			if (mName != null) {
				if (mEntity.name != null) {
					mName.setText(mEntity.name);
				}
				else {
					mName.setText(mEntity.schema);
				}
				UI.setVisibility(mName, View.VISIBLE);
			}

			UI.setVisibility(mSubtitle, View.GONE);
			if (mSubtitle != null) {
				if (mEntity.subtitle != null) {
					mSubtitle.setText(mEntity.subtitle);
					UI.setVisibility(mSubtitle, View.VISIBLE);
				}
			}

			if (mPhotoView != null) {
				Photo photo = mEntity.getPhoto();
				if (mPhotoView.getPhoto() == null || !photo.getUri().equals(mPhotoView.getPhoto().getUri())) {
					mPhotoView.setTarget(this);
					UI.drawPhoto(mPhotoView, photo);
					mPhotoView.setTag(photo);
				}
			}
		}
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);

		if (selected) {
			if (mButtonDelete != null) {
				mButtonDelete.setVisibility(View.VISIBLE);
				mPhotoView.setVisibility(View.GONE);
			}
		}
		else {
			if (mButtonDelete != null) {
				mButtonDelete.setVisibility(View.GONE);
				mPhotoView.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onBitmapFailed(Drawable arg0) {
		UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
	    /*
	     * Called on main thread and whether bitmap was loaded from network or memory.
	     */
		DownloadManager.decorate(bitmap, loadedFrom);
		DownloadManager.logBitmap(EntityView.this, bitmap, mPhotoView.getImageView());
		final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
		if (mAnimateDisabled) {
			mPhotoView.getImageView().setImageDrawable(bitmapDrawable);
			if (mParentView != null) {
				mParentView.invalidate();
			}
		}
		else {
			UI.showDrawableInImageView(bitmapDrawable, mPhotoView.getImageView(), true);
		}
	}

	@Override
	public void onPrepareLoad(Drawable drawable) {}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
	public void setAnimateDisabled(Boolean animateDisabled) {
		mAnimateDisabled = animateDisabled;
	}

	public void setLabel(Integer labelResId) {
		mLabelResId = labelResId;
	}

	public void setLayout(Integer layoutResId) {
		mLayoutResId = layoutResId;
	}

	public AirImageView getPhotoView() {
		return mPhotoView;
	}

	public void setParentView(View parentView) {
		mParentView = parentView;
	}

	public void setPhotoView(AirImageView mPhotoView) {
		this.mPhotoView = mPhotoView;
	}
}
