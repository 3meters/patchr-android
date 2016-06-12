package com.patchr.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class SquareButton extends Button {

	public SquareButton(Context context) {
		this(context, null);
	}

	public SquareButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SquareButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//noinspection SuspiciousNameCombination
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
