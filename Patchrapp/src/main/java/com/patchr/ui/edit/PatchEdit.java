package com.patchr.ui.edit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import com.patchr.components.LocationManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.events.LocationUpdatedEvent;
import com.patchr.model.Location;
import com.patchr.objects.Entity;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.Command;
import com.patchr.objects.enums.PatchType;
import com.patchr.objects.enums.State;
import com.patchr.ui.InviteScreen;
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

	private RadioGroup  buttonPatchType;
	private RadioButton buttonTypeEvent;
	private RadioButton buttonTypeGroup;
	private RadioButton buttonTypePlace;
	private RadioButton buttonTypeTrip;

	protected MapView        mapView;
	protected AirProgressBar mapProgressBar;
	protected GoogleMap      map;
	protected Marker         marker;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
		draw();
	}

	@Override public void onResume() {
		if (PermissionUtil.hasSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
			if (inputState.equals(State.Creating) && LocationManager.getInstance().isLocationAccessEnabled()) {
				LocationManager.getInstance().start(true);  // Location triggers sequence
			}
		}
		super.onResume();
	}

	@Override public void onPause() {
		if (mapView != null) mapView.onPause();
		if (inputState.equals(State.Creating)) {
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

		if (inputState.equals(State.Editing)) {
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
						buttonPrivacy.setTag(privacy);
						draw();
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_LOCATION_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String json = extras.getString(Constants.EXTRA_LOCATION);
					if (json != null) {
						final Location location = Patchr.gson.fromJson(json, Location.class);
						location.provider = Constants.LOCATION_PROVIDER_USER;
						mapView.setTag(location);
						proximityDisabled = true;
						draw();
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
		String type = (String) view.getTag();
		if (type.equals(PatchType.EVENT)) {
			buttonTypeEvent.performClick();
		}
		else if (type.equals(PatchType.GROUP)) {
			buttonTypeGroup.performClick();
		}
		else if (type.equals(PatchType.PLACE)) {
			buttonTypePlace.performClick();
		}
		else if (type.equals(PatchType.TRIP)) {
			buttonTypeTrip.performClick();
		}
	}

	public void onTypeButtonClick(View view) {
		buttonPatchType.setTag((String) view.getTag());
		draw();
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

			LocationManager.getInstance().setAndroidLocationLocked(event.location);
			Location location = LocationManager.getInstance().getLocationLocked();

			if (location != null) {
				mapView.setTag(location);
				drawLocation();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		insertProgressResId = R.string.progress_saving_patch;
		insertedResId = R.string.alert_inserted_patch;

		buttonTypeEvent = (RadioButton) findViewById(R.id.radio_event);
		buttonTypeGroup = (RadioButton) findViewById(R.id.radio_group);
		buttonTypePlace = (RadioButton) findViewById(R.id.radio_place);
		buttonTypeTrip = (RadioButton) findViewById(R.id.radio_trip);
		buttonPatchType = (RadioGroup) findViewById(R.id.buttons_type);

		buttonPrivacy = (TextView) findViewById(R.id.privacy_policy_button);
		title = (TextView) findViewById(R.id.title);
		mapView = (MapView) findViewById(R.id.mapview);
		mapProgressBar = (AirProgressBar) findViewById(R.id.map_progress);
		locationLabel = (TextView) findViewById(R.id.location_label);

		Drawable originalDrawable = buttonPrivacy.getCompoundDrawables()[2];
		Drawable chevron = UI.setTint(originalDrawable, R.color.brand_primary);
		buttonPrivacy.setCompoundDrawables(null, null, chevron, null);

		this.actionBarTitle.setText(inputState.equals(State.Editing) ? R.string.screen_title_patch_edit : R.string.screen_title_patch_new);
		this.title.setText(inputState.equals(State.Editing) ? R.string.screen_title_patch_edit : R.string.screen_title_patch_new);

		if (mapView != null) {

			mapView.onCreate(null);
			mapView.onResume();
			mapView.getMapAsync(new OnMapReadyCallback() {
				@Override public void onMapReady(GoogleMap googleMap) {
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
								drawLocation();
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
		if (entity != null) {
			mapView.setTag(entity.getLocation());

			/* Visibility */
			if (buttonPrivacy != null) {
				buttonPrivacy.setTag(entity.visibility);
			}

			/* Type */
			if (buttonPatchType != null) {
				buttonPatchType.setTag(entity.type);
			}
		}
		else {
			Location location = LocationManager.getInstance().getLocationLocked();
			if (location != null) {
				mapView.setTag(location);
			}

			buttonPrivacy.setTag(Constants.PRIVACY_PUBLIC);
		}
	}

	public void draw() {

		/* Visibility */
		if (buttonPrivacy != null) {
			String visibility = (String) buttonPrivacy.getTag();
			String value = (visibility.equals(Constants.PRIVACY_PUBLIC))
			               ? StringManager.getString(R.string.label_patch_privacy_public)
			               : StringManager.getString(R.string.label_patch_privacy_private);
			buttonPrivacy.setText(StringManager.getString(R.string.label_patch_edit_privacy) + ": " + value);
		}

		/* Type */
		if (buttonPatchType != null) {
			String type = (String) buttonPatchType.getTag();
			if (type != null) {
				Integer id = R.id.radio_event;
				if (type.equals(PatchType.GROUP)) {
					id = R.id.radio_group;
				}
				else if (type.equals(PatchType.PLACE)) {
					id = R.id.radio_place;
				}
				else if (type.equals(PatchType.TRIP)) {
					id = R.id.radio_trip;
				}
				buttonPatchType.check(id);
			}
		}

		/* Location */
		drawLocation();
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
		locationLabel.setText(StringManager.getString(R.string.label_location_provider_none));

		if (map != null) {

			map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			Location location = (Location) mapView.getTag();

			if (location != null) {

				locationLabel.setText(StringManager.getString(R.string.label_location_provider_google));
				if (location.provider != null) {
					if (location.provider.equals(Constants.LOCATION_PROVIDER_USER)) {
						locationLabel.setText(StringManager.getString(R.string.label_location_provider_user));
					}
					else if (location.provider.equals(Constants.LOCATION_PROVIDER_GOOGLE)) {
						if (inputState.equals(State.Editing)) {
							locationLabel.setText(StringManager.getString(R.string.label_location_provider_google));
						}
						else {
							locationLabel.setText(StringManager.getString(R.string.label_location_provider_google_new));
						}
					}
				}

				map.clear();
				map.addMarker(new MarkerOptions()
					.position(location.asLatLng())
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.img_patch_marker))
					.anchor(0.5f, 0.5f));

				map.moveCamera(CameraUpdateFactory.newLatLngZoom(location.asLatLng(), 17f));
				mapView.setVisibility(View.VISIBLE);
				mapProgressBar.hide();
				mapView.invalidate();
			}
		}
	}

	public void gather(SimpleMap parameters) {

		Location location = (Location) mapView.getTag();
		if (location != null) {
			parameters.put("location", location);
		}

		parameters.put("type", buttonPatchType.getTag());
		parameters.put("visibility", buttonPrivacy.getTag());
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

	@Override protected boolean isValid() {

		if (entity.name == null) {
			Dialogs.alert(R.string.error_missing_patch_name, this);
			return false;
		}

		if (entity.type == null) {
			Dialogs.alert(R.string.error_missing_patch_type, this);
			return false;
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_patch;
	}
}