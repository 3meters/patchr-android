package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.patchr.R;

@SuppressWarnings("ucd")
public class AirLinearLayout extends LinearLayout {

	private Integer mMaxWidth;

	public AirLinearLayout(Context context) {
		super(context);
	}

	public AirLinearLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		initialize(context, attrs);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (mMaxWidth != null && mMaxWidth > 0 && mMaxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	private void initialize(Context context, AttributeSet attrs) {
		TypedArray typeArray = getContext().obtainStyledAttributes(attrs, R.styleable.AirLinearLayout);
		mMaxWidth = typeArray.getDimensionPixelSize(R.styleable.AirLinearLayout_maxWidth, getContext().getResources().getDimensionPixelSize(R.dimen.form_max_width));
		typeArray.recycle();
	}
}
