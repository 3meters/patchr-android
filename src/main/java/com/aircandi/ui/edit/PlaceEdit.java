package com.aircandi.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.components.TabManager;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.CancelEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Category;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Message;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.interfaces.IBusy;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.utilities.ViewId;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.TokenCompleteTextView;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.ToolTipView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Registered")
public class PlaceEdit extends BaseEntityEdit
		implements TokenCompleteTextView.TokenListener {

	protected AirTokenCompleteTextView mTo;
	protected EntitySuggestController  mEntitySuggest;
	protected List<Entity> mTos = new ArrayList<Entity>();
	protected ToolTipRelativeLayout mTooltips;

	private TabManager  mTabManager;
	private ComboButton mButtonTune;
	private ComboButton mButtonUntune;

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

		if (mEntity == null || (mEntity.ownerId != null && (mEntity.ownerId.equals(Patch.getInstance().getCurrentUser().id)))) {
			ViewFlipper flipper = (ViewFlipper) findViewById(R.id.flipper_form);
			if (flipper != null) {
				mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
				mTabManager.initialize();
				mTabManager.doRestoreInstanceState(savedInstanceState);
			}
		}

		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		if (mTooltips != null) {
			mTooltips.setSingleShot(Constants.TOOLTIPS_PLACE_EDIT_ID);
		}

		mTo = (AirTokenCompleteTextView) findViewById(com.aircandi.R.id.to);
		if (mTo != null) {
			mTo.setLineSpacing((int) UI.getRawPixelsForDisplayPixels(5f), 1f);
			mTo.setTokenLayoutResId(com.aircandi.R.layout.widget_token_view);
			mTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						UI.showSoftInput(mTo);
					}
				}
			});
			mEntitySuggest = new EntitySuggestController(this)
					.setInput(mTo)
					.setTokenListener(this)
					.setSuggestScope(EntityManager.SuggestScope.USERS);
			mEntitySuggest.init();
		}
	}

	@Override
	public void draw(View view) {

		/* Place content */
		Place place = (Place) mEntity;

		if (findViewById(R.id.address) != null) {
			final String addressBlock = place.getAddressBlock();
			if (!addressBlock.equals("")) {
				((BuilderButton) findViewById(R.id.address)).setText(place.address);
			}
		}
		if (place.category != null) {
			if (findViewById(R.id.category) != null) {
				((BuilderButton) findViewById(R.id.category)).setText(place.category.name);
			}
		}

		/* Tuning buttons */
		UI.setVisibility(findViewById(R.id.group_tune), View.GONE);
		if (mEditing) {
			UI.setVisibility(findViewById(R.id.group_tune), View.VISIBLE);
			final Boolean hasActiveProximityLink = place.hasActiveProximity();
			if (hasActiveProximityLink) {
				mFirstTune = false;
				mButtonUntune.setVisibility(View.VISIBLE);
			}
		}

		/* Help message */
		UI.setVisibility(findViewById(R.id.label_message), mEditing ? View.GONE : View.VISIBLE);
		UI.setVisibility(findViewById(R.id.label_to), mEditing ? View.GONE : View.VISIBLE);
		UI.setVisibility(findViewById(R.id.to), mEditing ? View.GONE : View.VISIBLE);

		super.draw(view);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

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

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(PlaceEdit.this, "Query wifi scan received event: locking beacons");
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
					Logger.d(PlaceEdit.this, "Beacons locked event: tune entity");
					tuneProximity();
				}
			});
		}
	}

	@Subscribe
	public void onCancelEvent(CancelEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	@SuppressWarnings("ucd")
	public void onAddressBuilderClick(View view) {
		Patch.dispatch.route(this, Route.ADDRESS_EDIT, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		Patch.dispatch.route(this, Route.CATEGORY_EDIT, mEntity, null, null);
	}

	public void onTokenAdded(Object o) {
		if (!mTos.contains((Entity) o)) {
			mTos.add((Entity) o);
			mDirty = true;
		}
	}

	public void onTokenRemoved(Object o) {
		if (mTos.contains((Entity) o)) {
			mTos.remove((Entity) o);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mTooltips != null) {
			View view = findViewById(R.id.tooltips);
			view.post(new Runnable() {
				@Override
				public void run() {
					showTooltips();
				}
			});
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ADDRESS_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					mDirty = true;
					final Bundle extras = intent.getExtras();

					final String jsonPlace = extras.getString(Constants.EXTRA_PLACE);
					if (jsonPlace != null) {
						final Place placeUpdated = (Place) Json.jsonToObject(jsonPlace, Json.ObjectType.ENTITY);
						if (placeUpdated.phone != null) {
							placeUpdated.phone = placeUpdated.phone.replaceAll("[^\\d.]", "");
						}
						mEntity = placeUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(((Place) mEntity).address);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonCategory = extras.getString(Constants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						final Category categoryUpdated = (Category) Json.jsonToObject(jsonCategory, Json.ObjectType.CATEGORY);
						if (categoryUpdated != null) {
							mDirty = true;
							((Place) mEntity).category = categoryUpdated;
							((BuilderButton) findViewById(R.id.category)).setText(categoryUpdated.name);
							drawPhoto();
						}
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected boolean afterInsert() {
	    /*
	     * Only called if the insert was successful. Called on main ui thread.
         * We link replies to the places they are associated with. This give us the option
		 * to thread, flatten or do some combo. Called on background thread.
		 */
		if (!mEditing) {
			if (mTos.size() > 0) {

				mInsertedResId = R.string.alert_saved_and_shared;

				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mBusy.hideBusy(false);
						mBusy.showBusy(IBusy.BusyAction.ActionWithMessage, mInsertProgressResId);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncAutoSharePlace");

                    /* Create message entity */
						IEntityController controller = Patch.getInstance().getControllerForSchema(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);
						Entity message = controller.makeNew();
						message.description = String.format(StringManager.getString(R.string.label_place_share_body_self_oncreate), mEntity.name);
						message.type = Message.MessageType.SHARE;
						if (Patch.getInstance().getCurrentUser() != null) {
							message.creator = Patch.getInstance().getCurrentUser();
							message.creatorId = Patch.getInstance().getCurrentUser().id;
						}

                    /* Links */
						List<Link> links = new ArrayList<Link>();
						links.add(new Link(mEntity.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE));
						for (Entity to : mTos) {
							links.add(new Link(to.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER));
						}
						final ModelResult result = Patch.getInstance().getEntityManager().insertEntity(message, links, null, null, null, true);
						return result;
					}

					@Override
					protected void onPostExecute(Object response) {
						final ModelResult result = (ModelResult) response;
						mBusy.hideBusy(false);

						if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
							UI.showToastNotification(StringManager.getString(mInsertedResId), Toast.LENGTH_SHORT);
							setResultCode(Activity.RESULT_OK);
							finish();
							Patch.getInstance().getAnimationManager().doOverridePendingTransition(PlaceEdit.this, TransitionType.FORM_TO_PAGE);
						}
						else {
							Errors.handleError(PlaceEdit.this, result.serviceResponse);
						}
					}
				}.execute();

				return false; // Tells caller that we will handle finishing
			}
			else {
				Patch.dispatch.route(this, Route.BROWSE, mEntity, null, null);
			}
		}

		return true;
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
		Integer beaconMax = !mUntuning ? ServiceConstants.PROXIMITY_BEACON_COVERAGE : ServiceConstants.PROXIMITY_BEACON_UNCOVERAGE;
		final List<Beacon> beacons = ProximityManager.getInstance().getStrongestBeacons(beaconMax);
		final Beacon primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncTrackEntityProximity");

				final ModelResult result = Patch.getInstance().getEntityManager().trackEntity(mEntity
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

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;

		gather();
		Place place = (Place) mEntity;
		if (place.name == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_place_name)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override
	protected void gather() {
		super.gather();
		if (!mEditing) {
			((Place) mEntity).provider.aircandi = Patch.getInstance().getCurrentUser().id;
			/*
			 * Custom places get the current location.
			 * 
			 * Upsized places inherited a location from the place authority.
			 */
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
			if (location != null) {
				if (mEntity.location == null) {
					mEntity.location = new AirLocation();
				}
				mEntity.location.lat = location.lat;
				mEntity.location.lng = location.lng;
			}
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_PROXIMITY;
	}

	public void showTooltips() {

		if (mTooltips != null && !mTooltips.hasShot()) {
			mTooltips.setClickable(true);
			mTooltips.setVisibility(View.VISIBLE);
			mTooltips.clear();
			mTooltips.requestLayout();

			ToolTipView part1 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part1))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part1.setId(ViewId.getInstance().getUniqueId());
			part1.addRule(RelativeLayout.CENTER_HORIZONTAL);

			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) part1.getLayoutParams();
			params.setMargins(0, UI.getRawPixelsForDisplayPixels(40f), 0, 0);
			part1.setLayoutParams(params);

			ToolTipView part2 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part2))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part2.setId(ViewId.getInstance().getUniqueId());
			part2.setMinimumWidth(UI.getRawPixelsForDisplayPixels(250f));
			part2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part2.addRule(RelativeLayout.BELOW, part1.getId());

			ToolTipView part3 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part3))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part3.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part3.addRule(RelativeLayout.BELOW, part2.getId());
		}
	}

 	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		if (mTooltips != null) {
			View view = findViewById(R.id.tooltips);
			view.post(new Runnable() {
				@Override
				public void run() {
					showTooltips();
				}
			});
		}
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mTooltips != null) {
			mTooltips.hide(false);
		}
		return super.onOptionsItemSelected(item);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return (mLayoutResId != null && mLayoutResId != 0) ? mLayoutResId : R.layout.place_edit;
	}
}