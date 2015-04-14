package com.aircandi.ui;

import android.os.Bundle;

import com.aircandi.R;
import com.aircandi.ui.base.BaseActivity;

@SuppressWarnings("deprecation")
public class SettingsForm extends BaseActivity {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mCurrentFragment = new SettingsFragment();
		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.settings_form;
	}
}

