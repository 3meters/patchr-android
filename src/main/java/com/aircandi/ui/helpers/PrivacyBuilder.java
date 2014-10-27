package com.aircandi.ui.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEntityEdit;

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
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.BUILDER_TO_FORM);
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
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.BUILDER_TO_FORM);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.privacy_builder;
	}
}