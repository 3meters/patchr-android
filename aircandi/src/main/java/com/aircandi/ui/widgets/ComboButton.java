package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class ComboButton extends RelativeLayout {

	private Integer			mLayoutResId;
	private Integer			mDrawableResId;
	private String			mLabel;

	private ViewGroup		mLayout;
	private ImageView		mImageIcon;
	private TextView		mTextLabel;
	private ViewAnimator	mViewAnimator;

	public ComboButton(Context context) {
		this(context, null);
	}

	public ComboButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ComboButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {

			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ComboButton, defStyle, 0);
			mLayoutResId = ta.getResourceId(R.styleable.ComboButton_layout, R.layout.widget_combo_button);
			mDrawableResId = ta.getResourceId(R.styleable.ComboButton_drawable, 0);
			Integer labelResId = ta.getResourceId(R.styleable.ComboButton_label, 0);

			ta.recycle();
			initialize();
			if (!isInEditMode()) {
                if (labelResId != 0) {
                    mLabel = StringManager.getString(labelResId);
                }
				draw();
			}
		}
	}

	private void initialize() {

		mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(mLayoutResId, this, true);
		if (!isInEditMode()) {
			mTextLabel = (TextView) mLayout.findViewById(R.id.button_label);
			mImageIcon = (ImageView) mLayout.findViewById(R.id.button_image);
			mViewAnimator = (ViewAnimator) mLayout.findViewById(R.id.button_animator);
		}
	}

	private void draw() {
		if (mDrawableResId != 0) {
			mImageIcon.setImageDrawable(getResources().getDrawable(mDrawableResId));
		}
		else {
			UI.setVisibility(mViewAnimator, View.GONE);
		}
		if (mTextLabel != null) {
			if (mLabel != null) {
				mTextLabel.setText(mLabel);
			}
			else {
				mTextLabel.setVisibility(View.GONE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Setters/getters
	// --------------------------------------------------------------------------------------------

	public void setLayoutId(Integer layoutId) {
		mLayoutResId = layoutId;
	}

	public Integer getDrawableId() {
		return mDrawableResId;
	}

	public void setDrawableId(Integer drawableId) {
		mDrawableResId = drawableId;
		draw();
	}

	public String getLabel() {
		return mLabel;
	}

	public void setLabel(Integer labelResId) {
		mLabel = StringManager.getString(labelResId);
		draw();
	}

	public void setLabel(String label) {
		mLabel = label;
		draw();
	}

	public ImageView getImageIcon() {
		return mImageIcon;
	}

	public void setImageIcon(ImageView imageIcon) {
		mImageIcon = imageIcon;
	}

	public ViewAnimator getViewAnimator() {
		return mViewAnimator;
	}
}
