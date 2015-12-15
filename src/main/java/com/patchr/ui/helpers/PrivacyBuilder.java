package com.patchr.ui.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseEntityEdit;

@SuppressLint("Registered")
public class PrivacyBuilder extends BaseEntityEdit {

	private String      mOriginalPrivacy;
	private RadioGroup  mButtonGroupPrivacy;
	private RadioButton mButtonPublic;
	private RadioButton mButtonPrivate;
	private String      mPrivacy;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mOriginalPrivacy = extras.getString(Constants.EXTRA_PRIVACY);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mButtonPublic = (RadioButton) findViewById(R.id.button_public);
		mButtonPrivate = (RadioButton) findViewById(R.id.button_private);
		mButtonGroupPrivacy = (RadioGroup) findViewById(R.id.buttons_privacy);
	}

	@Override
	public void draw(View view) {
		mButtonGroupPrivacy.check(mOriginalPrivacy.equals(Constants.PRIVACY_PUBLIC)
		                          ? R.id.button_public
		                          : R.id.button_private);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onRadioButtonClicked(View view) {

		boolean checked = ((RadioButton) view).isChecked();
		switch (view.getId()) {
			case R.id.button_public:
				if (checked) {
					mPrivacy = Constants.PRIVACY_PUBLIC;
					mButtonPrivate.setChecked(false);
				}
				break;
			case R.id.button_private:
				if (checked) {
					mPrivacy = Constants.PRIVACY_PRIVATE;
					mButtonPublic.setChecked(false);
				}
				break;
		}
	}

	@Override
	public void onAccept() {
		gather();
		save();
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void gather() {}

	private void save() {
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PRIVACY, mPrivacy);
		setResultCode(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.privacy_builder;
	}
}