package com.patchr.ui.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.patchr.R;
import com.patchr.components.FontManager;
import com.patchr.utilities.Colors;

public class PasswordEditText extends AppCompatEditText {

	private boolean  unmaskEnabled;
	private Drawable unmaskDrawable;

	public PasswordEditText(Context context) {
		this(context, null);
	}

	public PasswordEditText(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.editTextStyle);
	}

	public PasswordEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context);
	}

	private void initialize(Context context) {

		if (!isInEditMode()) {
			unmaskDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_action_watch_light);
			if (unmaskDrawable != null) {
				unmaskDrawable = DrawableCompat.wrap(unmaskDrawable);
				DrawableCompat.setTintMode(unmaskDrawable, PorterDuff.Mode.SRC_ATOP);
			}

			setCompoundDrawablesWithIntrinsicBounds(null, null, unmaskDrawable, null);
			setOnTouchListener(new View.OnTouchListener() {
				@Override public boolean onTouch(View v, MotionEvent event) {
					final int DRAWABLE_LEFT = 0;
					final int DRAWABLE_TOP = 1;
					final int DRAWABLE_RIGHT = 2;
					final int DRAWABLE_BOTTOM = 3;

					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (event.getRawX() >= (getRight() - getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
							unmaskAction(!unmaskEnabled);
							unmaskEnabled = !unmaskEnabled;
							return true;
						}
					}
					return false;
				}
			});
		}
	}

	private void unmaskAction(boolean unmask) {
		if (unmask) {
			DrawableCompat.setTint(unmaskDrawable, Colors.getColor(R.color.brand_primary));
			setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
					| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			FontManager.getInstance().setTypefaceLight(this);
		}
		else {
			DrawableCompat.setTint(unmaskDrawable, Color.TRANSPARENT);
			setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD
					| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			FontManager.getInstance().setTypefaceLight(this);
		}
	}
}
