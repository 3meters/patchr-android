package com.patchr.ui.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class TextDrawable extends Drawable {

	private final String mText;
	private final Paint mPaint = new Paint();
	private float mTextSize;
	private int   mTextColor;
	private float mTextWidth;

	public TextDrawable(String text, Float textSize) {

		mText = text;
		mTextSize = textSize;       // In raw pixels
		mTextColor = Color.DKGRAY;
		buildPaint();
	}

	private void buildPaint() {
		mPaint.setColor(mTextColor);
		mPaint.setTextSize(mTextSize);
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setTextAlign(Paint.Align.LEFT);
		mTextWidth = mPaint.measureText(mText, 0, mText.length());
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		canvas.drawText(mText, 0, 15, mPaint);
	}

	public float getTextWidth() {
		return mTextWidth;
	}

	public void setTextSize(Float textSize) {
		mTextSize = textSize;
		buildPaint();
	}

	public void setTextColor(int textColor) {
		mTextColor = textColor;
		buildPaint();
	}

	@Override
	public void setAlpha(int alpha) {
		mPaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mPaint.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}