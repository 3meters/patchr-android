package com.patchr.ui.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.LocationStatusEvent;
import com.patchr.model.Location;
import com.patchr.objects.enums.FetchMode;
import com.patchr.objects.enums.LocationStatus;
import com.patchr.service.RestClient;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.atomic.AtomicBoolean;

public class NearbyListFragment extends EntityListFragment implements SwipeRefreshLayout.OnRefreshListener {

	private boolean atLeastOneLocationProcessed;
	protected AtomicBoolean locationDialogShot = new AtomicBoolean(false);

	@Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (listWidget != null) {
			listWidget.swipeRefresh.setOnRefreshListener(this);
		}
	}

	@Override public void onStart() {
		bindActionButton(); // User might have logged in/out while gone
		if (!Dispatcher.getInstance().isRegistered(this)) {
			Dispatcher.getInstance().register(this);
		}
		super.onStart();
	}

	@Override protected void doOnResume() {
		if (listWidget.query != null
			&& RestClient.getInstance().activityDateInsertDeletePatch > listWidget.query.activityDate) {
			Logger.d(this, "Calling activateNearby Manual: onResume: activityDateInsertDeletePatch triggered");
			activateNearby(FetchMode.MANUAL);
		}
		else {
			listWidget.draw();
			activateNearby(FetchMode.AUTO);
		}
	}

	@Override public void onStop() {
		super.onStop();

		/* Stop location updates */
		Dispatcher.getInstance().unregister(this);
		LocationManager.getInstance().stop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onRefresh() {
		Logger.d(this, "Calling activateNearby Manual: onRefresh");
		activateNearby(FetchMode.MANUAL);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onLocationStatusChanged(final LocationStatusEvent event) {
		if (event.status == LocationStatus.ALLOWED) {
			Logger.d(this, "Calling activateNearby Manual: Location allowed called");
			activateNearby(FetchMode.MANUAL);
			listWidget.emptyController.setText(StringManager.getString(R.string.empty_nearby));
		}
		else if (event.status == LocationStatus.DENIED) {
			listWidget.emptyController.setText("Location services disabled for Patchr");
			listWidget.emptyController.show(true);
		}
		else if (event.status == LocationStatus.UPDATED) {
			if (isResumed()) {
				if (event.location != null) {
					LocationManager.getInstance().setAndroidLocationLocked(event.location);
					final Location location = LocationManager.getInstance().getLocationLocked();
					if (location != null) {
						listWidget.fetch(FetchMode.AUTO);
						if (!atLeastOneLocationProcessed) {
							AsyncTask.execute(() -> MediaManager.playSound(MediaManager.SOUND_PLACES_FOUND, 1.0f, 1));
							atLeastOneLocationProcessed = true;
						}
					}
					else {
						listWidget.busyController.hide(true);
					}
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void activateNearby(FetchMode mode) {

		if (!PermissionUtil.hasSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
			requestPermissions();
			return;
		}

		/* Start location processing */
		if (LocationManager.getInstance().isLocationAccessEnabled()) {
			/*
			 * If manual mode then first location available is used and not possibly
			 * optimized out. This ensures that the location based patches will be rebuilt
			 * even if you haven't moved an inch.
			 */
			if (mode == FetchMode.MANUAL) {
				LocationManager.getInstance().stop();
				LocationManager.getInstance().start(true);  // Location update triggers fetch
			}
			else {
				LocationManager.getInstance().start(false); // Location update triggers fetch
			}
			return;
		}

		/* Let them know that location services are disabled */
		if (!locationDialogShot.get()) {
			Dialogs.locationServicesDisabled(getActivity(), locationDialogShot);
		}
		else {
			UI.toast(StringManager.getString(R.string.alert_location_services_disabled));
			showSnackbar(R.string.alert_location_permission_denied);
		}
	}

	public void bindActionButton() {

		if (listWidget.header != null) {
			TextView alertButton = (TextView) listWidget.header.findViewById(R.id.action_button);
			if (alertButton != null) {

				View rule = listWidget.header.findViewById(R.id.action_rule);
				if (rule != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					rule.setVisibility(View.GONE);
				}

				Boolean patched = (UserManager.currentUser.patchesOwned != null && UserManager.currentUser.patchesOwned > 0);
				alertButton.setText(patched ? R.string.button_alert_radar : R.string.button_alert_radar_no_patch);
			}
		}
	}

	private void showSnackbar(int textResId) {
		if (getView() != null) {
			Snackbar snackbar = Snackbar.make(getView(), "Snackbar", Snackbar.LENGTH_LONG);
			snackbar.setActionTextColor(Colors.getColor(R.color.brand_primary));
			snackbar.setText(textResId)
				.setAction("Settings", view -> {
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
					intent.setData(uri);
					startActivityForResult(intent, 100);
				}).show();
		}
	}

	private void requestPermissions() {

		if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

			getActivity().runOnUiThread(() -> {
				final AlertDialog dialog = Dialogs.alertDialog(null
					, StringManager.getString(R.string.alert_permission_location_title)
					, StringManager.getString(R.string.alert_permission_location_message)
					, null
					, getActivity()
					, R.string.alert_permission_location_positive
					, R.string.alert_permission_location_negative
					, null
					, (dialog1, which) -> {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							ActivityCompat.requestPermissions(getActivity()
								, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
								, Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
						}
						else {
							listWidget.busyController.hide(true);
							listWidget.emptyController.setText("Location services disabled for Patchr");
							listWidget.emptyController.show(true);
						}
					}, null);
				dialog.setCanceledOnTouchOutside(false);
			});
		}
		else {
			/*
			 * No explanation needed, we can request the permission.
			 * Parent activity will broadcast an event when permission request is complete.
			 */
			ActivityCompat.requestPermissions(getActivity()
				, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
				, Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
		}
	}
}