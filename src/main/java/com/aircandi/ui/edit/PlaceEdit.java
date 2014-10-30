package com.aircandi.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.components.TabManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.CancelEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Category;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.ToolTipView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.ViewId;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.List;

@SuppressLint("Registered")
public class PlaceEdit extends BaseEntityEdit {

	protected ToolTipRelativeLayout mTooltips;

	private   TabManager  mTabManager;
	private   ComboButton mButtonTune;
	private   ComboButton mButtonUntune;
	private   TextView    mButtonCategory;
	private   TextView    mButtonPrivacy;
	protected MapView     mMapView;
	protected GoogleMap   mMap;
	protected Marker      mMarker;

	private Boolean mTuned           = false;
	private Boolean mUntuned         = false;
	private Boolean mTuningInProcess = false;
	private Boolean mUntuning        = false;
	private Boolean mFirstTune       = true;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mButtonTune = (ComboButton) findViewById(R.id.button_tune);
		mButtonUntune = (ComboButton) findViewById(R.id.button_untune);
		mButtonCategory = (TextView) findViewById(R.id.button_category);
		mButtonPrivacy = (TextView) findViewById(R.id.button_privacy);
		mMapView = (MapView) findViewById(R.id.mapview);

		if (mMapView != null) {
			mMapView.onCreate(savedInstanceState);
			mMap = mMapView.getMap();
			if (checkReady()) {
				setUpMap();
			}
		}

