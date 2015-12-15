package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.patchr.R;
import com.patchr.components.FontManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.objects.Entity;
import com.patchr.objects.User;
import com.patchr.ui.components.TextDrawable;
import com.patchr.utilities.UI;

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
			TextDrawable text = new TextDrawable(StringManager.getString(mPrefixResId)
					, (float) UI.getRawPixelsForScaledPixels(20f));
			TypedArray ta = getContext().obtainStyledAttributes(R.styleable.AppTheme);
			int color = ta.getColor(R.styleable.AppTheme_textColorSecondary, 0);
			ta.recycle();
			text.setTextColor(color);
			/* Bounding rectangle for draw location: left, top, right, bottom */
			text.setBounds(0, UI.getRawPixelsForScaledPixels(5f), (int) text.getTextWidth(), 0);
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
