package com.patchr.ui.edit;

import android.Manifest;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.NetworkManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.events.LocationUpdatedEvent;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Entity;
import com.patchr.objects.Patch;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseEntityEdit;
import com.patchr.ui.widgets.AirPhotoView;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
public class PatchEdit extends BaseEntityEdit {

	private   TextView     mButtonPrivacy;
	private   TextView     mLocationLabel;
	protected AirPhotoView mPhotoViewPlace;

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

		mButtonPrivacy = (TextView) findViewById(R.id.button_privacy);
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapProgressBar = (AirProgressBar) findViewById(R.id.map_progress);
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
							//@SuppressLint("NewApi")
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

	public void onPrivacyBuilderClick(View view) {
		Patchr.router.route(this, Route.PRIVACY_EDIT, mEntity, null);
	}

	public void onLocationBuilderClick(View view) {
		Patchr.router.route(this, Route.LOCATION_EDIT, mEntity, null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_PRIVACY_EDIT) {
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
		if (mEditing) {
			update();
		}
		else {
			insert();
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
				return DataController.getInstance().trackEntity(mEntity, null, null, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
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
		if (PermissionUtil.hasSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
			if (!mEditing && LocationManager.getInstance().isLocationAccessEnabled()) {
				LocationManager.getInstance().start(true);  // Location triggers sequence
			}
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