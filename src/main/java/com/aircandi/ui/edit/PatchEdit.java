package com.aircandi.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityController;
import com.aircandi.components.ProximityController.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.LocationUpdatedEvent;
import com.aircandi.events.ProcessingCanceledEvent;
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
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.List;

@SuppressLint("Registered")
public class PatchEdit extends BaseEntityEdit {

	private   Button       mButtonTune;
	private   Button       mButtonUntune;
	private   TextView     mButtonPrivacy;
	private   TextView     mButtonPlace;
	private   TextView     mLocationLabel;
	protected AirImageView mPhotoViewPlace;
	protected MapView      mMapView;
	protected GoogleMap    mMap;
	protected Marker       mMarker;
	protected LatLng       mLocation;
	protected Entity       mPlaceToLinkTo;

	private List<Category> mCategories;
	private Spinner        mSpinnerCategory;
	private Integer        mSpinnerItemResId;
	private Category       mOriginalCategory;

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
				mPlaceToLinkTo = (Entity) Json.jsonToObject(json, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mButtonTune = (Button) findViewById(R.id.button_tune);
		mButtonUntune = (Button) findViewById(R.id.button_untune);
		mButtonPlace = (TextView) findViewById(R.id.button_place);
		mButtonPrivacy = (TextView) findViewById(R.id.button_privacy);
		mMapView = (MapView) findViewById(R.id.mapview);
		mPhotoViewPlace = (AirImageView) findViewById(R.id.place_photo);
		mSpinnerCategory = (Spinner) findViewById(R.id.spinner_category);
		mLocationLabel = (TextView) findViewById(R.id.location_label);

		if (mMapView != null) {

			mMapView.onCreate(null);
			mMapView.onResume();
			mMapView.getMapAsync(new OnMapReadyCallback() {
				@Override
				public void onMapReady(GoogleMap googleMap) {

					mMap = googleMap;
					mMap.getUiSettings().setMapToolbarEnabled(false);
					MapsInitializer.initialize(Patchr.applicationContext); // Initializes BitmapDescriptorFactory

					drawLocation();

					if (mMapView.getViewTreeObserver().isAlive()) {
						mMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
							@SuppressWarnings("deprecation") // We use the new method when supported
							@SuppressLint("NewApi") // We check which build version we are using.
							@Override
							public void onGlobalLayout() {

								if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
									mMapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
								else {
									mMapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
								}

								if (mLocation != null) {
									mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLocation, 17f));
									mMapView.invalidate();
								}
							}
						});
					}
				}
			});
		}

		mCategories = DataController.getInstance().getCategories();
	}

	@Override
	public void bind(BindingMode mode) {
		super.bind(mode);

		mOriginalCategory = ((Patch) mEntity).category;
		initCategorySpinner();

		Patch patch = (Patch) mEntity;

		if (!mEditing) {
			if (mPlaceToLinkTo != null) {
				/*
				 * Place was passed in for default linking.
				 */
				((Patch) mEntity).location = mPlaceToLinkTo.location;
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
					patch.location.provider = Constants.LOCATION_PROVIDER_GOOGLE;
				}
			}
		}
		else {
			Link linkPlace = patch.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);
			if (linkPlace != null) {
				mPlaceToLinkTo = linkPlace.shortcut.getAsEntity();
			}
		}

		draw(null);
	}

	@Override
	public void draw(View view) {
		/*
		 * Only called when the activity is first created.
		 */
		Patch patch = (Patch) mEntity;

		drawLocation();

		if (mButtonPrivacy != null) {
			mButtonPrivacy.setTag(patch.privacy);
			String value = (patch.privacy.equals(Constants.PRIVACY_PUBLIC))
			               ? StringManager.getString(R.string.label_patch_privacy_public)
			               : StringManager.getString(R.string.label_patch_privacy_private);
			mButtonPrivacy.setText(StringManager.getString(R.string.label_patch_edit_privacy) + ": " + value);
		}

		if (mButtonPlace != null) {
			UI.setVisibility(mPhotoViewPlace, View.GONE);

			mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": None");
			if (mPlaceToLinkTo != null) {
				mButtonPlace.setTag(mPlaceToLinkTo);
				mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": " + mPlaceToLinkTo.name);
				UI.drawPhoto(mPhotoViewPlace, mPlaceToLinkTo.getPhoto());
				UI.setVisibility(mPhotoViewPlace, View.VISIBLE);
			}
		}

		/* Tuning buttons */
		UI.setVisibility(findViewById(R.id.group_tune), View.GONE);
		if (mEditing) {
			UI.setVisibility(findViewById(R.id.group_tune), View.VISIBLE);
			final Boolean hasActiveProximityLink = patch.hasActiveProximity();
			if (hasActiveProximityLink) {
				mFirstTune = false;
				UI.setVisibility(mButtonUntune, View.VISIBLE);
			}
		}

		super.draw(view);
	}

	public void drawLocation() {
		/*
		 * When creating a new patch we use the devices current location by default if
		 * available. If a location isn't available for whatever reason we show the
		 * map without a marker and say so in a label. From that point on, the only way
		 * a patch gets a location is if the user sets one or it is linked to a place.
		 */
		Patch patch = (Patch) mEntity;
		mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_none));

		if (mMap != null) {
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		}

		if (mMap != null && mMapView != null && patch.location != null) {

			mLocation = new LatLng(patch.location.lat.doubleValue(), patch.location.lng.doubleValue());
			mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_google));

			if (patch.location.provider != null) {
				if (patch.location.provider.equals(Constants.LOCATION_PROVIDER_USER)) {
					mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_user));
				}
				else if (patch.location.provider.equals(Constants.LOCATION_PROVIDER_PLACE)) {
					if (mPlaceToLinkTo != null) {
						mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_place));
					}
					else {
						mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_unknown));
					}
				}
				else if (patch.location.provider.equals(Constants.LOCATION_PROVIDER_GOOGLE)) {
					if (mEditing) {
						mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_google));
					}
					else {
						mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_google_new));
					}
				}
			}

			mMap.clear();
			mMap.addMarker(new MarkerOptions()
					.position(mLocation)
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
					.anchor(0.5f, 0.5f));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onLocationChanged(final LocationUpdatedEvent event) {
		/*
		 * Getting location updates because we are inserting a patch and
		 * location services are enabled.
		 */
		if (event.location != null) {

			LocationManager.getInstance().setLocationLocked(event.location);
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();

			if (location != null) {

				Patch patch = (Patch) mEntity;
				if (patch.location == null) {
					patch.location = new AirLocation();
					patch.location.lat = location.lat;
					patch.location.lng = location.lng;
					patch.location.accuracy = location.accuracy;
					patch.location.provider = Constants.LOCATION_PROVIDER_GOOGLE;
				}
				else if (patch.location.provider.equals(Constants.LOCATION_PROVIDER_GOOGLE)) {
					patch.location.lat = location.lat;
					patch.location.lng = location.lng;
					patch.location.accuracy = location.accuracy;
				}
				drawLocation();
			}
		}
	}

	@Subscribe
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(PatchEdit.this, "Query wifi scan received event: locking beacons");
					if (event.wifiList != null) {
						ProximityController.getInstance().lockBeacons();
					}
					else {
					    /*
					     * We fake that the tuning happened because it is simpler than enabling/disabling ui
						 */
						mUiController.getBusyController().hide(false);
						if (mUntuning) {
							mButtonUntune.setText(R.string.button_tuning_tuned);
							mUntuned = true;
						}
						else {
							mButtonTune.setText(R.string.button_tuning_tuned);
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
	public void onCancelEvent(ProcessingCanceledEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	public void onTuneButtonClick(View view) {
		if (!mTuned) {
			mUntuning = false;
			mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_tuning, PatchEdit.this);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	public void onUntuneButtonClick(View view) {
		if (!mUntuned) {
			mUntuning = true;
			mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_tuning, PatchEdit.this);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	public void onPlacePickerClick(View view) {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_SEARCH_SCOPE, DataController.SuggestScope.PLACES.ordinal());
		extras.putBoolean(Constants.EXTRA_SEARCH_RETURN_ENTITY, true);
		extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.VIEW_TO);

		if (mButtonPlace.getTag() != null) {
			Entity entity = (Entity) mButtonPlace.getTag();
			extras.putBoolean(Constants.EXTRA_SEARCH_CLEAR_BUTTON, true);
			extras.putString(Constants.EXTRA_SEARCH_CLEAR_BUTTON_MESSAGE, StringManager.getString(R.string.button_clear_place));
			extras.putString(Constants.EXTRA_SEARCH_PHRASE, entity.name);
		}
		Patchr.router.route(this, Route.SEARCH, mEntity, extras);
	}

	public void onPrivacyBuilderClick(View view) {
		Patchr.router.route(this, Route.PRIVACY_EDIT, mEntity, null);
	}

	public void onLocationBuilderClick(View view) {
		Patchr.router.route(this, Route.LOCATION_EDIT, mEntity, null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_SEARCH) {
				if (intent == null || intent.getExtras() == null) {
					mDirty = true;
					mPlaceToLinkTo = null;
					mButtonPlace.setTag(null);
					mButtonPlace.setText(StringManager.getString(R.string.label_patch_edit_place) + ": None");
					UI.setVisibility(mPhotoViewPlace, View.GONE);
				}
				else {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_ENTITY);
					if (json != null) {
						final Place place = (Place) Json.jsonToObject(json, Json.ObjectType.ENTITY);
						mDirty = true;
						((Patch) mEntity).location = place.location;
						((Patch) mEntity).location.provider = Constants.LOCATION_PROVIDER_PLACE;
						mProximityDisabled = true;
						mPlaceToLinkTo = place;
					}
				}
				draw(null);
			}
			else if (requestCode == Constants.ACTIVITY_PRIVACY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String privacy = extras.getString(Constants.EXTRA_PRIVACY);
					if (privacy != null) {
						mDirty = true;
						((Patch) mEntity).privacy = privacy;
						draw(null);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_LOCATION_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_LOCATION);
					if (json != null) {
						final AirLocation location = (AirLocation) Json.jsonToObject(json, Json.ObjectType.AIR_LOCATION);
						mDirty = true;
						((Patch) mEntity).location = location;
						((Patch) mEntity).location.provider = Constants.LOCATION_PROVIDER_USER;
						draw(null);
						mProximityDisabled = true;
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

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(mEditing ? R.string.form_title_patch_edit : R.string.form_title_patch_new);
		}
	}

	public void accept() {

		if (placeDirty()) {

			mTaskService = new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mUiController.getBusyController().show(BusyAction.Update);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("AsyncInsertPlace");

					ModelResult result = new ModelResult();
					Link placeLink = mEntity.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);
					Place place = (Place) mButtonPlace.getTag();

					/* Adding a new place. */
					if (place != null && Type.isTrue(place.synthetic)) {
						result = DataController.getInstance().insertEntity(place, null, null, null, null, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
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
							result = DataController.getInstance().deleteLink(mEntity.id
									, placeLink.shortcut.id
									, Constants.TYPE_LINK_PROXIMITY
									, true
									, Constants.SCHEMA_ENTITY_PLACE
									, "delete_link_proximity", NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
						}
						/*
						 * If editing then add link here otherwise it will be added when
						 * the entity is inserted
						 */
						if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS
								&& mEditing && place != null) {
							result = DataController.getInstance().insertLink(null
									, mEntity.id
									, place.id
									, Constants.TYPE_LINK_PROXIMITY
									, true
									, null
									, null
									, "insert_link_proximity"
									, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
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
			}.executeOnExecutor(Constants.EXECUTOR);
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
		Patchr.router.route(this, Route.BROWSE, mEntity, null);
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

	private void initCategorySpinner() {

		final Patch entity = (Patch) mEntity;
		final List<String> categoryStrings = DataController.getInstance().getCategoriesAsStringArray();
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(PatchEdit.this
				, R.layout.spinner_item
				, categoryStrings);

		adapter.setDropDownViewResource(R.layout.temp_listitem_dropdown_spinner);

		mSpinnerCategory.setVisibility(View.VISIBLE);
		mSpinnerCategory.setClickable(true);
		mSpinnerCategory.setAdapter(adapter);

		if (entity.category != null) {
			int index = 0;
			for (Category category : mCategories) {
				if (category.id.equals(entity.category.id)) {
					mSpinnerCategory.setSelection(index);
					break;
				}
				index++;
			}
		}

		mSpinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position < mCategories.size()) {
					if (entity.category == null || !entity.category.id.equals(mCategories.get(position).id)) {
						mDirty = true;
						entity.category = mCategories.get(position);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		/* Encourages the user to select a patch type when they are creating a new patch. */
		if (!mEditing && mEntity != null) {
			mSpinnerCategory.performClick();
		}
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
		Integer beaconMax = !mUntuning ? Constants.PROXIMITY_BEACON_COVERAGE : Constants.PROXIMITY_BEACON_UNCOVERAGE;
		final List<Beacon> beacons = ProximityController.getInstance().getStrongestBeacons(beaconMax);
		final Beacon primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(mNotEmpty ? BusyAction.Refreshing : BusyAction.Refreshing_Empty);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncTrackEntityProximity");

				final ModelResult result = DataController.getInstance().trackEntity(mEntity
						, beacons
						, primaryBeacon
						, mUntuning, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				mUiController.getBusyController().hide(false);

				if (mTuned || mUntuned) {
				    /* Undoing a tuning */
					mButtonTune.setText(R.string.button_tuning_tune);
					mButtonUntune.setText(R.string.button_tuning_untune);
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
						mButtonUntune.setText(R.string.button_tuning_tuned);
						mButtonTune.setText(R.string.button_tuning_undo);
						mUntuned = true;
					}
					else {
						mButtonTune.setText(R.string.button_tuning_tuned);
						mButtonUntune.setText(R.string.button_tuning_undo);
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
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void clearProximity() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(mNotEmpty ? BusyAction.Refreshing : BusyAction.Refreshing_Empty);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncClearEntityProximity");
				final ModelResult result = DataController.getInstance().trackEntity(mEntity, null, null, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				mUiController.getBusyController().hide(false);
				UI.showToastNotification(StringManager.getString(mUpdatedResId), Toast.LENGTH_SHORT);
				setResultCode(Activity.RESULT_OK);
				finish();
				AnimationManager.doOverridePendingTransition(PatchEdit.this, TransitionType.FORM_BACK);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
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

	@Override
	protected int getLayoutId() {
		return (mLayoutResId != null && mLayoutResId != 0) ? mLayoutResId : R.layout.patch_edit;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onResume() {
		if (!mEditing && LocationManager.getInstance().isLocationAccessEnabled()) {
			LocationManager.getInstance().requestLocationUpdates(this);  // Location triggers sequence
		}
		super.onResume();
	}

	@Override
	public void onPause() {
		if (mMapView != null) mMapView.onPause();
		if (!mEditing) {
			LocationManager.getInstance().stop();
		}
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

	private class CategoryAdapter extends ArrayAdapter {

		private final List<String> mCategories;

		private CategoryAdapter(Context context, int textViewResourceId, List categories) {
			super(context, textViewResourceId, categories);
			mCategories = categories;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final View view = super.getView(position, convertView, parent);
			final TextView text = (TextView) view.findViewById(R.id.text1);
			FontManager.getInstance().setTypefaceLight(text);
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

			if (position == getCount()) {
				text.setText("");
				text.setHint(mCategories.get(getCount())); //"Hint to be displayed"
			}

			return view;
		}

		@Override
		public int getCount() {
			return super.getCount() - 1; // you dont display last item. It is used as hint.
		}
	}
}