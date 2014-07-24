package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.aircandi.components.FontManager;

public class AirSquareButton extends AirButton {

	public AirSquareButton(Context context) {
		this(context, null);
	}

	public AirSquareButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirSquareButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
		}		
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
