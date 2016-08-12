package com.patchr.ui.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.patchr.BuildConfig;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.ContainerManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.AboutScreen;
import com.patchr.utilities.Colors;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.picasso.Picasso;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	protected ColorDrawable divider;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Load preferences layout */
		addPreferencesFromResource(R.xml.preferences);
		if (Utils.isDev()) {
			addPreferencesFromResource(R.xml.preferences_dev);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (getActivity() == null || getActivity().isFinishing()) return null;

		View root = super.onCreateView(inflater, container, savedInstanceState);

		/* Configure dividers to the thickness we want */
		if (root != null) {
			ListView list = (ListView) root.findViewById(android.R.id.list);
			Integer padding = UI.getRawPixelsForDisplayPixels(10.0f);
			list.setPadding(padding, 0, padding, padding);
			list.setDividerHeight(UI.getRawPixelsForDisplayPixels(1.0f));
		}

		initialize();
		initializeDev();
		return root;
	}

	@Override public void onResume() {
		super.onResume();
		setSummaries(getPreferenceScreen());
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_LOGIN) {
			if (resultCode == Constants.RESULT_USER_LOGGED_IN) {
				/*
				 * Restarts this activity using the same intent as used for the previous start.
				 */
				Intent originalIntent = getActivity().getIntent();
				originalIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
				getActivity().finish();
				getActivity().overridePendingTransition(0, 0);
				startActivity(originalIntent);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		/*
		 * Update the setting summaries when a shared pref is changed.
		 */
		Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
		else if (pref instanceof EditTextPreference) {
			if (((EditTextPreference) pref).getEditText().getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
				EditTextPreference etPref = (EditTextPreference) pref;
				String maskedPw = "";
				if (etPref.getText() != null) {
					for (int j = 0; j < etPref.getText().length(); j++) {
						maskedPw = maskedPw + "*";
					}
					pref.setSummary(maskedPw);
				}
			}
			else {
				EditTextPreference etPref = (EditTextPreference) pref;
				pref.setSummary(etPref.getText());
			}
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);

		/* If the user has clicked on a preference screen, set up the action bar */
		if (preference instanceof PreferenceScreen) {   // Notifications screen
			configureScreen((PreferenceScreen) preference);
		}

		return false;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize() {

		/* Listen for about click */
		Preference pref = findPreference("Pref_About");
		if (pref != null) {
			pref.setTitle("Version: " + BuildConfig.VERSION_NAME);
			pref.setSummary("Terms of Service, Privacy Policy, Licenses");

			pref.setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(getActivity(), AboutScreen.class);
				getActivity().startActivity(intent);
				AnimationManager.doOverridePendingTransition(getActivity(), TransitionType.FORM_TO);
				return true;
			});
		}

		/* Listen for signin/out click */
		pref = findPreference("Pref_Signin_Signout");
		if (pref != null) {
			pref.setTitle(StringManager.getString(R.string.pref_signout_title));
			pref.setSummary(StringManager.getString(R.string.pref_signout_summary));

			pref.setOnPreferenceClickListener(preference -> {
				UserManager.shared().logout();
				UI.routeLobby(Patchr.applicationContext);
				return true;
			});
		}

		/* Listen for feedback click */
		pref = findPreference("Pref_Feedback");
		if (pref != null) {
			pref.setOnPreferenceClickListener(preference -> {
				Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:feedback@patchr.com"));
				email.putExtra(Intent.EXTRA_SUBJECT, "Feedback for Patchr");
				startActivity(Intent.createChooser(email, "Send feedback using:"));
				return true;
			});
		}
	}

	private void initializeDev() {

		/* Listen for dev toggle */
		Preference pref = findPreference(com.patchr.objects.enums.Preference.ENABLE_DEV);
		if (pref != null) {
			Boolean enabled = Patchr.settings.getBoolean(com.patchr.objects.enums.Preference.ENABLE_DEV, false);
			enableDeveloper(enabled);

			pref.setOnPreferenceChangeListener((preference, newValue) -> {
				Boolean enabled1 = (Boolean) newValue;
				enableDeveloper(enabled1);
				return true;
			});
		}

		/* Listen for tag refresh click */
		final Preference prefTagRefresh = findPreference(com.patchr.objects.enums.Preference.TAG_REFRESH);
		if (prefTagRefresh != null) {
			prefTagRefresh.setSummary("Last refresh: "
				+ DateTime.dateString(ContainerManager.getInstance().getContainer().getLastRefreshTime(), DateTime.DATE_FORMAT_DEFAULT));
			prefTagRefresh.setOnPreferenceClickListener(preference -> {
				prefTagRefresh.setSummary("Refreshing...");
				ContainerManager.getInstance().refresh();
				prefTagRefresh.setSummary("Last refresh: "
					+ DateTime.dateString(ContainerManager.getInstance().getContainer().getLastRefreshTime(), DateTime.DATE_FORMAT_DEFAULT));
				return true;
			});
		}
	}

	private void enableDeveloper(Boolean enable) {
		findPreference(com.patchr.objects.enums.Preference.ENABLE_LOCATION_HIGH_ACCURACY).setEnabled(enable);
		findPreference(com.patchr.objects.enums.Preference.TAG_REFRESH).setEnabled(enable);
		findPreference(com.patchr.objects.enums.Preference.USE_STAGING_SERVICE).setEnabled(enable);
		Picasso.with(Patchr.applicationContext).setIndicatorsEnabled(enable);
		Picasso.with(Patchr.applicationContext).setLoggingEnabled(enable);
	}

	private void setSummaries(PreferenceGroup prefGroup) {
		/*
		 * Walk and set the current pref values in the UI
		 */
		for (int i = 0; i < prefGroup.getPreferenceCount(); i++) {
			Preference pref = prefGroup.getPreference(i);
			if (pref instanceof ListPreference) {
				ListPreference listPref = (ListPreference) pref;
				pref.setSummary(listPref.getEntry());
			}
			else if (pref instanceof EditTextPreference) {
				if (((EditTextPreference) pref).getEditText().getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
					EditTextPreference etPref = (EditTextPreference) pref;
					String maskedPw = "";
					if (etPref.getText() != null) {
						for (int j = 0; j < etPref.getText().length(); j++) {
							maskedPw = maskedPw + "*";
						}
						pref.setSummary(maskedPw);
					}
				}
				else {
					EditTextPreference etPref = (EditTextPreference) pref;
					pref.setSummary(etPref.getText());
				}
			}
			else if (pref instanceof PreferenceGroup) {
				setSummaries((PreferenceGroup) pref);
			}
		}
	}

	public void configureScreen(PreferenceScreen preferenceScreen) {
		/*
		 * Sets up the action bar for an {@link PreferenceScreen}
		 */
		final Dialog dialog = preferenceScreen.getDialog();

		if (dialog != null) {

			ViewGroup root = (ViewGroup) dialog.getWindow().getDecorView().getRootView();
			root.setBackgroundColor(Colors.getColor(R.color.white));

			/* Configure dividers to the thickness we want */
			ListView list = (ListView) root.findViewById(android.R.id.list);
			if (list != null) {
				list.setDividerHeight(UI.getRawPixelsForDisplayPixels(1.0f));
			}

			/* Insert toolbar at the top */
			ViewGroup target = (ViewGroup) root.getChildAt(0);
			Toolbar toolbar = (Toolbar) LayoutInflater.from(getActivity()).inflate(R.layout.view_actionbar_toolbar, target, false);
			TextView actionBarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
			actionBarTitle.setText(preferenceScreen.getTitle());
			@SuppressWarnings("deprecation") Drawable backArrow = getResources().getDrawable(R.drawable.ic_arrow_back_light);
			if (backArrow != null) {
				backArrow.setColorFilter(Colors.getColor(R.color.brand_primary), PorterDuff.Mode.SRC_ATOP);
				toolbar.setNavigationIcon(backArrow);
			}
			target.addView(toolbar, 0); // insert at top

			toolbar.setNavigationOnClickListener(v -> Dialogs.dismiss(dialog));
		}
	}
}