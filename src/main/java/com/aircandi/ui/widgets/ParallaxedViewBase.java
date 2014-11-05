package com.aircandi.ui.widgets;

import android.view.View;

import java.lang.ref.WeakReference;

public abstract class ParallaxedViewBase {

	protected WeakReference<View> mView;
	protected int                 mLastOffset; // NO_UCD (unused code)

	protected ParallaxedViewBase(View view) {
		this.mLastOffset = 0;
		this.mView = new WeakReference<View>(view);
	}

	public boolean is(View v) {
		return (v != null && mView != null && mView.get() != null && mView.get().equals(v));
	}

	public void setOffset(float offset) {
		View view = this.mView.get();
		if (view != null)
			view.setTranslationY(offset);
	}

	public void setView(View view) {
		this.mView = new WeakReference<View>(view);
	}

	public View getView() {
		return this.mView.get();
	}
}