package com.patchr.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.interfaces.IBind.BindingMode;
import com.patchr.objects.TransitionType;

public abstract class BasePicker extends Activity {

	protected String mPrefTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Logger.d(this, "Picker activity created");

		setTheme(true, false);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

		super.setContentView(getLayoutId());
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize(savedInstanceState);
		}
	}

	public void initialize(Bundle savedInstanceState) {}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onCancelButtonClick(View view) {
		onCancel(true);
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	@Override
	public void onBackPressed() {
		onCancel(true);
	}

	/*--------------------------------------------------------------------------------------------
	 * UI
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(BindingMode mode) {}

	public void setTheme(Boolean isDialog, Boolean isTransparent) {
		mPrefTheme = Patchr.settings.getString(StringManager.getString(R.string.pref_theme), StringManager.getString(R.string.pref_theme_default));
		/*
		 * Need to use application context so our app level themes and attributes are available to actionbarsherlock
		 */
		Integer themeId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		if (isDialog) {
			themeId = R.style.patchr_theme_dialog_dark;
			if (mPrefTheme.equals("patchr_theme_snow")) {
				themeId = R.style.patchr_theme_dialog_light;
			}
		}
		else if (isTransparent) {
			themeId = R.style.patchr_theme_midnight_transparent;
			if (mPrefTheme.equals("patchr_theme_snow")) {
				themeId = R.style.patchr_theme_snow_transparent;
			}
		}

		setTheme(themeId);
	}

	protected int getLayoutId() {
		return 0;
	}

	public void setResultCode(int resultCode) {
		setResult(resultCode);
		Patchr.resultCode = resultCode;
	}

	public void setResultCode(int resultCode, Intent intent) {
		setResult(resultCode, intent);
		Patchr.resultCode = resultCode;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			bind(BindingMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
}