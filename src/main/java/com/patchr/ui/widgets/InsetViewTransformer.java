package com.patchr.ui.widgets;

import android.view.View;

import com.flipboard.bottomsheet.BaseViewTransformer;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.patchr.R;

/**
 * Created by jaymassena on 2/4/16.
 */
public class InsetViewTransformer extends BaseViewTransformer {

	public static final float MAX_DIM_ALPHA = 0.4f;

	@Override
	public void transformView(float translation, float maxTranslation, float peekedTranslation, BottomSheetLayout parent, View view) {
		float progress = Math.min(translation / peekedTranslation, 1);
		float scale = (1 - progress) + progress * 0.9f;
		view.setScaleX(scale);
		view.setScaleY(scale);

		if (translation == 0 || translation == parent.getHeight()) {
			parent.setBackgroundColor(0);
			ensureLayer(view, View.LAYER_TYPE_NONE);
		}
		else {
			parent.setBackgroundColor(view.getResources().getColor(R.color.brand_accent));
			ensureLayer(view, View.LAYER_TYPE_HARDWARE);
		}

		float translationToTop = -(view.getHeight() * (1 - scale)) / 2;
		view.setTranslationY(translationToTop + progress * 20 * view.getContext().getResources().getDisplayMetrics().density);
	}

	@Override
	public float getDimAlpha(float translation, float maxTranslation, float peekedTranslation, BottomSheetLayout parent, View view) {
		float progress = translation / maxTranslation;
		return progress * MAX_DIM_ALPHA;
	}

	private void ensureLayer(View view, int layerType) {
		if (view.getLayerType() != layerType) {
			view.setLayerType(layerType, null);
		}
	}
}