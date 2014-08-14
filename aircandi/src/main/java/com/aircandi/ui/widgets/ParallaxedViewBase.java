package com.aircandi.ui.widgets;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

import com.aircandi.Constants;

import java.lang.ref.WeakReference;

public abstract class ParallaxedViewBase {

	protected WeakReference<View> mView;
	protected int                 mLastOffset; // NO_UCD (unused code)

	abstract protected void translatePreICS(View view, float offset);

	protected ParallaxedViewBase(View view) {
		this.mLastOffset = 0;
		this.mView = new WeakReference<View>(view);
	}

	public boolean is(View v) {
		return (v != null && mView != null && mView.get() != null && mView.get().equals(v));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setOffset(float offset) {
		View view = this.mView.get();
		if (view != null)
			if (Constants.SUPPORTS_HONEYCOMB) {
				view.setTranslationY(offset);
			}
			else {
				translatePreICS(view, offset);
			}
	}

	public void setView(View view) {
		this.mView = new WeakReference<View>(view);
	}

	public View getView() {
		return this.mView.get();
	}
}