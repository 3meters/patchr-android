package com.patchr.ui.edit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.ProximityController;
import com.patchr.components.ProximityController.ScanReason;
import com.patchr.components.StringManager;
import com.patchr.events.BeaconsLockedEvent;
import com.patchr.events.QueryWifiScanReceivedEvent;
import com.patchr.objects.Beacon;
import com.patchr.objects.Patch;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;

import java.util.List;

@SuppressLint("Registered")
public class ProximityEdit extends BaseEdit {

	private Button   buttonTune;
	private Button   buttonUntune;
	private TextView title;

	private Boolean tuned           = false;
	private Boolean untuned         = false;
	private Boolean tuningInProcess = false;
	private Boolean untuning        = false;
	private Boolean firstTune       = true;
	private Boolean placeDirty      = false;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onTuneButtonClick(View view) {
		if (!tuned) {
			untuning = false;
			busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_tuning, ProximityEdit.this);
			if (NetworkManager.getInstance().isWifiEnabled()
					&& PermissionUtil.hasSelfPermission(Patchr.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
				tuningInProcess = true;
				ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	public void onUntuneButtonClick(View view) {
		if (!untuned) {
			untuning = true;
			busyPresenter.show(BusyPresenter.BusyAction.ActionWithMessage, R.string.progress_tuning, ProximityEdit.this);
			if (NetworkManager.getInstance().isWifiEnabled()
					&& PermissionUtil.hasSelfPermission(Patchr.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
				tuningInProcess = true;
				ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (tuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(ProximityEdit.this, "Query wifi scan received event: locking beacons");
					if (event.wifiList != null) {
						ProximityController.getInstance().lockBeacons();
					}
					else {
					    /*
					     * We fake that the tuning happened because it is simpler than enabling/disabling ui
						 */
						busyPresenter.hide(false);
						if (untuning) {
							buttonUntune.setText(R.string.button_tuning_tuned);
							untuned = true;
						}
						else {
							buttonTune.setText(R.string.button_tuning_tuned);
							tuned = true;
						}
						buttonTune.forceLayout();
						buttonUntune.forceLayout();
						tuningInProcess = false;
					}
				}
			});
		}
	}

	@Subscribe public void onBeaconsLocked(BeaconsLockedEvent event) {

		if (tuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(ProximityEdit.this, "Beacons locked event: tune entity");
					tuneProximity();
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		buttonTune = (Button) findViewById(R.id.tune_button);
		buttonUntune = (Button) findViewById(R.id.button_untune);
		title = (TextView) findViewById(R.id.title);
	}

	@Override public void bind() {
		super.bind();

		entity = DataController.getStoreEntity(entityId);
		Patch patch = (Patch) entity;

		final Boolean hasActiveProximityLink = patch.hasActiveProximity();
		if (hasActiveProximityLink) {
			firstTune = false;
			UI.setVisibility(buttonUntune, View.VISIBLE);
		}
		actionBar.setTitle(R.string.screen_title_proximity_edit);
		title.setText(patch.name);
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_proximity;
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
		Integer beaconMax = !untuning ? Constants.PROXIMITY_BEACON_COVERAGE : Constants.PROXIMITY_BEACON_UNCOVERAGE;
		final List<Beacon> beacons = ProximityController.getInstance().getStrongestBeacons(beaconMax);
		final Beacon primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.Refreshing);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncTrackEntityProximity");

				final ModelResult result = DataController.getInstance().trackEntity(entity
						, beacons
						, primaryBeacon
						, untuning, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

				return result;
			}

			@Override protected void onPostExecute(Object response) {
				busyPresenter.hide(false);

				if (tuned || untuned) {
				    /* Undoing a tuning */
					buttonTune.setText(R.string.button_tuning_tune);
					buttonUntune.setText(R.string.button_tuning_untune);
					untuned = false;
					tuned = false;
					if (!firstTune) {
						buttonUntune.setVisibility(View.VISIBLE);
					}
					else {
						buttonUntune.setVisibility(View.GONE);
					}
				}
				else {
					/* Tuning or untuning */
					if (untuning) {
						buttonUntune.setText(R.string.button_tuning_tuned);
						buttonTune.setText(R.string.button_tuning_undo);
						untuned = true;
					}
					else {
						buttonTune.setText(R.string.button_tuning_tuned);
						buttonUntune.setText(R.string.button_tuning_undo);
						tuned = true;
						if (buttonUntune.getVisibility() != View.VISIBLE) {
							buttonUntune.setVisibility(View.VISIBLE);
						}
					}
				}
				buttonTune.forceLayout();
				buttonUntune.forceLayout();
				tuningInProcess = false;
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	private void clearProximity() {

		new AsyncTask() {

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyPresenter.BusyAction.Refreshing);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncClearEntityProximity");
				final ModelResult result = DataController.getInstance().trackEntity(entity, null, null, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
				return result;
			}

			@Override protected void onPostExecute(Object response) {
				busyPresenter.hide(false);
				UI.toast(StringManager.getString(updatedResId));
				setResult(Activity.RESULT_OK);
				finish();
				AnimationManager.doOverridePendingTransition(ProximityEdit.this, TransitionType.FORM_BACK);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}