package com.aircandi.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityController;
import com.aircandi.components.ProximityController.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.ProcessingCanceledEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Patch;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.List;

@SuppressLint("Registered")
public class ProximityEdit extends BaseEntityEdit {

	private Button mButtonTune;
	private Button mButtonUntune;

	private Boolean mTuned           = false;
	private Boolean mUntuned         = false;
	private Boolean mTuningInProcess = false;
	private Boolean mUntuning        = false;
	private Boolean mFirstTune       = true;
	private Boolean mPlaceDirty      = false;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mButtonTune = (Button) findViewById(R.id.button_tune);
		mButtonUntune = (Button) findViewById(R.id.button_untune);
	}

	@Override
	public void bind(BindingMode mode) {
		mEntity = DataController.getStoreEntity(mEntityId);
		draw(null);
	}

	@Override
	public void draw(View view) {
		/*
		 * Only called when the activity is first created.
		 */
		Patch patch = (Patch) mEntity;

		/* Tuning buttons */

		final Boolean hasActiveProximityLink = patch.hasActiveProximity();
		if (hasActiveProximityLink) {
			mFirstTune = false;
			UI.setVisibility(mButtonUntune, View.VISIBLE);
		}

		super.draw(view);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
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
					Logger.d(ProximityEdit.this, "Beacons locked event: tune entity");
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
			mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_tuning, ProximityEdit.this);
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
			mUiController.getBusyController().show(BusyAction.ActionWithMessage, R.string.progress_tuning, ProximityEdit.this);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityController.getInstance().scanForWifi(ScanReason.QUERY);
			}
			else {
				tuneProximity();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(R.string.form_title_proximity_edit);
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
				mUiController.getBusyController().show(BusyAction.Refreshing);
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
				AnimationManager.doOverridePendingTransition(ProximityEdit.this, TransitionType.FORM_BACK);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override
	protected int getLayoutId() {
		return (mLayoutResId != null && mLayoutResId != 0) ? mLayoutResId : R.layout.proximity_edit;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
}