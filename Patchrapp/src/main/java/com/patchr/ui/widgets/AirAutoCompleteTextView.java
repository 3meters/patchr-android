package com.patchr.ui.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AutoCompleteTextView;

import com.patchr.R;
import com.patchr.ui.components.SimpleTextWatcher;

public class AirAutoCompleteTextView extends AutoCompleteTextView {
	/*
	 * Only used by the photo picker search
	 */
	private Boolean mEnableClearButton = false;

	private Drawable mClearDrawable;
	private Drawable mSearchDrawable;

	@SuppressWarnings("ucd")
	public AirAutoCompleteTextView(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirAutoCompleteTextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {

		final Drawable[] drawables = getCompoundDrawables();

		if (drawables.length == 4) {
			mClearDrawable = drawables[2];
			mSearchDrawable = drawables[0];
			Integer drawableWidth = getResources().getDimensionPixelSize(R.dimen.drawable_width);
			@SuppressWarnings("SuspiciousNameCombination") Integer drawableHeight = drawableWidth;

			if (mClearDrawable != null) {
				mEnableClearButton = true;
				Bitmap bitmap = ((BitmapDrawable) mClearDrawable).getBitmap();
				mClearDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, drawableWidth, drawableHeight, true));
			}
			if (mSearchDrawable != null) {
				Bitmap bitmap = ((BitmapDrawable) mSearchDrawable).getBitmap();
				mSearchDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, drawableWidth, drawableHeight, true));
			}
		}

		if (mEnableClearButton) {

			/* Set the bounds of the button */
			this.setCompoundDrawablesWithIntrinsicBounds(mSearchDrawable, null, mClearDrawable, null);

			// button should be hidden on first draw
			clearButtonHandler();

			//if the clear button is pressed, clear it. Otherwise do nothing
			setOnTouchListener((view, event) -> {
				if (AirAutoCompleteTextView.this.getCompoundDrawables()[2] == null || event.getAction() != MotionEvent.ACTION_UP)
					return false;

				if (event.getX() > AirAutoCompleteTextView.this.getWidth() - AirAutoCompleteTextView.this.getPaddingRight()
						- mClearDrawable.getIntrinsicWidth()) {
					AirAutoCompleteTextView.this.setText("");
					AirAutoCompleteTextView.this.clearButtonHandler();
				}
				return false;
			});

			addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					clearButtonHandler();
				}
			});
		}
	}

	private void clearButtonHandler() {
		if (!hasFocus() || TextUtils.isEmpty(getText().toString())) {
			/* Remove clear button */
			setCompoundDrawables(mSearchDrawable, null, null, null);
		}
		else {
			/* Add clear button */
			this.setCompoundDrawablesWithIntrinsicBounds(mSearchDrawable, null, mClearDrawable, null);
		}
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (mEnableClearButton) {
			clearButtonHandler();
		}
	}
}
