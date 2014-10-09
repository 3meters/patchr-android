package com.aircandi.ui.edit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.ui.widgets.ComboButton;
import com.squareup.otto.Subscribe;

import java.util.List;

public class TuningEdit extends BaseEntityEdit {

	private ComboButton mButtonTune;
	private ComboButton mButtonUntune;
	private View        mHolderEditing;

	private Boolean mTuned           = false;
	private Boolean mUntuned         = false;
	private Boolean mTuningInProcess = false;
	private Boolean mUntuning        = false;
	private Boolean mFirstTune       = true;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mButtonTune = (ComboButton) findViewById(R.id.button_tune);
		mButtonUntune = (ComboButton) findViewById(R.id.button_untune);
		mHolderEditing = findViewById(R.id.holder_editing);
	}

	@Override
	public void draw(View view) {

		if (mEntity != null) {

			/* Edit or not */

			mHolderEditing.setVisibility(Patchr.getInstance().getMenuManager().showAction(Route.EDIT, mEntity, mForId) ? View.VISIBLE : View.GONE);
			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				mHolderEditing.setVisibility(View.GONE);
			}

			/* Color */

			final Entity entity = mEntity;

			/* Tuning buttons */
			final Boolean hasActiveProximityLink = entity.hasActiveProximity();
			if (hasActiveProximityLink) {
				mFirstTune = false;
				mButtonUntune.setVisibility(View.VISIBLE);
			}

			String labelTune;
			String labelBanner;
			String labelEdit;

			if (!TextUtils.isEmpty(entity.name)) {
				labelTune = String.format(StringManager.getString(R.string.label_tuning_tune_name), entity.name);
				labelBanner = String.format(StringManager.getString(R.string.label_tuning_banner_name), entity.name);
				labelEdit = String.format(StringManager.getString(R.string.label_tuning_edit_name), entity.name);
				((TextView) findViewById(R.id.label_tune)).setText(labelTune);
				((TextView) findViewById(R.id.label_banner)).setText(labelBanner);
				if (findViewById(R.id.label_edit) != null) {
					((TextView) findViewById(R.id.label_edit)).setText(labelEdit);
				}
			}

			drawPhoto();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		Patchr.dispatch.route(this, Route.EDIT, mEntity, null, null);
	}

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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected String getLinkType() {
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(TuningEdit.this, "Query wifi scan received event: locking beacons");
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
					Logger.d(TuningEdit.this, "Beacons locked event: tune entity");
					tuneProximity();
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/
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
		final List<Beacon> beacons = ProximityManager.getInstance().getStrongestBeacons(ServiceConstants.PROXIMITY_BEACON_COVERAGE);
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

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected int getLayoutId() {
		return R.layout.tuning_edit;
	}
}