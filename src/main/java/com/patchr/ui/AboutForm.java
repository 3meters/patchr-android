package com.patchr.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.StringManager;
import com.patchr.objects.BindingMode;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Utils;

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

		if (Utils.devModeEnabled()) {
			String serviceUrl = Constants.serviceUrl();
			((TextView) findViewById(R.id.service_url)).setText(serviceUrl);
			((TextView) findViewById(R.id.install_id)).setText(Patchr.getInstance().getinstallId());
			((TextView) findViewById(R.id.install_type)).setText("Id type: " + Patchr.getInstance().getInstallType());
			((TextView) findViewById(R.id.install_date)).setText("Install date: " + DateTime.dateString(Patchr.getInstance().getInstallDate(), DateTime.DATE_FORMAT_DEFAULT));
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
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_BACK);
	}

	@SuppressWarnings("ucd")
	public void onTermsButtonClick(View view) {
		Patchr.router.route(this, Route.TERMS, null, null);
	}

	@SuppressWarnings("ucd")
	public void onPrivacyButtonClick(View view) {
		Patchr.router.route(this, Route.PRIVACY, null, null);
	}

	@SuppressWarnings("ucd")
	public void onLegalButtonClick(View view) {
		Patchr.router.route(this, Route.LEGAL, null, null);
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