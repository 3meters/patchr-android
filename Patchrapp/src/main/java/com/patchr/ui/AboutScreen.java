package com.patchr.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.StringManager;
import com.patchr.objects.enums.TransitionType;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AboutScreen extends BaseScreen {

	private TextView version;
	private String   versionName;
	private TextView copyright;

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

	public void termsAction() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	public void privacyPolicyAction() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
		startActivity(intent);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	public void legalAction() {
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
		version = (TextView) findViewById(R.id.version);
		copyright = (TextView) findViewById(R.id.copyright);
	}

	public void bind() {

		final String year = new SimpleDateFormat("yyyy", Locale.US).format(Calendar.getInstance().getTime());
		final String company = StringManager.getString(R.string.name_company);
		final String copyrightSymbol = StringManager.getString(R.string.symbol_copyright);

		versionName = Patchr.getVersionName(this, MainScreen.class);
		final String version = StringManager.getString(R.string.label_about_version) + ": " + versionName
			+ " (" + String.valueOf(Patchr.getVersionCode(this, MainScreen.class)) + ")";

		final String copyright = copyrightSymbol + " " + year + " " + company;

		this.version.setText(version);
		this.copyright.setText(copyright);

		if (Utils.devModeEnabled()) {
			UI.setTextView(findViewById(R.id.install_id), Patchr.getInstance().getinstallId());
			UI.setTextView(findViewById(R.id.install_type), "Id type: " + Patchr.getInstance().getInstallType());
			UI.setTextView(findViewById(R.id.install_date), "Install date: " + DateTime.dateString(Patchr.getInstance().getInstallDate(), DateTime.DATE_FORMAT_DEFAULT));
			UI.setVisibility(findViewById(R.id.holder_footer), View.VISIBLE);
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_about;
	}
}