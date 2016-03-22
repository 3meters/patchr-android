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
	/*
	 * Only used by message edit.
	 */
	private Integer prefixResId = 0;

	private Integer tokenLayoutResId;

	public AirTokenCompleteTextView(Context context) {
		this(context, null);
	}

	public AirTokenCompleteTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirTokenCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

		if (!isInEditMode()) {
			setTypeface();
			initialize();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {
		if (prefixResId != 0) {
			TextDrawable text = new TextDrawable(StringManager.getString(prefixResId), (float) UI.getRawPixelsForScaledPixels(20f));
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

	@Override protected View getViewForObject(Object object) {

		final Entity entity = (Entity) object;
		Logger.v(this, "Building view: " + entity.name);

//		EntityView view = new EntityView(getContext());
//
//		view.setLayout(tokenLayoutResId != null ? tokenLayoutResId : R.layout.widget_token_view);
//		view.setAnimateDisabled(true);
//		view.setParentView(this);
//		view.initialize();
//		view.databind(entity);

		return null;
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
		this.tokenLayoutResId = tokenLayoutResId;
	}
}
