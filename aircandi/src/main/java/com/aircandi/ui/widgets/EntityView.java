package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class EntityView extends LinearLayout {

	private ViewGroup		mBoundView;
	private AirImageView	mPhotoView;
	private TextView		mName;
	private TextView		mSubtitle;
	private TextView		mLabel;

	private Entity			mEntity;
	private Integer			mLabelResId;

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
			final int layoutId = ta.getResourceId(R.styleable.EntityView_layout, R.layout.widget_entity_view);
			mBoundView = (ViewGroup) LayoutInflater.from(getContext()).inflate(layoutId, null);
			mLabelResId = ta.getResourceId(R.styleable.EntityView_label, 0);

			ta.recycle();
			initialize();
		}
	}

	private void initialize() {
		if (!isInEditMode()) {
			mPhotoView = (AirImageView) mBoundView.findViewById(R.id.entity_view_photo);
			mName = (TextView) mBoundView.findViewById(R.id.entity_view_name);
			mSubtitle = (TextView) mBoundView.findViewById(R.id.entity_view_subtitle);
			mLabel = (TextView) mBoundView.findViewById(R.id.entity_view_label);
		}

		removeAllViews();
		this.addView(mBoundView);
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
				if (mPhotoView.getPhoto() == null || !mPhotoView.getPhoto().getUri().equals(mEntity.getPhoto().getUri())) {
					UI.drawPhoto(mPhotoView, mEntity.getPhoto());
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public void setLabel(Integer labelResId) {
		mLabelResId = labelResId;
	}
}
