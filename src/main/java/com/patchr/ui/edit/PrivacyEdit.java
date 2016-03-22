package com.patchr.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.objects.TransitionType;
import com.patchr.ui.edit.BaseEdit;

@SuppressLint("Registered")
public class PrivacyEdit extends BaseEdit {

	private String      originalPrivacy;
	private RadioGroup  buttonGroupPrivacy;
	private RadioButton buttonPublic;
	private RadioButton buttonPrivate;
	private String      privacy;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;

		getMenuInflater().inflate(R.menu.menu_submit, menu);
		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onRadioButtonClicked(View view) {

		boolean checked = ((RadioButton) view).isChecked();
		switch (view.getId()) {
			case R.id.button_public:
				if (checked) {
					privacy = Constants.PRIVACY_PUBLIC;
					buttonPrivate.setChecked(false);
				}
				break;
			case R.id.button_private:
				if (checked) {
					privacy = Constants.PRIVACY_PRIVATE;
					buttonPublic.setChecked(false);
				}
				break;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			originalPrivacy = extras.getString(Constants.EXTRA_PRIVACY);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		buttonPublic = (RadioButton) findViewById(R.id.button_public);
		buttonPrivate = (RadioButton) findViewById(R.id.button_private);
		buttonGroupPrivacy = (RadioGroup) findViewById(R.id.buttons_privacy);
	}

	@Override protected int getLayoutId() {
		return R.layout.privacy_builder;
	}

	@Override public void submitAction() {
		save();
	}

	@Override public void cancelAction(Boolean force) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
	}

	public void bind() {
		buttonGroupPrivacy.check(originalPrivacy.equals(Constants.PRIVACY_PUBLIC)
		                         ? R.id.button_public
		                         : R.id.button_private);
	}

	private void save() {
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PRIVACY, privacy);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_BACK);
	}
}