package com.patchr.ui.views;

import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

public class BaseView {

	protected void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	protected void setOrGone(TextView text, String value) {
		text.setText(!TextUtils.isEmpty(value) ? Html.fromHtml(value) : null);    // null becomes ""
		text.setVisibility(TextUtils.isEmpty(value) ? View.GONE : View.VISIBLE);
	}
}
