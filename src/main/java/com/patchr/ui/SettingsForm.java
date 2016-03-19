package com.patchr.ui;

import android.os.Bundle;

import com.patchr.R;

@SuppressWarnings("deprecation")
public class SettingsForm extends BaseActivity {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		currentFragment = new SettingsFragment();
		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, currentFragment)
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

