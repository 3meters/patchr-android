package com.patchr.ui.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.patchr.model.RealmEntity;
import com.patchr.utilities.Utils;

public class BaseView extends FrameLayout {

	public BaseView(Context context) {
		this(context, null, 0);
	}

	public BaseView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BaseView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void bind(RealmEntity entity) {}

	public void draw() {}

	protected void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	protected void setOrGone(TextView text, String value) {
		if (text != null) {
			text.setText(!TextUtils.isEmpty(value) ? Utils.fromHtml(value) : null);    // null becomes ""
			text.setVisibility(TextUtils.isEmpty(value) ? View.GONE : View.VISIBLE);
		}
	}
}
