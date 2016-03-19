package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class AirRelativeLayout extends RelativeLayout {

	private Integer mMaxWidth;

	public AirRelativeLayout(Context context) {
		super(context);
		initialize(context, null);
	}

	public AirRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, attrs);
	}

	public AirRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context, attrs);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int maxWidth = UI.getRawPixelsForDisplayPixels((float) Constants.MAX_WIDTH_FORM);
		if (maxWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize(Context context, AttributeSet attrs) {
		TypedArray typeArray = getContext().obtainStyledAttributes(attrs, R.styleable.AirRelativeLayout);
		mMaxWidth = typeArray.getDimensionPixelSize(R.styleable.AirRelativeLayout_maxWidth, getContext().getResources().getDimensionPixelSize(R.dimen.form_max_width));
		typeArray.recycle();
	}
}
