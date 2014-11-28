package com.aircandi.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AboutForm extends BaseActivity {

	private TextView mVersion;
	private TextView mCopyright;
	private String   mVersionName;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mVersion = (TextView) findViewById(R.id.version);
		mCopyright = (TextView) findViewById(R.id.copyright);
	}

	@Override
	public void bind(BindingMode mode) {
		draw(null);
	}

	@Override
	public void draw(View view) {

		final String year = new SimpleDateFormat("yyyy", Locale.US).format(Calendar.getInstance().getTime());
		final String company = StringManager.getString(R.string.name_company);
		final String copyrightSymbol = StringManager.getString(R.string.symbol_copyright);

		mVersionName = Patchr.getVersionName(this, AircandiForm.class);
		final String version = StringManager.getString(R.string.label_about_version) + ": " + mVersionName
				+ " (" + String.valueOf(Patchr.getVersionCode(this, AircandiForm.class)) + ")";

		final String copyright = copyrightSymbol + " " + year + " " + company;

		mVersion.setText(version);
		mCopyright.setText(copyright);

		if (Patchr.getInstance().getCurrentUser() != null
				&& Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Patchr.getInstance().getCurrentUser().developer != null
				&& Patchr.getInstance().getCurrentUser().developer) {
			((TextView) findViewById(R.id.install_id)).setText(Patchr.getInstance().getinstallId());
			((TextView) findViewById(R.id.install_type)).setText("Id type: " + Patchr.getInstance().getInstallType());
			((TextView) findViewById(R.id.install_date)).setText(DateTime.dateString(Patchr.getInstance().getInstallDate(), DateTime.DATE_FORMAT_DEFAULT));
			findViewById(R.id.holder_footer).setVisibility(View.VISIBLE);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.FORM_BACK);
	}

	@SuppressWarnings("ucd")
	public void onTermsButtonClick(View view) {
		Patchr.dispatch.route(this, Route.TERMS, null, null);
	}

	@SuppressWarnings("ucd")
	public void onPrivacyButtonClick(View view) {
		Patchr.dispatch.route(this, Route.PRIVACY, null, null);
	}

	@SuppressWarnings("ucd")
	public void onLegalButtonClick(View view) {
		Patchr.dispatch.route(this, Route.LEGAL, null, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.about_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
		super.onResume();
		bind(BindingMode.AUTO);
	}
}