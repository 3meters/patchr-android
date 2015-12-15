package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.patchr.components.FontManager;

public class SquareButton extends AirButton {

	public SquareButton(Context context) {
		this(context, null);
	}

	public SquareButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SquareButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//noinspection SuspiciousNameCombination
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
