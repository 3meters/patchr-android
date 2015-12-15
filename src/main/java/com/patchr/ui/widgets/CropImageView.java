package com.patchr.ui.widgets;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CropImageView extends ImageView {
	float mWidthPercent = 0, mHeightPercent = 0;

	public CropImageView(Context context) {
		super(context);
		setup();
	}

	public CropImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public CropImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup();
	}

	public void setOffset(float widthPercent, float heightPercent) {
		/*
		 * How far right or down should we place the upper-left corner of the cropbox? [0, 1]
		 */
		mWidthPercent = widthPercent;
		mHeightPercent = heightPercent;
	}

	private void setup() {
		setScaleType(ScaleType.MATRIX);
	}

	@Override
	protected boolean setFrame(int l, int t, int r, int b) {
		configure();
		return super.setFrame(l, t, r, b);
	}

	protected void configure() {

		Matrix matrix = getImageMatrix();

		float scale;
		int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
		int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
		int drawableWidth = 0, drawableHeight = 0;

		/* Allow for setting the drawable later in code by guarding ourselves here. */
		if (getDrawable() != null) {
			drawableWidth = getDrawable().getIntrinsicWidth();
			drawableHeight = getDrawable().getIntrinsicHeight();
		}

		/* Get the scale. */
		if (drawableWidth * viewHeight > drawableHeight * viewWidth) {
			/*
			 * Drawable is flatter than view. Scale it to fill the view height.
			 * A Top/Bottom crop here should be identical in this case.
			 */
			scale = (float) viewHeight / (float) drawableHeight;
		}
		else {
			/*
			 * Drawable is taller than view. Scale it to fill the view width.
			 * Left/Right crop here should be identical in this case.
			 */
			scale = (float) viewWidth / (float) drawableWidth;
		}

		float viewToDrawableWidth = viewWidth / scale;
		float viewToDrawableHeight = viewHeight / scale;
		float xOffset = mWidthPercent * (drawableWidth - viewToDrawableWidth);
		float yOffset = mHeightPercent * (drawableHeight - viewToDrawableHeight);

		/* Define the rect from which to take the image portion. */
		RectF drawableRect = new RectF(xOffset, yOffset, xOffset + viewToDrawableWidth,
				yOffset + viewToDrawableHeight);
		RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
		matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);

		setImageMatrix(matrix);
	}
}