package com.aircandi.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.LocationManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.LocationUpdatedEvent;
import com.aircandi.events.ProcessingCanceledEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirProgressBar;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.List;

@SuppressLint("Registered")
public class PatchEdit extends BaseEntityEdit {

	private   TextView     mButtonPrivacy;
	private   TextView     mButtonPlace;
	private   TextView     mLocationLabel;
	protected AirImageView mPhotoViewPlace;

	private RadioGroup  mButtonGroupType;
	private RadioButton mButtonTypeEvent;
	private RadioButton mButtonTypeGroup;
	private RadioButton mButtonTypePlace;
	private RadioButton mButtonTypeProject;

	protected MapView        mMapView;
	protected AirProgressBar mMapProgressBar;
	protected GoogleMap      mMap;
	protected Marker         mMarker;

	protected Entity mPlaceToLinkTo;

	private Boolean mPlaceDirty = false;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mInsertProgressResId = R.string.progress_saving_patch;
		mInsertedResId = R.string.alert_inserted_patch;

		mButtonTypeEvent = (RadioButton) findViewById(R.id.radio_event);
		mButtonTypeGroup = (RadioButton) findViewById(R.id.radio_group);
		mButtonTypePlace = (RadioButton) findViewById(R.id.radio_place);
		mButtonTypeProject = (RadioButton) findViewById(R.id.radio_project);
		mButtonGroupType = (RadioGroup) findViewById(R.id.buttons_type);

		mButtonPlace = (TextView) findViewById(R.id.button_place);
		mButtonPrivacy = (TextView) findViewById(R.id.button_privacy);
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapProgressBar = (AirProgressBar) findViewById(R.id.map_progress);
		mPhotoViewPlace = (AirImageView) findViewById(R.id.place_photo);
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

					if (mMapView.getViewTreeObserver().isAlive()) {
						mMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
							@SuppressWarnings("deprecation")
							// We use the new method when supported
							@SuppressLint("NewApi")
							// We check which build version we are using.
							@Override
							public void onGlobalLayout() {

								if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
									mMapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
								else {
									mMapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
								}
								drawLocation();
							}
						});
					}
				}
			});
		}
	}

	@Override
	public void bind(BindingMode mode) {
		super.bind(mode);

		Patch patch = (Patch) mEntity;

		if (!mEditing) {
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

		UI.setVisibility(findViewById(R.id.button_holder), (mEditing ? View.GONE : View.VISIBLE));

		/* Type */

		if (mButtonGroupType != null && patch.type != null) {
			Integer id = R.id.radio_event;
			if (patch.type.equals(Patch.Type.GROUP)) {
				id = R.id.radio_group;
			}
			else if (patch.type.equals(Patch.Type.PLACE)) {
				id = R.id.radio_place;
			}
			else if (patch.type.equals(Patch.Type.PROJECT)) {
				id = R.id.radio_project;
			}
			mButtonGroupType.check(id);
		}

		/* Linked place */

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

		super.draw(view);
	}

	public void drawLocation() {
		/*
		 * When creating a new patch we use the devices current location by default if
		 * available. If a location isn't available for whatever reason we show the
		 * map without a marker and say so in a label. From that point on, the only way
		 * a patch gets a location is if the user sets one or it is linked to a place.
		 *
		 * When linked to a place, the patch location is set to a copy of the place
		 * location. If the place link is later cleared, the patch location stays
		 * unchanged.
		 */
		Patch patch = (Patch) mEntity;  // Always set by the time we get here
		mLocationLabel.setText(StringManager.getString(R.string.label_location_provider_none));

		if (mMap != null) {
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

			if (patch.location != null) {

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
						.position(patch.location.asLatLng())
						.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
						.anchor(0.5f, 0.5f));

				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(((Patch) mEntity).location.asLatLng(), 17f));
				mMapView.setVisibility(View.VISIBLE);
				mMapProgressBar.hide();
				mMapView.invalidate();
			}
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
	public void onCancelEvent(ProcessingCanceledEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	public void onCreateButtonClick(View view) {
		onAccept();
	}

	public void onTypeLabelClick(View view) {
		mDirty = true;
		String type = (String) view.getTag();
		if (type.equals(Patch.Type.EVENT)) {
			mButtonTypeEvent.performClick();
		}
		else if (type.equals(Patch.Type.GROUP)) {
			mButtonTypeGroup.performClick();
		}
		else if (type.equals(Patch.Type.PLACE)) {
			mButtonTypePlace.performClick();
		}
		else if (type.equals(Patch.Type.PROJECT)) {
			mButtonTypeProject.performClick();
		}
	}

	public void onTypeButtonClick(View view) {

		mDirty = true;
		String type = (String) view.getTag();
		mButtonGroupType.check(view.getId());
		((Patch) mEntity).type = type;
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

			final Place place = (Place) mButtonPlace.getTag();

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

					/* Adding a new place. */
					if (place != null && com.aircandi.utilities.Type.isTrue(place.synthetic)) {
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
									, null, "insert_link_proximity", true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT, null
							);
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

	private void clearProximity() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.Refreshing);
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

		if (patch.type == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_patch_type)
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
			LocationManager.getInstance().start(true);  // Location triggers sequence
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
}