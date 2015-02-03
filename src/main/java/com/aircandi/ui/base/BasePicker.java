package com.aircandi.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBind.BindingMode;
import com.aircandi.objects.Entity;
import com.aircandi.objects.TransitionType;
import com.aircandi.utilities.Json;

public abstract class BasePicker extends Activity {

	protected String mPrefTheme;
	protected Entity mEntity;

	public void unpackIntent() {

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Logger.d(this, "Picker activity created");

		setTheme(true, false);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

		super.setContentView(getLayoutId());
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			unpackIntent();
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
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
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
			themeId = R.style.aircandi_theme_dialog_dark;
			if (mPrefTheme.equals("aircandi_theme_snow")) {
				themeId = R.style.aircandi_theme_dialog_light;
			}
		}
		else if (isTransparent) {
			themeId = R.style.aircandi_theme_midnight_transparent;
			if (mPrefTheme.equals("aircandi_theme_snow")) {
				themeId = R.style.aircandi_theme_snow_transparent;
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