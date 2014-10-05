package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.User;
import com.aircandi.ui.components.TextDrawable;
import com.aircandi.utilities.UI;

public class AirTokenCompleteTextView extends TokenCompleteTextView {

	private Integer mPrefixResId = 0;

	private Integer mTokenLayoutResId;

	@SuppressWarnings("ucd")
	public AirTokenCompleteTextView(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTokenCompleteTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	@SuppressWarnings("ucd")
	public AirTokenCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		if (attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AirTokenCompleteTextView, defStyle, 0);
			mPrefixResId = ta.getResourceId(R.styleable.AirTokenCompleteTextView_prefix, 0);
			ta.recycle();
		}

		if (!isInEditMode()) {
			setTypeface();
			initialize();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {
		if (mPrefixResId != 0) {
			TextDrawable text = new TextDrawable(StringManager.getString(mPrefixResId));
			TypedArray ta = getContext().obtainStyledAttributes(R.styleable.AircandiTheme);
			int color = ta.getColor(R.styleable.AircandiTheme_textColorSecondary, 0);
			ta.recycle();
			text.setTextColor(color);
			text.setTextSize((float) UI.getRawPixelsForScaledPixels(20f));
			text.setBounds(0, 0, (int) text.getTextWidth(), 0);
			this.setCompoundDrawables(text, null, null, null);
		}
	}

	public void expand(Boolean expand) {
		handleFocus(expand);
	}

	@Override
	protected View getViewForObject(Object object) {

		final Entity entity = (Entity) object;
		Logger.v(this, "Building view: " + entity.name);

		EntityView view = new EntityView(getContext());
		view.setLayout(mTokenLayoutResId != null ? mTokenLayoutResId : R.layout.widget_token_view);
		view.setAnimateDisabled(true);
		view.setParentView(this);
		view.initialize();
		view.databind(entity);

		return view;
	}

	@Override
	protected Object defaultObject(String s) {
		Entity entity = new User();
		entity.name = "Duh";
		return entity;
	}

	protected void setTypeface() {
		FontManager.getInstance().setTypefaceLight(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
	public void setTokenLayoutResId(Integer tokenLayoutResId) {
		mTokenLayoutResId = tokenLayoutResId;
	}
}
