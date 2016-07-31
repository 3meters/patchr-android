package com.patchr.ui.edit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.LocationManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.events.LocationStatusEvent;
import com.patchr.model.Location;
import com.patchr.model.Photo;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.FetchStrategy;
import com.patchr.objects.enums.PatchType;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.InviteSwitchboardScreen;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

@SuppressLint("Registered")
public class PatchEdit extends BaseEdit {

	private TextView privacyButton;
	private TextView locationLabel;
	private TextView title;

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
			if (inputState.equals(State.Inserting) && LocationManager.getInstance().isLocationAccessEnabled()) {
				LocationManager.getInstance().start(true);  // Location triggers sequence
			}
		}
		if (mapView != null) {
			mapView.onResume();
		}
		super.onResume();
	}

	@Override public void onPause() {
		if (mapView != null) mapView.onPause();
		if (inputState.equals(State.Inserting)) {
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
		else if (item.getItemId() == R.id.submit || item.getItemId() == R.id.next) {
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
						privacyButton.setTag(privacy);
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

	@Override public void submitAction() {

		if (!isValid()) return;
		if (!processing) {
			processing = true;
			SimpleMap parameters = new SimpleMap();
			gather(parameters);
			post(parameters);
		}
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

		final Intent intent = new Intent(this, PrivacyEdit.class);
		intent.putExtra(Constants.EXTRA_PRIVACY, (String) privacyButton.getTag());
		startActivityForResult(intent, Constants.ACTIVITY_PRIVACY_EDIT);
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_TO);
	}

	public void onLocationBuilderClick(View view) {

		Intent intent = new Intent(this, LocationEdit.class);
		if (mapView != null && mapView.getTag() != null) {
			Location location = (Location) mapView.getTag();
			String locationJson = Patchr.gson.toJson(location);
			intent.putExtra(Constants.EXTRA_LOCATION, locationJson);
		}
		startActivityForResult(intent, Constants.ACTIVITY_LOCATION_EDIT);
		AnimationManager.doOverridePendingTransition(this, TransitionType.BUILDER_TO);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onLocationChanged(final LocationStatusEvent event) {
		/*
		 * Getting location updates because we are inserting a patch and
		 * location services are enabled.
		 */
		if (event.location != null) {

			LocationManager.getInstance().setAndroidLocationLocked(event.location);
			Location location = LocationManager.getInstance().getLocationLocked();

			if (location != null && mapView != null) {
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

		entitySchema = Constants.SCHEMA_ENTITY_PATCH;
		insertProgressResId = R.string.progress_saving_patch;
		insertedResId = R.string.alert_inserted_patch;

		buttonTypeEvent = (RadioButton) findViewById(R.id.radio_event);
		buttonTypeGroup = (RadioButton) findViewById(R.id.radio_group);
		buttonTypePlace = (RadioButton) findViewById(R.id.radio_place);
		buttonTypeTrip = (RadioButton) findViewById(R.id.radio_trip);
		buttonPatchType = (RadioGroup) findViewById(R.id.buttons_type);

		privacyButton = (TextView) findViewById(R.id.privacy_policy_button);
		title = (TextView) findViewById(R.id.title);
		mapView = (MapView) findViewById(R.id.mapview);
		mapProgressBar = (AirProgressBar) findViewById(R.id.map_progress);
		locationLabel = (TextView) findViewById(R.id.location_label);

		Drawable originalDrawable = privacyButton.getCompoundDrawables()[2];
		Drawable chevron = UI.setTint(originalDrawable, R.color.brand_primary);
		privacyButton.setCompoundDrawables(null, null, chevron, null);

		this.actionBarTitle.setText(inputState.equals(State.Editing) ? R.string.screen_title_patch_edit : R.string.screen_title_patch_new);
		this.title.setText(inputState.equals(State.Editing) ? R.string.screen_title_patch_edit : R.string.screen_title_patch_new);

		if (mapView != null) {
			mapView.onCreate(null);
			mapView.getMapAsync(googleMap -> {
				map = googleMap;
				map.getUiSettings().setMapToolbarEnabled(false);
				drawLocation();
			});
		}
	}

	@Override public void bind() {
		super.bind();

		/* Only called when the activity is first created. */
		if (entity != null) {
			if (mapView != null) {
				mapView.setTag(entity.getLocation());
			}

			/* Visibility */
			if (privacyButton != null) {
				privacyButton.setTag(entity.visibility);
			}

			/* Type */
			if (buttonPatchType != null) {
				buttonPatchType.setTag(entity.type);
			}
		}
		else {
			Location location = LocationManager.getInstance().getLocationLocked();
			if (location != null && mapView != null) {
				mapView.setTag(location);
			}

			privacyButton.setTag(Constants.PRIVACY_PUBLIC);
		}
	}

	public void draw() {

		/* Visibility */
		if (privacyButton != null) {
			String visibility = (String) privacyButton.getTag();
			String value = (visibility.equals(Constants.PRIVACY_PUBLIC))
			               ? StringManager.getString(R.string.label_patch_privacy_public)
			               : StringManager.getString(R.string.label_patch_privacy_private);
			privacyButton.setText(StringManager.getString(R.string.label_patch_edit_privacy) + ": " + value);
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
		super.gather(parameters);   // name, photo, description

		if (mapView != null) {
			Location location = (Location) mapView.getTag();
			if (location != null) {
				parameters.put("location", location.asMap());
			}
		}

		parameters.put("type", buttonPatchType.getTag());
		parameters.put("visibility", privacyButton.getTag());
	}

	protected void post(SimpleMap data) {

		String path = entity == null ? "data/patches" : String.format("data/patches/%1$s", entity.id);
		busyController.show(BusyController.BusyAction.ActionWithMessage, insertProgressResId, PatchEdit.this);

		AsyncTask.execute(() -> {

			if (data.containsKey("photo")) {
				Photo photo = Photo.setPropertiesFromMap(new Photo(), (SimpleMap) data.get("photo"));
				if (photo != null) {
					Photo photoFinal = postPhotoToS3(photo);
					data.put("photo", photoFinal);
				}
			}

			subscription = RestClient.getInstance().postEntity(path, data)
				.flatMap(response -> {
					String entityId = response.data.get(0).id;
					return RestClient.getInstance().fetchEntity(entityId, FetchStrategy.IgnoreCache);
				})
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);
						if (inputState.equals(State.Inserting)) {
							RestClient.getInstance().activityDateInsertDeletePatch = DateTime.nowDate().getTime();
							UI.toast(StringManager.getString(insertedResId));
							String entityId = response.data.get(0).id;
							Intent intent = new Intent(this, InviteSwitchboardScreen.class)
								.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
							startActivity(intent);
						}
						finish();
						AnimationManager.doOverridePendingTransition(PatchEdit.this, TransitionType.FORM_BACK);
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(this, error);
					});
		});
	}

	protected boolean isDirty() {

		if (inputState.equals(State.Inserting)) {
			if (privacyButton != null && !privacyButton.getTag().equals(Constants.PRIVACY_PUBLIC)) {
				return true;
			}
			if (buttonPatchType != null && buttonPatchType.getTag() != null) {
				return true;
			}
		}
		else if (inputState.equals(State.Editing)) {
			if (privacyButton != null && !privacyButton.getTag().equals(entity.visibility)) {
				return true;
			}
			if (buttonPatchType != null && !buttonPatchType.getTag().equals(entity.type)) {
				return true;
			}
			if (mapView != null && !entity.getLocation().sameAs((Location) mapView.getTag())) {
				return true;
			}
		}

		return super.isDirty();
	}

	@Override protected boolean isValid() {

		if (TextUtils.isEmpty(nameField.getText().toString().trim())) {
			Dialogs.alert(R.string.error_missing_patch_name, this);
			return false;
		}

		if (buttonPatchType.getTag() == null) {
			Dialogs.alert(R.string.error_missing_patch_type, this);
			return false;
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_patch;
	}
}