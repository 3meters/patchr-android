package com.patchr.ui.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

import com.patchr.R;
import com.patchr.ui.components.SimpleTextWatcher;

public class ClearableEditText extends EditText {

	private boolean  enableClearButton;
	private Drawable clearDrawable;

	public ClearableEditText(Context context) {
		this(context, null);
	}

	public ClearableEditText(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.editTextStyle);
	}

	public ClearableEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {

		if (!isInEditMode()) {
			final Drawable[] drawables = getCompoundDrawables();
			if (drawables.length == 4) {
				clearDrawable = drawables[2];

				if (clearDrawable != null) {
					enableClearButton = true;
					Bitmap bitmap = ((BitmapDrawable) clearDrawable).getBitmap();
					Integer drawableWidth = getResources().getDimensionPixelSize(R.dimen.drawable_width);
					@SuppressWarnings("SuspiciousNameCombination") Integer drawableHeight = drawableWidth;
					clearDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, drawableWidth, drawableHeight, true));
				}
			}

			if (enableClearButton) {

				/* Set the bounds of the button */
				this.setCompoundDrawablesWithIntrinsicBounds(drawables[0], null, clearDrawable, null);

				// button should be hidden on first draw
				clearButtonHandler();

				//if the clear button is pressed, clear it. Otherwise do nothing
				setOnTouchListener((view, event) -> {
					if (ClearableEditText.this.getCompoundDrawables()[2] == null || event.getAction() != MotionEvent.ACTION_UP)
						return false;

					if (event.getX() > ClearableEditText.this.getWidth() - ClearableEditText.this.getPaddingRight() - clearDrawable.getIntrinsicWidth()) {
						ClearableEditText.this.setText("");
						ClearableEditText.this.clearButtonHandler();
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
	}

	private void clearButtonHandler() {
		final Drawable[] drawables = getCompoundDrawables();
		if (!hasFocus() || TextUtils.isEmpty(getText().toString())) {
			/* Remove clear button */
			setCompoundDrawables(drawables[0], null, null, null);
		}
		else {
			/* Add clear button */
			this.setCompoundDrawablesWithIntrinsicBounds(drawables[0], null, clearDrawable, null);
		}
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (enableClearButton) {
			clearButtonHandler();
		}
	}
}
