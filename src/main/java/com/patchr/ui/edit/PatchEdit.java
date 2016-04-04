package com.patchr.ui.edit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

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
import com.patchr.objects.AirLocation;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.Patch;
import com.patchr.objects.TransitionType;
import com.patchr.ui.InviteScreen;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

@SuppressLint("Registered")
public class PatchEdit extends BaseEdit {

	private   TextView    buttonPrivacy;
	private   TextView    locationLabel;
	protected ImageWidget photoViewPlace;
	private   TextView    title;

	private RadioGroup  buttonGroupType;
	private RadioButton buttonTypeEvent;
	private RadioButton buttonTypeGroup;
	private RadioButton buttonTypePlace;
	private RadioButton buttonTypeProject;

	protected MapView        mapView;
	protected AirProgressBar mapProgressBar;
	protected GoogleMap      map;
	protected Marker         marker;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	@Override public void onResume() {
		if (PermissionUtil.hasSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
			if (!editing && LocationManager.getInstance().isLocationAccessEnabled()) {
				LocationManager.getInstance().start(true);  // Location triggers sequence
			}
		}
		super.onResume();
	}

	@Override public void onPause() {
		if (mapView != null) mapView.onPause();
		if (!editing) {
			LocationManager.getInstance().stop();
		}
		super.onPause();
	}

	@Override public void onDestroy() {
		super.onDestroy();
		if (mapView != null) mapView.onDestroy();
	}

