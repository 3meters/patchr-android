package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressLint("Registered")
public class PlaceForm extends BaseEntityForm {

	private Boolean mDoUpsize;
	protected final PackageReceiver mPackageReceiver = new PackageReceiver();
	protected final List<String>    mPackageInstalls = new ArrayList<String>();
	protected       Boolean         mWaitForContent  = true;
	protected       Boolean         mAutoWatch       = false;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			/*
			 * We handle upsizing if the place entity we want to browse isn't
			 * stored in the service yet.
			 */
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
			mDoUpsize = extras.getBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC);
			mAutoWatch = extras.getBoolean(Constants.EXTRA_AUTO_WATCH);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;

		/* Package receiver */
		final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addDataScheme("package");
		registerReceiver(mPackageReceiver, filter);
	}

	@Override
	public void bind(final BindingMode mode) {
		if (mDoUpsize && mEntity != null) {
			mDoUpsize = false;
			upsize();
		}
		else {
			super.bind(mode);
		}
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);

		if (mAutoWatch && mEntity != null) {
			Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null) {
			    /* User is not already watching this */
				if (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
						, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
					watch(true);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAdd(Bundle extras) {
		Aircandi.dispatch.route(this, Route.NEW_PICKER, mEntity, null, null);
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.place_help);
		Aircandi.dispatch.route(this, Route.HELP, null, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {

		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			Integer messageResId = R.string.alert_signin_message_place_tune;
			Dialogs.signinRequired(this, messageResId);
			return;
		}

		if (Aircandi.getInstance().getMenuManager().canUserAdd(mEntity)) {
			Aircandi.dispatch.route(this, Route.TUNE, mEntity, null, null);
			return;
		}

		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
	    /*
	     * Refresh the form because something new has been added to it
		 * like a comment or picture.
		 */
		if (event.message.action.toEntity != null
				&& mEntityId.equals(event.message.action.toEntity.id)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void draw() {
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */

		mFirstDraw = false;
		setActivityTitle(mEntity.name);

		/*
		 * Drawing is broken up so sections can be selectively overridden.
		 */

		/* Photo overlayed with info */
		drawBanner();

		/* Description and address */
		drawBody();

		/* Links to entities */
		drawShortcuts();

		/* Creator and/or editor */
		drawUsers();

		/* Buttons */
		drawButtons();

		/* Visibility */
		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	protected void drawBanner() {

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) findViewById(R.id.entity_photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);

		/* Primary candi image */

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiView.databind(mEntity, options);
		}
		else {
			UI.setVisibility(photoView, View.GONE);
			if (photoView != null) {
				Photo photo = mEntity.getPhoto();
				UI.drawPhoto(photoView, photo);
				if (Type.isFalse(photo.usingDefault)) {
					photoView.setClickable(true);
				}
				UI.setVisibility(photoView, View.VISIBLE);
			}

			UI.setVisibility(name, View.GONE);
			if (name != null) {
				name.setText(null);
				if (!TextUtils.isEmpty(mEntity.name)) {
					name.setText(Html.fromHtml(mEntity.name));
					UI.setVisibility(name, View.VISIBLE);
				}
			}

			UI.setVisibility(subtitle, View.GONE);
			if (subtitle != null) {
				subtitle.setText(null);
				if (mEntity.subtitle != null && !mEntity.subtitle.equals("")) {
					subtitle.setText(Html.fromHtml(mEntity.subtitle));
					UI.setVisibility(subtitle, View.VISIBLE);
				}
			}
		}

	}

	protected void drawBody() {

		final TextView description = (TextView) findViewById(R.id.candi_form_description);
		final TextView address = (TextView) findViewById(R.id.candi_form_address);

		UI.setVisibility(findViewById(R.id.section_description), View.GONE);
		if (description != null) {
			description.setText(null);
			if (!TextUtils.isEmpty(mEntity.description)) {
				description.setText(Html.fromHtml(mEntity.description));
				UI.setVisibility(findViewById(R.id.section_description), View.VISIBLE);
			}
		}

		/* Place specific info */
		final Place place = (Place) mEntity;

		UI.setVisibility(address, View.GONE);
		if (address != null) {
			String addressBlock = place.getAddressBlock();

			if (place.phone != null) {
				addressBlock += "<br/>" + place.getFormattedPhone();
			}

			if (!"".equals(addressBlock)) {
				address.setText(Html.fromHtml(addressBlock));
				UI.setVisibility(address, View.VISIBLE);
			}
		}
	}

	protected void drawShortcuts() {

		/* Clear shortcut holder */
		ViewGroup shortcutHolder = (ViewGroup) findViewById(R.id.shortcut_holder);

		if (shortcutHolder != null) {
			shortcutHolder.removeAllViews();

			/* Synthetic applink shortcuts */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, true, true);
			settings.appClass = Applink.class;
			List<Shortcut> shortcuts = mEntity.getShortcuts(settings, null, null);
			if (shortcuts.size() > 0) {
				Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
				prepareShortcuts(shortcuts
						, settings
						, R.string.label_section_applinks
						, R.string.label_link_links_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.widget_shortcut);
			}

			/* service applink shortcuts */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, true);
			settings.appClass = Applink.class;
			shortcuts = mEntity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
			if (shortcuts.size() > 0) {
				for (Shortcut shortcut : shortcuts) {
					if (!shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
						shortcut.name = shortcut.app;
					}
					if (shortcut.app.equals(Constants.TYPE_APP_GOOGLEPLUS)) {
						shortcut.name = shortcut.name.replaceFirst("plus", "+");
					}
				}
				Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
				prepareShortcuts(shortcuts
						, settings
						, null
						, R.string.label_link_links_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.widget_shortcut);
			}
		}
	}

	protected void drawUsers() {

		final UserView user_one = (UserView) findViewById(R.id.user_one);
		final UserView user_two = (UserView) findViewById(R.id.user_two);

		/* Creator block */

		UI.setVisibility(user_one, View.GONE);
		UI.setVisibility(user_two, View.GONE);
		UserView userView = user_one;

		if (userView != null
				&& mEntity.creator != null
				&& !mEntity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (((Place) mEntity).getProvider().type.equals("aircandi")) {
					userView.setLabel(R.string.label_created_by);
					userView.databind(mEntity.creator, mEntity.createdDate.longValue());
					UI.setVisibility(userView, View.VISIBLE);
					userView = user_two;
				}
			}
			else {
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					userView.setLabel(R.string.label_added_by);
				}
				else {
					userView.setLabel(R.string.label_created_by);
				}
				userView.databind(mEntity.creator, mEntity.createdDate.longValue());
				UI.setVisibility(user_one, View.VISIBLE);
				userView = user_two;
			}

			if (userView != null && mEntity.modifier != null
					&& !mEntity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)
					&& !mEntity.modifier.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

				if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
					userView.setLabel(R.string.label_edited_by);
					userView.databind(mEntity.modifier, mEntity.modifiedDate.longValue());
					UI.setVisibility(userView, View.VISIBLE);
				}
			}
		}
	}

	@Override
	protected void drawStats() {

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		if (candiView != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, null, 0);
			}
			candiView.updateIndicator(R.id.holder_indicator_watching, String.valueOf(count.count.intValue()) + " "
					+ StringManager.getString(R.string.label_indicator_watching).toUpperCase(Locale.US));
		}
	}

	@Override
	public void drawButtons() {
		super.drawButtons();

		Place place = (Place) mEntity;

		/* TUNE */
		UI.setVisibility(findViewById(R.id.button_tune), View.GONE);

			/* Tuning buttons */
		final Boolean hasActiveProximityLink = place.hasActiveProximity();
		if (hasActiveProximityLink) {
			ComboButton button = (ComboButton) findViewById(R.id.button_tune);
			if (button != null) {
				button.setDrawableId(R.drawable.ic_action_signal_tuned);
			}
		}

		UI.setVisibility(findViewById(R.id.button_tune), View.VISIBLE);
	}

	protected void handlePackageInstalls() {

		for (String packageName : mPackageInstalls) {
			final String publicName = AndroidManager.getInstance().getPublicName(packageName);
			if (publicName != null) {
				UI.showToastNotification(publicName + " " + getText(R.string.dialog_install_toast_package_installed)
						, Toast.LENGTH_SHORT);
			}
		}

		if (mPackageInstalls.size() > 0) {
			mInvalidated = true;
			Aircandi.mainThreadHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					bind(BindingMode.AUTO);
				}
			}, 1500);
		}

		mPackageInstalls.clear();
	}

	protected void upsize() {
		/*
		 * Upsized places do not automatically link to nearby beacons because
		 * the browsing action isn't enough of an indicator of proximity.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_upsizing);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncUpsizeSynthetic");
				final ModelResult result = Aircandi.getInstance().getEntityManager().upsizeSynthetic((Place) mEntity, mWaitForContent);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusy.hideBusy(false);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final Entity upsizedEntity = (Entity) result.data;
					mEntityId = upsizedEntity.id;
					mEntity = null;
					mEntityMonitor = new EntityMonitor(mEntityId);
					mFirstDraw = true;
					bind(BindingMode.AUTO);
				}
				else {
					Errors.handleError(PlaceForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
		super.onResume();
		if (!this.isFinishing()) {
			handlePackageInstalls();
		}
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "Activity destroying: contextId: " + ((Object) this).hashCode());
		try {
			unregisterReceiver(mPackageReceiver);
		}
		catch (Exception ignore) {} // $codepro.audit.disable emptyCatchClause
		super.onDestroy();
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.place_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class PackageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {

				final String packageName = intent.getData().getEncodedSchemeSpecificPart();
				if (packageName != null) {

					String appName = AndroidManager.getAppNameByPackageName(packageName);
					if (appName != null && AndroidManager.hasIntentSupport(appName)) {
						/*
						 * It's an application we care about.
						 */
						mPackageInstalls.add(packageName);
						if (Aircandi.isActivityVisible()) {
							handlePackageInstalls();
						}
					}
				}
			}
		}
	}
}