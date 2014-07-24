package com.aircandi.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;

public class HelpForm extends BaseActivity {

	@SuppressWarnings("unused")
	private Integer	mHelpResId;

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onHelpClick(View view) {
		onCancel(false);
	}

	@Override
	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.HELP_TO_PAGE);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected Boolean isTransparent() {
		return true;
	}

	@Override
	protected int getLayoutId() {
		Integer mHelpResId = 0;
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mHelpResId = extras.getInt(Constants.EXTRA_HELP_ID);
		}
		return mHelpResId;
	}
}