package com.aircandi.ui.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.aircandi.components.FontManager;
import com.aircandi.utilities.UI;

public class TextDrawable extends Drawable {

	private final String mText;
	private final Paint  mPaint;
	private       float  mTextSize;
	private       int    mTextColor;
	private       float  mTextWidth;

	public TextDrawable(String text) {

		mText = text;
		mTextSize = UI.getRawPixelsForScaledPixels(18f);
		mTextColor = Color.DKGRAY;
		this.mPaint = new Paint();
		buildPaint();
	}

	private void buildPaint() {
		mPaint.setColor(mTextColor);
		mPaint.setTextSize(mTextSize);
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setTextAlign(Paint.Align.LEFT);
		mPaint.setTypeface(FontManager.fontRobotoLight);
		mTextWidth = mPaint.measureText(mText, 0, mText.length());
	}

	@Override
	public void draw(Canvas canvas) {
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