	@Override public void onLowMemory() {
		super.onLowMemory();
		if (mapView != null) mapView.onLowMemory();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (editing) {
			getMenuInflater().inflate(R.menu.menu_save, menu);
			getMenuInflater().inflate(R.menu.menu_delete, menu);
		}
		else {
			getMenuInflater().inflate(R.menu.menu_next, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.delete) {
			confirmDelete();
		}
		else if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_PRIVACY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String privacy = extras.getString(Constants.EXTRA_PRIVACY);
					if (privacy != null) {
						dirty = true;
						((Patch) entity).privacy = privacy;
						bind();
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_LOCATION_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_LOCATION);
					if (json != null) {
						final AirLocation location = (AirLocation) Json.jsonToObject(json, Json.ObjectType.AIR_LOCATION);
						dirty = true;
						((Patch) entity).location = location;
						((Patch) entity).location.provider = Constants.LOCATION_PROVIDER_USER;
						bind();
						proximityDisabled = true;
					}
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (mapView != null) mapView.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	public void onCreateButtonClick(View view) {
		super.submitAction();
	}

	public void onTypeLabelClick(View view) {
		dirty = true;
		String type = (String) view.getTag();
		if (type.equals(Patch.Type.EVENT)) {
			buttonTypeEvent.performClick();
		}
		else if (type.equals(Patch.Type.GROUP)) {
			buttonTypeGroup.performClick();
		}
		else if (type.equals(Patch.Type.PLACE)) {
			buttonTypePlace.performClick();
		}
		else if (type.equals(Patch.Type.PROJECT)) {
			buttonTypeProject.performClick();
		}
	}

	public void onTypeButtonClick(View view) {

		dirty = true;
		String type = (String) view.getTag();
		buttonGroupType.check(view.getId());
		((Patch) entity).type = type;
	}

	public void onPrivacyBuilderClick(View view) {
		Patchr.router.route(this, Command.PRIVACY_EDIT, entity, null);
	}

	public void onLocationBuilderClick(View view) {
		Patchr.router.route(this, Command.LOCATION_EDIT, entity, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onLocationChanged(final LocationUpdatedEvent event) {
		/*
		 * Getting location updates because we are inserting a patch and
		 * location services are enabled.
		 */
		if (event.location != null) {

			LocationManager.getInstance().setLocationLocked(event.location);
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();

			if (location != null) {

				Patch patch = (Patch) entity;
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
				bindLocation();
			}
		}
	}

	@Subscribe public void onCancelEvent(ProcessingCanceledEvent event) {
		if (taskService != null) {
			taskService.cancel(true);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		Patch patch = (Patch) this.entity;

		if (!this.editing) {
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

		insertProgressResId = R.string.progress_saving_patch;
		insertedResId = R.string.alert_inserted_patch;

		buttonTypeEvent = (RadioButton) findViewById(R.id.radio_event);
		buttonTypeGroup = (RadioButton) findViewById(R.id.radio_group);
		buttonTypePlace = (RadioButton) findViewById(R.id.radio_place);
		buttonTypeProject = (RadioButton) findViewById(R.id.radio_project);
		buttonGroupType = (RadioGroup) findViewById(R.id.buttons_type);

		buttonPrivacy = (TextView) findViewById(R.id.privacy_policy_button);
		title = (TextView) findViewById(R.id.title);
		mapView = (MapView) findViewById(R.id.mapview);
		mapProgressBar = (AirProgressBar) findViewById(R.id.map_progress);
		locationLabel = (TextView) findViewById(R.id.location_label);

		this.actionBarTitle.setText(editing ? R.string.screen_title_patch_edit : R.string.screen_title_patch_new);
		this.title.setText(editing ? R.string.screen_title_patch_edit : R.string.screen_title_patch_new);

		if (mapView != null) {

			mapView.onCreate(null);
			mapView.onResume();
			mapView.getMapAsync(new OnMapReadyCallback() {
				@Override
				public void onMapReady(GoogleMap googleMap) {

					map = googleMap;
					map.getUiSettings().setMapToolbarEnabled(false);
					MapsInitializer.initialize(Patchr.applicationContext); // Initializes BitmapDescriptorFactory

					if (mapView.getViewTreeObserver().isAlive()) {
						mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
							@SuppressWarnings("deprecation")
							// We use the new method when supported
							//@SuppressLint("NewApi")
							// We check which build version we are using.
							@Override
							public void onGlobalLayout() {

								if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
									mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
								else {
									mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
								}
								bindLocation();
							}
						});
					}
				}
			});
		}
	}

	@Override public void bind() {
		super.bind();

		/* Only called when the activity is first created. */

		Patch patch = (Patch) entity;

		bindLocation();

		if (buttonPrivacy != null) {
			buttonPrivacy.setTag(patch.privacy);
			String value = (patch.privacy.equals(Constants.PRIVACY_PUBLIC))
			               ? StringManager.getString(R.string.label_patch_privacy_public)
			               : StringManager.getString(R.string.label_patch_privacy_private);
			buttonPrivacy.setText(StringManager.getString(R.string.label_patch_edit_privacy) + ": " + value);
		}

		/* Type */

		if (buttonGroupType != null && patch.type != null) {
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
			buttonGroupType.check(id);
		}
	}

	@Override protected boolean afterInsert(Entity insertedEntity) {

	    /* Only called if the insert was successful. Called on main ui thread. */
		if (insertedResId != null && insertedResId != 0) {
			UI.toast(StringManager.getString(insertedResId));
		}

		Bundle extras = new Bundle();
		final String jsonEntity = Json.objectToJson(insertedEntity);
		extras.putString(Constants.EXTRA_ENTITY, jsonEntity);
		startActivity(new Intent(this, InviteScreen.class).putExtras(extras));
		finish();

		return false;       // We are handling the finish
	}

	@Override protected boolean afterUpdate() {
	    /*
	     * Only called if the update was successful. Called on main ui thread.
	     * If proximity links are now invalid because the patch was manually moved
	     * to a new location then clear all the links.
		 */
		if (proximityDisabled) {
			clearProximity();
			return false;
		}
		return true;
	}

	@Override protected boolean validate() {

		gather();
		Patch patch = (Patch) entity;
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

	@Override protected String getEntitySchema() {
		return Constants.SCHEMA_ENTITY_PATCH;
	}

	@Override protected String getLinkType() {
		return Constants.TYPE_LINK_PROXIMITY;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_patch;
	}

	public void bindLocation() {
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
		Patch patch = (Patch) entity;  // Always set by the time we get here
		locationLabel.setText(StringManager.getString(R.string.label_location_provider_none));

		if (map != null) {
			map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

			if (patch.location != null) {

				locationLabel.setText(StringManager.getString(R.string.label_location_provider_google));
				if (patch.location.provider != null) {
					if (patch.location.provider.equals(Constants.LOCATION_PROVIDER_USER)) {
						locationLabel.setText(StringManager.getString(R.string.label_location_provider_user));
					}
					else if (patch.location.provider.equals(Constants.LOCATION_PROVIDER_GOOGLE)) {
						if (editing) {
							locationLabel.setText(StringManager.getString(R.string.label_location_provider_google));
						}
						else {
							locationLabel.setText(StringManager.getString(R.string.label_location_provider_google_new));
						}
					}
				}

				map.clear();
				map.addMarker(new MarkerOptions()
						.position(patch.location.asLatLng())
						.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
						.anchor(0.5f, 0.5f));

				map.moveCamera(CameraUpdateFactory.newLatLngZoom(((Patch) entity).location.asLatLng(), 17f));
				mapView.setVisibility(View.VISIBLE);
				mapProgressBar.hide();
				mapView.invalidate();
			}
		}
	}

	private void clearProximity() {

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.Refreshing);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncClearEntityProximity");
				return DataController.getInstance().trackEntity(entity, null, null, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				busyPresenter.hide(false);
				UI.toast(StringManager.getString(updatedResId));
				setResult(Activity.RESULT_OK);
				finish();
				AnimationManager.doOverridePendingTransition(PatchEdit.this, TransitionType.FORM_BACK);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}