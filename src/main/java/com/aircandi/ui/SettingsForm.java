package com.aircandi.ui;

import com.aircandi.R;
import com.aircandi.ui.base.BaseActivity;

@SuppressWarnings("deprecation")
public class SettingsForm extends BaseActivity {

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void setCurrentFragment(String fragmentType) {
		mCurrentFragment = new SettingsFragment();
		getFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.settings_form;
	}
}

