package com.aircandi.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.ToolTipView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
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
public class PatchEdit extends BaseEntityEdit {

	protected ToolTipRelativeLayout mTooltips;

	private   TabManager  mTabManager;
	private   ComboButton mButtonTune;
	private   ComboButton mButtonUntune;
	private   TextView    mButtonCategory;
	private   TextView    mButtonPrivacy;
	private   TextView    mButtonPlace;
	protected MapView     mMapView;
	protected GoogleMap   mMap;
	protected Marker      mMarker;
	protected Entity      mPlace;

	private Boolean mTuned           = false;
	private Boolean mUntuned         = false;
	private Boolean mTuningInProcess = false;
	private Boolean mUntuning        = false;
	private Boolean mFirstTune       = true;
	private Boolean mPlaceDirty      = false;

	public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String json = extras.getString(Constants.EXTRA_ENTITY_PARENT);
			if (json != null) {
				mPlace = (Entity) Json.jsonToObject(json, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mButtonTune = (ComboButton) findViewById(R.id.button_tune);
		mButtonUntune = (ComboButton) findViewById(R.id.button_untune);
		mButtonCategory = (TextView) findViewById(R.id.button_category);
		mButtonPlace = (TextView) findViewById(R.id.button_place);
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
		Patch patch = (Patch) mEntity;

		if (!mEditing) {
			if (mPlace != null) {
				/*
				 * Place was passed in for default linking.
				 */
				((Patch) mEntity).location = mPlace.location;
				mProximityDisabled = true;
			}
			else {
				final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
				if (location != null) {
					if (patch.location == null) {
						patch.location = new AirLocation();
					}
					patch.location.lat = location.lat;
					patch.location.lng = location.lng;
					patch.location.accuracy = location.accuracy;
				}
			}
		}

		if (mMapView != null && patch.location != null) {
			mMap.clear();
			mMarker = mMap.addMarker(new MarkerOptions()
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
					.position(new LatLng(mEntity.location.lat.doubleValue(), mEntity.location.lng.doubleValue()))
					.draggable(true));
			mMarker.setAnchor(0.5f, 0.5f);
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(patch.location.lat.doubleValue(), patch.location.lng.doubleValue()), 17));
		}

		if (mButtonPrivacy != null) {
			mButtonPrivacy.setTag(patch.privacy);
			String value = (patch.privacy.equals(Constants.PRIVACY_PUBLIC))
			               ? StringManager.getString(R.string.label_patch_privacy_public)
			               : StringManager.getString(R.string.label_patch_privacy_private);
			mButtonPrivacy.setText(StringManager.getString(R.string.label_patch_edit_privacy) + ": " + value);
		}

		if (mButtonCategory != null) {
			mButtonCategory.setTag(patch.category);
			mButtonCategory.setText(patch.category != null
			                        ? StringManager.getString(R.string.label_patch_edit_category) + ": " + patch.category.name
			                        : StringManager.getString(R.string.label_patch_edit_category) + ": None");
		}

