package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.Patchr.ThemeTone;
import com.aircandi.R;
import com.aircandi.components.BusyManager;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.Document;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.components.ShotStateStore;
import com.aircandi.ui.widgets.BeaconPreference;
import com.aircandi.ui.widgets.ListPreferenceMultiSelect;
import com.aircandi.ui.widgets.LocationPreference;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

@SuppressWarnings("deprecation")
public class SettingsForm extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	protected BusyManager   mBusy;
	protected ColorDrawable mDivider; // NO_UCD (unused code)

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/*
		 * We need to set the theme so ActionBarSherlock behaves correctly on API < V14
		 * TODO: Switch over to using the preferenceStyle attribute for the current theme.
		 */
		setTheme();
		super.onCreate(savedInstanceState);

		/* Load preferences layout */
		addPreferencesFromResource(R.xml.preferences);
		if (Patchr.getInstance().getCurrentUser() != null
				&& Patchr.getInstance().getCurrentUser().developer != null
				&& Patchr.getInstance().getCurrentUser().developer) {
			addPreferencesFromResource(R.xml.preferences_dev);
		}

		final TypedValue resourceName = new TypedValue();
		if (getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			Patchr.themeTone = (String) resourceName.coerceToString();
		}

		initialize();
	}

	@SuppressLint("ResourceAsColor")
	private void initialize() {

		/* Set dividers */

		ListView list = (ListView) findViewById(android.R.id.list);

		if (Patchr.themeTone.equals(ThemeTone.DARK)) {
			int color = getResources().getColor(R.color.stroke_button_dark);
			int[] colors = {color, color, color};
			list.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
		}
		else {
			int color = getResources().getColor(R.color.stroke_button_light);
			int[] colors = {color, color, color};
			list.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
		}

		list.setDividerHeight(UI.getRawPixelsForDisplayPixels(0.5f));

		/* Configure action bar */

		if (getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setDisplayShowHomeEnabled(true);
			getActionBar().setTitle(StringManager.getString(R.string.form_title_preferences));
			getActionBar().setIcon(Patchr.applicationContext.getResources().getDrawable(R.drawable.img_logo_dark));
		}

		mBusy = new BusyManager(this);
		SwipeRefreshLayout swipeRefreshNotifications = (SwipeRefreshLayout) findViewById(R.id.swipe);
		if (swipeRefreshNotifications != null) {
			swipeRefreshNotifications.setProgressBackgroundColor(R.color.brand_primary);
			swipeRefreshNotifications.setColorSchemeResources(R.color.white);
			mBusy.setSwipeRefresh(swipeRefreshNotifications);
		}

		/* Listen for theme change */
		Preference pref = findPreference(StringManager.getString(R.string.pref_theme));
		if (pref != null) {
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Patchr.settingsEditor.putString(StringManager.getString(R.string.pref_theme), (String) newValue);
					Patchr.settingsEditor.commit();

					final Intent intent = getIntent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					finish();
					overridePendingTransition(0, 0);
					startActivity(intent);
					return false;
				}
			});
		}

		/* Listen for clear history click */
		pref = findPreference("Pref_Button_Clear_History");
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					/*
					 * Alert and then clear browse history
					 */
					return true; // we handled it
				}
			});
		}

		/* Listen for about click */
		pref = findPreference("Pref_About");
		if (pref != null) {
			pref.setTitle("Version: " + Patchr.getVersionName(this, SettingsForm.class));
			pref.setSummary("Terms of Service, Privacy Policy, Licenses");

			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					DownloadManager.getInstance().getSnapshot();
					Patchr.dispatch.route(SettingsForm.this, Route.ABOUT, null, null, null);

					return true;
				}
			});
		}

		/* Listen for feedback click */
		pref = findPreference("Pref_Feedback");
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Patchr.dispatch.route(SettingsForm.this, Route.FEEDBACK, null, null, null);
					return true;
				}
			});
		}

		/* Listen for tutorial reset click */
		pref = findPreference("Pref_Reset_Tutorials");
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					ShotStateStore.resetTooltips(Patchr.applicationContext);
					UI.showToastNotification("Tooltips restored", Toast.LENGTH_SHORT);
					return true;
				}
			});
		}

		/* Listen for signin/out click */
		pref = findPreference("Pref_Signin_Signout");
		if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
			pref.setTitle(StringManager.getString(R.string.pref_signin_title));
			pref.setSummary(StringManager.getString(R.string.pref_signin_summary));
		}
		else {
			pref.setTitle(StringManager.getString(R.string.pref_signout_title));
			pref.setSummary(StringManager.getString(R.string.pref_signout_summary));
		}
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
					Patchr.dispatch.route(SettingsForm.this, Route.SIGNIN, null, null, null);
				}
				else {
					mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_signing_out);
					Patchr.dispatch.route(SettingsForm.this, Route.SIGNOUT, null, null, null);
				}
				return true;
			}
		});

		/*
		 * Init the dev preferences
		 */
		initializeDev();
	}

	private void initializeDev() {

		/* Listen for dev toggle */
		Preference pref = findPreference(StringManager.getString(R.string.pref_enable_dev));
		if (pref != null) {
			Boolean enabled = Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false);
			enableDeveloper(enabled);

			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Boolean enabled = (Boolean) newValue;
					enableDeveloper(enabled);
					return true;
				}
			});
		}

		/* Listen for tag refresh click */
		final Preference prefTagRefresh = findPreference(StringManager.getString(R.string.pref_tag_refresh));
		if (prefTagRefresh != null) {
			prefTagRefresh.setSummary("Last refresh: "
					+ DateTime.dateString(Patchr.getInstance().getContainer().getLastRefreshTime(), DateTime.DATE_FORMAT_DEFAULT));
			prefTagRefresh.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					prefTagRefresh.setSummary("Refreshing...");
					Patchr.getInstance().getContainer().refresh();
					prefTagRefresh.setSummary("Last refresh: "
							+ DateTime.dateString(Patchr.getInstance().getContainer().getLastRefreshTime(), DateTime.DATE_FORMAT_DEFAULT));
					return true;
				}
			});
		}
	}

	private void enableDeveloper(Boolean enable) {
		findPreference(StringManager.getString(R.string.pref_testing_screen)).setEnabled(enable);
		findPreference(StringManager.getString(R.string.pref_tag_refresh)).setEnabled(enable);
		Patchr.tracker.enableDeveloper(enable);
		DownloadManager.getInstance().setDebugging(enable);
	}

	private void handleAnonymous() {
		/* Hide notification item if anonymous */
		Preference pref = findPreference("Pref_Notifications_Screen");
		if (pref != null) {
			pref.setShouldDisableView(true);
			if (Patchr.getInstance().getCurrentUser() != null) {
				pref.setEnabled(!(Patchr.getInstance().getCurrentUser().isAnonymous()));
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				Intent originalIntent = getIntent();
				originalIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
				finish();
				overridePendingTransition(0, 0);
				startActivity(originalIntent);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);

		if (pref instanceof ListPreferenceMultiSelect) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getValue().replace(ListPreferenceMultiSelect.DEFAULT_SEPARATOR, "|"));
		}
		else if (pref instanceof ListPreference) {
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
		if (preference instanceof PreferenceScreen) {
			initializeActionBar((PreferenceScreen) preference);
		}

		return false;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	private void setSummaries(PreferenceGroup prefGroup) {
		/*
		 * Walk and set the current pref values in the UI
		 */
		for (int i = 0; i < prefGroup.getPreferenceCount(); i++) {
			Preference pref = prefGroup.getPreference(i);
			if (pref instanceof LocationPreference) {
				LocationPreference listPref = (LocationPreference) pref;
				String jsonLocation = listPref.getValue();
				if (jsonLocation != null) {
					Document location = (Document) Json.jsonToObject(jsonLocation, Json.ObjectType.DOCUMENT);
					if (location != null) {
						pref.setSummary(location.name);
					}
				}
			}
			if (pref instanceof BeaconPreference) {
				BeaconPreference listPref = (BeaconPreference) pref;
				String jsonBeacon = listPref.getValue();
				if (jsonBeacon != null) {
					Document beacon = (Document) Json.jsonToObject(jsonBeacon, Json.ObjectType.DOCUMENT);
					if (beacon != null && beacon.data != null) {
						pref.setSummary((String) beacon.data.get("ssid"));
					}
				}
			}
			else if (pref instanceof ListPreferenceMultiSelect) {
				ListPreference listPref = (ListPreference) pref;
				pref.setSummary(listPref.getValue().replace(ListPreferenceMultiSelect.DEFAULT_SEPARATOR, "|"));
			}
			else if (pref instanceof ListPreference) {
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

	private void setTheme() {
		final String prefTheme = Patchr.settings.getString(StringManager.getString(R.string.pref_theme)
				, StringManager.getString(R.string.pref_theme_default));

		if (prefTheme.equals("aircandi_theme_snow")) {
			setTheme(R.style.aircandi_theme_snow);
		}
		else {
			setTheme(R.style.aircandi_theme_midnight);
		}
	}

	private void clearReferences() {
		Activity currentActivity = Patchr.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.equals(this)) {
			Patchr.getInstance().setCurrentActivity(null);
		}
	}

	/**
	 * Sets up the action bar for an {@link PreferenceScreen}
	 */
	@SuppressLint("NewApi")
	public static void initializeActionBar(PreferenceScreen preferenceScreen) {

		final Dialog dialog = preferenceScreen.getDialog();

		if (dialog != null) {

			/* Inialize the action bar */
			dialog.getActionBar().setDisplayHomeAsUpEnabled(true);
			dialog.getActionBar().setDisplayShowHomeEnabled(true);
			dialog.getActionBar().setIcon(Patchr.applicationContext.getResources().getDrawable(R.drawable.img_logo_dark));

			/*
			 * Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are
			 * dialogs which swallow events instead of passing to the activity Related Issue:
			 * https://code.google.com/p/android/issues/detail?id=4611
			 */
			View homeBtn = dialog.findViewById(android.R.id.home);

			if (homeBtn != null) {

				OnClickListener dismissDialogClickListener = new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				};

				/* Prepare yourselves for some hacky programming */
				ViewParent homeBtnContainer = homeBtn.getParent();

				/* The home button is an ImageView inside a FrameLayout */
				if (homeBtnContainer instanceof FrameLayout) {
					ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

					if (containerParent instanceof LinearLayout) {
						/* This view also contains the title text, set the whole view as clickable */
						((LinearLayout) containerParent).setOnClickListener(dismissDialogClickListener);
					}
					else {
						/* Just set it on the home button */
						((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
					}
				}
				else {
					/* The 'If all else fails' default case */
					homeBtn.setOnClickListener(dismissDialogClickListener);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			/*
			 * We aren't using Routing because pref activity doesn't derive
			 * from BaseActivity.
			 */
			setResult(Activity.RESULT_CANCELED);
			finish();
			Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_TO_PAGE);
		}
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onStart() {
		super.onStart();
		Patchr.tracker.activityStart(this);
		handleAnonymous();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.d(this, "Activity resuming");
		Patchr.getInstance().setCurrentActivity(this);
		setSummaries(getPreferenceScreen());
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(this, "Activity pausing");
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		if (mBusy != null) {
			mBusy.onPause();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Patchr.tracker.activityStop(this);
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "Activity destroying: contextId: " + this.hashCode());
		super.onDestroy();
		clearReferences();
	}
}