		if (mEntity == null || (mEntity.ownerId != null && (mEntity.ownerId.equals(Patchr.getInstance().getCurrentUser().id)))) {
			ViewFlipper flipper = (ViewFlipper) findViewById(R.id.flipper_form);
			if (flipper != null) {
				mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
				mTabManager.initialize();
				mTabManager.doRestoreInstanceState(savedInstanceState);
			}
		}

		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		if (mTooltips != null) {
			mTooltips.setSingleShot(Constants.TOOLTIPS_PLACE_EDIT_ID);
		}
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public void draw(View view) {
		/*
		 * Only called when the activity is being created.
		 */
		Place place = (Place) mEntity;

		if (!mEditing) {
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
			if (location != null) {
				if (place.location == null) {
					place.location = new AirLocation();
				}
				place.location.lat = location.lat;
				place.location.lng = location.lng;
				place.location.accuracy = location.accuracy;
				place.provider.aircandi = Patchr.getInstance().getCurrentUser().id;
			}
		}

		if (mMapView != null && place.location != null) {
			mMap.clear();
			mMarker = mMap.addMarker(new MarkerOptions()
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
					.position(new LatLng(mEntity.location.lat.doubleValue(), mEntity.location.lng.doubleValue()))
					.draggable(true));
			mMarker.setAnchor(0.5f, 0.5f);
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(place.location.lat.doubleValue(), place.location.lng.doubleValue()), 17));
		}

		if (mButtonPrivacy != null) {
			mButtonPrivacy.setTag(place.privacy);
			String value = (place.privacy.equals(Constants.PRIVACY_PUBLIC))
			               ? StringManager.getString(R.string.label_place_privacy_public)
			               : StringManager.getString(R.string.label_place_privacy_private);
			mButtonPrivacy.setText(StringManager.getString(R.string.label_place_edit_privacy) + ": " + value);
		}

		if (mButtonCategory != null) {
			mButtonCategory.setTag(place.category);
			mButtonCategory.setText(place.category != null
			                        ? StringManager.getString(R.string.label_place_edit_category) + ": " + place.category.name
			                        : StringManager.getString(R.string.label_place_edit_category) + ": None");
		}

		/* Tuning buttons */
		UI.setVisibility(findViewById(R.id.group_tune), View.GONE);
		if (mEditing) {
			UI.setVisibility(findViewById(R.id.group_tune), View.VISIBLE);
			final Boolean hasActiveProximityLink = place.hasActiveProximity();
			if (hasActiveProximityLink) {
				mFirstTune = false;
				mButtonUntune.setVisibility(View.VISIBLE);
			}
		}

		/* Help message */
		UI.setVisibility(findViewById(R.id.label_message), mEditing ? View.GONE : View.VISIBLE);
		UI.setVisibility(findViewById(R.id.label_to), mEditing ? View.GONE : View.VISIBLE);
		UI.setVisibility(findViewById(R.id.to), mEditing ? View.GONE : View.VISIBLE);

		super.draw(view);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		if (!mTuned) {
			mUntuning = false;
			mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_tuning);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityManager.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onUntuneButtonClick(View view) {
		if (!mUntuned) {
			mUntuning = true;
			mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_tuning);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityManager.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(PlaceEdit.this, "Query wifi scan received event: locking beacons");
					if (event.wifiList != null) {
						ProximityManager.getInstance().lockBeacons();
					}
					else {
					    /*
					     * We fake that the tuning happened because it is simpler than enabling/disabling ui
						 */
						mBusy.hideBusy(false);
						if (mUntuning) {
							mButtonUntune.setLabel(R.string.button_tuning_tuned);
							mUntuned = true;
						}
						else {
							mButtonTune.setLabel(R.string.button_tuning_tuned);
							mTuned = true;
						}
						mButtonTune.forceLayout();
						mButtonUntune.forceLayout();
						mTuningInProcess = false;
					}
				}
			});
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(PlaceEdit.this, "Beacons locked event: tune entity");
					tuneProximity();
				}
			});
		}
	}

	@Subscribe
	public void onCancelEvent(CancelEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	@SuppressWarnings("ucd")
	public void onAddressBuilderClick(View view) {
		Patchr.dispatch.route(this, Route.ADDRESS_EDIT, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		Patchr.dispatch.route(this, Route.CATEGORY_EDIT, mEntity, null, null);
	}

	public void onPrivacyBuilderClick(View view) {
		Patchr.dispatch.route(this, Route.PRIVACY_EDIT, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onLocationBuilderClick(View view) {
		Patchr.dispatch.route(this, Route.LOCATION_EDIT, mEntity, null, null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mTooltips != null) {
			View view = findViewById(R.id.tooltips);
			view.post(new Runnable() {
				@Override
				public void run() {
					showTooltips();
				}
			});
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ADDRESS_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					mDirty = true;
					final Bundle extras = intent.getExtras();

					final String jsonPlace = extras.getString(Constants.EXTRA_PLACE);
					if (jsonPlace != null) {
						final Place placeUpdated = (Place) Json.jsonToObject(jsonPlace, Json.ObjectType.ENTITY);
						if (placeUpdated.phone != null) {
							placeUpdated.phone = placeUpdated.phone.replaceAll("[^\\d.]", "");
						}
						mEntity = placeUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(((Place) mEntity).address);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonCategory = extras.getString(Constants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						final Category categoryUpdated = (Category) Json.jsonToObject(jsonCategory, Json.ObjectType.CATEGORY);
						if (categoryUpdated != null) {
							mDirty = true;
							((Place) mEntity).category = categoryUpdated;
							mButtonCategory.setTag(categoryUpdated);
							mButtonCategory.setText(categoryUpdated != null ? "Category: " + categoryUpdated.name : "Category: None");
							drawPhoto();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PRIVACY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String privacy = extras.getString(Constants.EXTRA_PRIVACY);
					if (privacy != null) {
						mDirty = true;
						((Place) mEntity).privacy = privacy;
						mButtonPrivacy.setTag(privacy);
						String value = (privacy.equals(Constants.PRIVACY_PUBLIC)) ? "Public" : "Closed";
						mButtonPrivacy.setText("Privacy: " + value);
						drawPhoto();
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_LOCATION_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_LOCATION);
					if (json != null) {
						final AirLocation locationUpdated = (AirLocation) Json.jsonToObject(json, Json.ObjectType.AIR_LOCATION);
						if (locationUpdated != null) {
							mDirty = true;
							((Place) mEntity).location = locationUpdated;
							mMarker.setPosition(new LatLng(locationUpdated.lat.doubleValue(), locationUpdated.lng.doubleValue()));
							mProximityInvalid = true;
							mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(((Place) mEntity).location.lat.doubleValue(), ((Place) mEntity).location.lng.doubleValue()), 16));
						}
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected boolean afterInsert() {
	    /*
	     * Only called if the insert was successful. Called on main ui thread.
		 */
		if (mInsertedResId != null && mInsertedResId != 0) {
			UI.showToastNotification(StringManager.getString(mInsertedResId), Toast.LENGTH_SHORT);
		}
		Patchr.dispatch.route(this, Route.BROWSE, mEntity, null, null);
		return true;
	}

	@Override
	protected boolean afterUpdate() {
	    /*
	     * Only called if the update was successful. Called on main ui thread.
	     * If proximity links are now invalid because the place was manually moved
	     * to a new location then clear all the links.
		 */
		if (mProximityInvalid) {
			clearProximity();
			return false;
		}
		return true;
	}

	private void tuneProximity() {
	    /*
	     * If there are beacons:
		 * 
		 * - links to beacons created.
		 * - link_proximity action logged.
		 * 
		 * If no beacons:
		 * 
		 * - no links are created.
		 * - entity_proximity action logged.
		 */
		Integer beaconMax = !mUntuning ? ServiceConstants.PROXIMITY_BEACON_COVERAGE : ServiceConstants.PROXIMITY_BEACON_UNCOVERAGE;
		final List<Beacon> beacons = ProximityManager.getInstance().getStrongestBeacons(beaconMax);
		final Beacon primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncTrackEntityProximity");

				final ModelResult result = Patchr.getInstance().getEntityManager().trackEntity(mEntity
						, beacons
						, primaryBeacon
						, mUntuning);

				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				setProgressBarIndeterminateVisibility(false);
				mBusy.hideBusy(false);

				if (mTuned || mUntuned) {
				    /* Undoing a tuning */
					mButtonTune.setLabel(R.string.button_tuning_tune);
					mButtonUntune.setLabel(R.string.button_tuning_untune);
					mUntuned = false;
					mTuned = false;
					if (!mFirstTune) {
						mButtonUntune.setVisibility(View.VISIBLE);
					}
					else {
						mButtonUntune.setVisibility(View.GONE);
					}
				}
				else {
					/* Tuning or untuning */
					if (mUntuning) {
						mButtonUntune.setLabel(R.string.button_tuning_tuned);
						mButtonTune.setLabel(R.string.button_tuning_undo);
						mUntuned = true;
					}
					else {
						mButtonTune.setLabel(R.string.button_tuning_tuned);
						mButtonUntune.setLabel(R.string.button_tuning_undo);
						mTuned = true;
						if (mButtonUntune.getVisibility() != View.VISIBLE) {
							mButtonUntune.setVisibility(View.VISIBLE);
						}
					}
				}
				mButtonTune.forceLayout();
				mButtonUntune.forceLayout();
				mTuningInProcess = false;
			}
		}.execute();
	}

	private void clearProximity() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncClearEntityProximity");
				final ModelResult result = Patchr.getInstance().getEntityManager().trackEntity(mEntity, null, null, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				mBusy.hideBusy(false);
				UI.showToastNotification(StringManager.getString(mUpdatedResId), Toast.LENGTH_SHORT);
				setResultCode(Activity.RESULT_OK);
				finish();
				Patchr.getInstance().getAnimationManager().doOverridePendingTransition(PlaceEdit.this, TransitionType.FORM_TO_PAGE);
			}
		}.execute();
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;

		gather();
		Place place = (Place) mEntity;
		if (place.name == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_place_name)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override
	protected void gather() {
		super.gather();
		if (!mEditing) {
			((Place) mEntity).provider.aircandi = Patchr.getInstance().getCurrentUser().id;
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_PROXIMITY;
	}

	public void showTooltips() {

		if (mTooltips != null && !mTooltips.hasShot()) {
			mTooltips.setClickable(true);
			mTooltips.setVisibility(View.VISIBLE);
			mTooltips.clear();
			mTooltips.requestLayout();

			ToolTipView part1 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part1))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part1.setId(ViewId.getInstance().getUniqueId());
			part1.addRule(RelativeLayout.CENTER_HORIZONTAL);

			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) part1.getLayoutParams();
			params.setMargins(0, UI.getRawPixelsForDisplayPixels(40f), 0, 0);
			part1.setLayoutParams(params);

			ToolTipView part2 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part2))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part2.setId(ViewId.getInstance().getUniqueId());
			part2.setMinimumWidth(UI.getRawPixelsForDisplayPixels(250f));
			part2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part2.addRule(RelativeLayout.BELOW, part1.getId());

			ToolTipView part3 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part3))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part3.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part3.addRule(RelativeLayout.BELOW, part2.getId());
		}
	}

	private boolean checkReady() {
		/*
		 * Parent activity performs play services check. We can't get to
		 * here if they are not available.
		 */
		if (mMap == null) {
			UI.showToastNotification("Map not ready", Toast.LENGTH_SHORT);
			return false;
		}
		return true;
	}

	private void setUpMap() {

		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.setLocationSource(null);
		mMap.setMyLocationEnabled(false);

		UiSettings uiSettings = mMap.getUiSettings();

		uiSettings.setZoomControlsEnabled(false);
		uiSettings.setMyLocationButtonEnabled(false);
		uiSettings.setAllGesturesEnabled(false);
		uiSettings.setCompassEnabled(false);

		MapsInitializer.initialize(this);
	}

	public void lookupAddressForLocation(final AirLocation location) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncAddressForLocation");
				ModelResult result = LocationManager.getInstance().getAddressForLocation(location);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				if (isFinishing()) return;

				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {

					List<Address> addresses = (List<Address>) result.data;
					Address geolookup = addresses.get(0);

					Place place = (Place) mEntity;

					place.address = geolookup.getAddressLine(0);
					place.city = geolookup.getLocality();
					place.region = geolookup.getAdminArea();
					place.postalCode = geolookup.getPostalCode();
					place.country = geolookup.getCountryName();
				}
				else {
					Errors.handleError(PlaceEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

 	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		if (mTooltips != null) {
			View view = findViewById(R.id.tooltips);
			view.post(new Runnable() {
				@Override
				public void run() {
					showTooltips();
				}
			});
		}
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mTooltips != null) {
			mTooltips.hide(false);
		}
		return super.onOptionsItemSelected(item);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onResume() {
		if (mMapView != null) mMapView.onResume();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mMapView != null) mMapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if (mMapView != null) mMapView.onLowMemory();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return (mLayoutResId != null && mLayoutResId != 0) ? mLayoutResId : R.layout.place_edit;
	}
}