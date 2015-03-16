package com.aircandi.ui.components;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.aircandi.Constants;
import com.aircandi.Patchr;

public class ListController extends UiController {

	private FloatingActionController mFloatingActionController;

	public ListController() {}

	public FloatingActionController getFloatingActionController() {
		return mFloatingActionController;
	}

	public ListController setFloatingActionController(FloatingActionController floatingActionController) {
		mFloatingActionController = floatingActionController;
		return this;
	}

	public static void position(final View view, final View header, final Integer headerHeightProjected) {

		if (view != null && header != null) {

			ViewTreeObserver vto = header.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {

					if (Patchr.getInstance().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						int headerHeight = (headerHeightProjected != null)
						                   ? headerHeightProjected
						                   : header.getHeight();
						params.topMargin = headerHeight;
						view.setLayoutParams(params);
					}
					else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						view.setLayoutParams(params);
					}

					if (Constants.SUPPORTS_JELLY_BEAN) {
						header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
					else {
						header.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
	}
}
