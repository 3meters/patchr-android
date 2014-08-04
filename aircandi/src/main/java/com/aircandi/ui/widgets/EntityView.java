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

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.utilities.UI;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

@SuppressWarnings("ucd")
public class EntityView extends LinearLayout {

    private ViewGroup            mBoundView;
    private AirImageView         mPhotoView;
    private TextView             mName;
    private TextView             mSubtitle;
    private TextView             mLabel;
    private ImageView            mButtonDelete;
    private Entity               mEntity;
    private Integer              mLabelResId;
    private Integer              mLayoutResId;
    private View                 mParentView;

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
            mLayoutResId = ta.getResourceId(R.styleable.EntityView_layout, R.layout.widget_entity_view);
            mLabelResId = ta.getResourceId(R.styleable.EntityView_label, 0);

            ta.recycle();
            initialize();
        }
    }

    // --------------------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------------------

    public void initialize() {

        if (mLayoutResId != null) {
            removeAllViews();
            mBoundView = (ViewGroup) LayoutInflater.from(getContext()).inflate(mLayoutResId, this, true);
            mPhotoView = (AirImageView) mBoundView.findViewById(R.id.entity_view_photo);
            mName = (TextView) mBoundView.findViewById(R.id.entity_view_name);
            mSubtitle = (TextView) mBoundView.findViewById(R.id.entity_view_subtitle);
            mLabel = (TextView) mBoundView.findViewById(R.id.entity_view_label);
            mButtonDelete = (ImageView) mBoundView.findViewById(R.id.button_delete);
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
                    mName.setText(mEntity.getSchemaMapped());
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

                    mPhotoView.setTarget(new Target() {

                        @Override
                        public void onBitmapFailed(Drawable arg0) {
                            UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
                        }

                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                            final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
                            UI.showDrawableInImageView(bitmapDrawable, mPhotoView.getImageView(), false, null);
                            if (mParentView != null) {
                                mParentView.invalidate();
                            }
                        }

                        @Override
                        public void onPrepareLoad(Drawable arg0) {
                        }
                    });

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

    // --------------------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------------------

    public void setLabel(Integer labelResId) {
        mLabelResId = labelResId;
    }

    public void setLayout(Integer layoutResId) {
        mLayoutResId = layoutResId;
    }

    public AirImageView getPhotoView() {
        return mPhotoView;
    }

    public void setPhotoView(AirImageView mPhotoView) {
        this.mPhotoView = mPhotoView;
    }

    public View getParentView() {
        return mParentView;
    }

    public void setParentView(View mParentView) {
        this.mParentView = mParentView;
    }
}