		if (mButtonPlace != null) {
			mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": None");
			if (!mEditing) {
				if (mPlace != null) {
					mButtonPlace.setTag(mPlace);
					mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": " + mPlace.name);
				}
			}
			else {
				Link linkPlace = patch.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);
				if (linkPlace != null) {
					Entity place = linkPlace.shortcut.getAsEntity();
					mButtonPlace.setTag(place);
					mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": " + place.name);
				}
			}
		}

		/* Tuning buttons */
		UI.setVisibility(findViewById(R.id.group_tune), View.GONE);
		if (mEditing) {
			UI.setVisibility(findViewById(R.id.group_tune), View.VISIBLE);
			final Boolean hasActiveProximityLink = patch.hasActiveProximity();
			if (hasActiveProximityLink) {
				mFirstTune = false;
				mButtonUntune.setVisibility(View.VISIBLE);
			}
		}

		/* Help message */
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
					Logger.d(PatchEdit.this, "Query wifi scan received event: locking beacons");
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
					Logger.d(PatchEdit.this, "Beacons locked event: tune entity");
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
	public void onCategoryBuilderClick(View view) {
		Patchr.dispatch.route(this, Route.CATEGORY_EDIT, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onPlacePickerClick(View view) {
		Patchr.dispatch.route(this, Route.PLACE_SEARCH, mEntity, null, null);
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
			if (requestCode == Constants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonCategory = extras.getString(Constants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						final Category categoryUpdated = (Category) Json.jsonToObject(jsonCategory, Json.ObjectType.CATEGORY);
						if (categoryUpdated != null) {
							mDirty = true;
							((Patch) mEntity).category = categoryUpdated;
							mButtonCategory.setTag(categoryUpdated);
							mButtonCategory.setText("Category: " + categoryUpdated.name);
							drawPhoto();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PLACE_SEARCH) {
				if (intent == null || intent.getExtras() == null) {
					mDirty = true;
					mButtonPlace.setTag(null);
					mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": None");
				}
				else {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_ENTITY);
					if (json != null) {
						final Place place = (Place) Json.jsonToObject(json, Json.ObjectType.ENTITY);
						mDirty = true;
						mButtonPlace.setTag(place);
						mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": " + place.name);
						((Patch) mEntity).location = place.location;
						mMarker.setPosition(new LatLng(place.location.lat.doubleValue(), place.location.lng.doubleValue()));
						mProximityDisabled = true;
						mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(((Patch) mEntity).location.lat.doubleValue(), ((Patch) mEntity).location.lng.doubleValue()), 16));
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PRIVACY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String privacy = extras.getString(Constants.EXTRA_PRIVACY);
					if (privacy != null) {
						mDirty = true;
						((Patch) mEntity).privacy = privacy;
						mButtonPrivacy.setTag(privacy);
						String value = (privacy.equals(Constants.PRIVACY_PUBLIC)) ? "Public" : "Private";
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
							((Patch) mEntity).location = locationUpdated;
							mMarker.setPosition(new LatLng(locationUpdated.lat.doubleValue(), locationUpdated.lng.doubleValue()));
							mProximityDisabled = true;
							mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(((Patch) mEntity).location.lat.doubleValue(), ((Patch) mEntity).location.lng.doubleValue()), 16));
						}
					}
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (mMapView != null) mMapView.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void accept() {

		if (placeDirty()) {

			mTaskService = new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mBusy.showBusy(BusyAction.Update);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("AsyncInsertPlace");

					ModelResult result = new ModelResult();
					Link placeLink = mEntity.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);
					Place place = (Place) mButtonPlace.getTag();

					/* Adding a new place. */
					if (place != null && Type.isTrue(place.synthetic)) {
						result = Patchr.getInstance().getEntityManager().insertEntity(place, null, null, null, null, true);
						if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
							Entity insertedPlace = (Entity) result.data;
							place.id = insertedPlace.id;
							place.synthetic = false;
						}
					}

					/* Link management */
					if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {

						/* Existing link so delete it */
						if (placeLink != null) {
							result = Patchr.getInstance().getEntityManager().deleteLink(mEntity.id
									, placeLink.shortcut.id
									, Constants.TYPE_LINK_PROXIMITY
									, true
									, Constants.SCHEMA_ENTITY_PLACE
									, "delete_link_proximity");
						}

						/* Add link */
						if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS
								&& place != null) {
							result = Patchr.getInstance().getEntityManager().insertLink(null
									, mEntity.id
									, place.id
									, Constants.TYPE_LINK_PROXIMITY
									, true
									, null
									, null
									, "insert_link_proximity"
									, true);
						}
					}
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;
					if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
						if (mEditing) {
							update();
						}
						else {
							insert();
						}
					}
					else {
						Errors.handleError(PatchEdit.this, result.serviceResponse);
					}
				}
			}.execute();
			return;
		}

		if (mEditing) {
			update();
		}
		else {
			insert();
		}
	}

	@Override
	protected void beforeInsert(Entity entity, List<Link> links) {
	    /*
	     * We link patches to the places if needed. Called on background thread.
		 */
		if (mButtonPlace.getTag() != null) {
			final Place place = (Place) mButtonPlace.getTag();
			links.add(new Link(place.id, Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE));
		}
	}

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
	     * If proximity links are now invalid because the patch was manually moved
	     * to a new location then clear all the links.
		 */
		if (mProximityDisabled) {
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
				Patchr.getInstance().getAnimationManager().doOverridePendingTransition(PatchEdit.this, TransitionType.FORM_TO_PAGE);
			}
		}.execute();
	}

	private boolean placeDirty() {

		final Link placeLink = mEntity.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);

		final Boolean originalLink = (placeLink != null);
		final Boolean newLink = (mButtonPlace.getTag() != null);

		/* Staying unlinked */
		if (!originalLink && !newLink)
			return false;

		/* Staying linked so compare targets */
		if (originalLink && newLink) {
			String oldId = placeLink.shortcut.id;
			String newId = ((Entity) mButtonPlace.getTag()).id;
			if (oldId.equals(newId))
				return false;
		}

		/* Making a change */
		return true;
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;

		gather();
		Patch patch = (Patch) mEntity;
		if (patch.name == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_patch_name)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
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
					.withText(StringManager.getString(R.string.tooltip_patch_new_part1))
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
					.withText(StringManager.getString(R.string.tooltip_patch_new_part2))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part2.setId(ViewId.getInstance().getUniqueId());
			part2.setMinimumWidth(UI.getRawPixelsForDisplayPixels(250f));
			part2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part2.addRule(RelativeLayout.BELOW, part1.getId());

			ToolTipView part3 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_patches_new_part3))
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
	public void onPause() {
		if (mMapView != null) mMapView.onPause();
		super.onPause();
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
		return (mLayoutResId != null && mLayoutResId != 0) ? mLayoutResId : R.layout.patch_edit;
	}
}