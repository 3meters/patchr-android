package com.patchr.ui.helpers;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.patchr.components.AnimationManager;
import com.patchr.objects.TransitionType;

public abstract class BasePicker extends AppCompatActivity {

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.setContentView(getLayoutId());
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		initialize(savedInstanceState);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onBackPressed() {
		onCancel(true);
	}

	public void onCancelButtonClick(View view) {
		onCancel(true);
	}

	public void onCancel(Boolean force) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(Bundle savedInstanceState) {
		/*
		 * Perform all setup that should only happen once and
		 * only after the view tree is available.
		 */
	}

	protected int getLayoutId() {
		return 0;
	}
}