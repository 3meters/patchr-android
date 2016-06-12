package com.patchr.ui;

import android.os.Bundle;

import com.patchr.R;
import com.patchr.ui.fragments.SettingsFragment;

@SuppressWarnings("deprecation")
public class SettingsScreen extends BaseScreen {

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		this.actionBarTitle.setText(R.string.screen_title_settings_form);

		currentFragment = new SettingsFragment();
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, currentFragment)
				.commit();
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_settings;
	}
}

