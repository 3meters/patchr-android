package com.patchr.components;

import android.app.Activity;

import com.patchr.R;
import com.patchr.objects.enums.TransitionType;

public class AnimationManager {

	public static Integer DURATION_LONG   = 1000;
	public static Integer DURATION_MEDIUM = 500;
	public static Integer DURATION_SHORT  = 200;

	public static void doOverridePendingTransition(Activity activity, Integer transitionType) {
		/*
		 * Default android animations are used unless overridden here.
		 */
		if (transitionType == TransitionType.VIEW_TO) {
			activity.overridePendingTransition(R.anim.fade_in_short, R.anim.hold);
		}
		else if (transitionType == TransitionType.VIEW_BACK) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_out_short);
		}
		else if (transitionType == TransitionType.FORM_TO) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_zoom_out);
		}
		else if (transitionType == TransitionType.FORM_BACK) {
			activity.overridePendingTransition(R.anim.fade_zoom_in, R.anim.slide_out_right);
		}
		else if (transitionType == TransitionType.BUILDER_TO) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}
		else if (transitionType == TransitionType.BUILDER_BACK) {
			activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
		else if (transitionType == TransitionType.DIALOG_TO) {
			activity.overridePendingTransition(R.anim.fade_zoom_in, R.anim.hold);
		}
		else if (transitionType == TransitionType.DIALOG_BACK) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_zoom_out);
		}
	}
}
