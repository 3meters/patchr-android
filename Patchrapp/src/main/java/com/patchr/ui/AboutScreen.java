package com.patchr.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.patchr.BuildConfig;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.NotificationManager;
import com.patchr.components.StringManager;
import com.patchr.objects.enums.TransitionType;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AboutScreen extends BaseScreen {

	private TextView versionView;
	private TextView copyrightView;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {

		Integer id = view.getId();
		if (id == R.id.terms_button) {
			termsAction();
		}
		else if (id == R.id.legal_button) {
			legalAction();
		}
		else if (id == R.id.privacy_policy_button) {
			privacyPolicyAction();
		}
	}

	private void termsAction() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	private void privacyPolicyAction() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	private void legalAction() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(StringManager.getString(R.string.url_legal)));
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		this.actionBarTitle.setText(R.string.screen_title_about_form);
		versionView = (TextView) findViewById(R.id.version);
		copyrightView = (TextView) findViewById(R.id.copyright);
	}

	private void bind() {

		final String year = new SimpleDateFormat("yyyy", Locale.US).format(Calendar.getInstance().getTime());
		final String company = StringManager.getString(R.string.name_company);
		final String copyrightSymbol = StringManager.getString(R.string.symbol_copyright);

		String versionName = BuildConfig.VERSION_NAME;
		final String version = String.format("Version: %1$s (%2$s)", versionName, String.valueOf(BuildConfig.VERSION_CODE));
		final String copyright = copyrightSymbol + " " + year + " " + company;

		this.versionView.setText(version);
		this.copyrightView.setText(copyright);

		if (Utils.devModeEnabled()) {
			UI.setTextView(findViewById(R.id.install_id), NotificationManager.installId);
			UI.setTextView(findViewById(R.id.install_date), "Install date: " + DateTime.dateString(NotificationManager.installDate, DateTime.DATE_FORMAT_DEFAULT));
			UI.setVisibility(findViewById(R.id.holder_footer), View.VISIBLE);
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_about;
	}
}