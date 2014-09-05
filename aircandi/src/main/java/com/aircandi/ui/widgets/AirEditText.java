package com.aircandi.ui.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.ui.base.BaseActivity.SimpleTextWatcher;

public class AirEditText extends EditText {

	private Boolean mEnableClearButton = false;
	private Drawable mClearDrawable;

	@SuppressWarnings("ucd")
	public AirEditText(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirEditText(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.editTextStyle);
	}

	@SuppressWarnings("ucd")
	public AirEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context);
	}

	private void initialize(Context context) {

		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
			final Drawable[] drawables = getCompoundDrawables();
			if (drawables != null && drawables.length == 4) {
				mClearDrawable = drawables[2];

				if (mClearDrawable != null) {
					mEnableClearButton = true;
					Bitmap bitmap = ((BitmapDrawable) mClearDrawable).getBitmap();
					Integer drawableWidth = getResources().getDimensionPixelSize(R.dimen.drawable_width);
					Integer drawableHeight = drawableWidth;
					mClearDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, drawableWidth, drawableHeight, true));
				}
			}

			if (mEnableClearButton) {

				/* Set the bounds of the button */
				this.setCompoundDrawablesWithIntrinsicBounds(drawables != null ? drawables[0] : null, null, mClearDrawable, null);

				// button should be hidden on first draw
				clearButtonHandler();

				//if the clear button is pressed, clear it. Otherwise do nothing
				setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View view, MotionEvent event) {
						if (AirEditText.this.getCompoundDrawables()[2] == null || event.getAction() != MotionEvent.ACTION_UP)
							return false;

						if (event.getX() > AirEditText.this.getWidth() - AirEditText.this.getPaddingRight() - mClearDrawable.getIntrinsicWidth()) {
							AirEditText.this.setText("");
							AirEditText.this.clearButtonHandler();
						}
						return false;
					}
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
		if (!hasFocus() || getText().toString().equals("") || getText().toString().length() == 0) {
			/* Remove clear button */
			setCompoundDrawables(drawables[0], null, null, null);
		}
		else {
			/* Add clear button */
			this.setCompoundDrawablesWithIntrinsicBounds(drawables[0], null, mClearDrawable, null);
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
