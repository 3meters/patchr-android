package com.aircandi.ui.edit;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.StringManager;
import com.aircandi.components.TabManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Category;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Maps;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;
import com.tokenautocomplete.TokenCompleteTextView;

public class PlaceEdit extends BaseEntityEdit {

    private TabManager               mTabManager;
    private ComboButton              mButtonTune;
    private ComboButton              mButtonUntune;

    private Boolean      mTuned           = false;
    private Boolean      mUntuned         = false;
    private Boolean      mTuningInProcess = false;
    private Boolean      mUntuning        = false;
    private Boolean      mFirstTune       = true;

    @Override
    public void initialize(Bundle savedInstanceState) {
        super.initialize(savedInstanceState);

        mButtonTune = (ComboButton) findViewById(R.id.button_tune);
        mButtonUntune = (ComboButton) findViewById(R.id.button_untune);

        if (mEntity == null || (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)))) {
            ViewFlipper flipper = (ViewFlipper) findViewById(R.id.flipper_form);
            if (flipper != null) {
                mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
                mTabManager.initialize();
                mTabManager.doRestoreInstanceState(savedInstanceState);
            }
        }
    }

    @Override
    public void draw() {

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

        super.draw();
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

    @SuppressWarnings("ucd")
    public void onAddressBuilderClick(View view) {
        Aircandi.dispatch.route(this, Route.ADDRESS_EDIT, mEntity, null, null);
    }

    @SuppressWarnings("ucd")
    public void onCategoryBuilderClick(View view) {
        Aircandi.dispatch.route(this, Route.CATEGORY_EDIT, mEntity, null, null);
    }

    @SuppressWarnings("ucd")
    public void onApplinksBuilderClick(View view) {
        loadApplinks();
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
            else if (requestCode == Constants.ACTIVITY_APPLINKS_EDIT) {

                if (intent != null && intent.getExtras() != null) {
                    final Bundle extras = intent.getExtras();
                    final List<String> jsonApplinks = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
                    mApplinks.clear();
                    for (String jsonApplink : jsonApplinks) {
                        Applink applink = (Applink) Json.jsonToObject(jsonApplink, Json.ObjectType.ENTITY);
                        mApplinks.add(applink);
                    }
                    mDirty = true;
                    drawShortcuts(mEntity);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    // --------------------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------------------

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

                final ModelResult result = Aircandi.getInstance().getEntityManager().trackEntity(mEntity
                        , beacons
                        , primaryBeacon
                        , mUntuning);

                return result;
            }

            @Override
            protected void onPostExecute(Object response) {
                setSupportProgressBarIndeterminateVisibility(false);
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

    private void loadApplinks() {
		/*
		 * First, we need the real applinks to send them to the applinks editor
		 */
        if (mApplinks != null) {
            doApplinksBuilder();
        }
        else {
            new AsyncTask() {

                @Override
                protected void onPreExecute() {
                    mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_loading_applinks);
                }

                @Override
                protected Object doInBackground(Object... params) {
                    Thread.currentThread().setName("AsyncBindApplinks");

                    List<String> linkTypes = new ArrayList<String>();
                    List<String> schemas = new ArrayList<String>();
                    linkTypes.add(Constants.TYPE_LINK_CONTENT);
                    schemas.add(Constants.SCHEMA_ENTITY_APPLINK);

                    Cursor cursor = new Cursor()
                            .setLimit(Integers.getInteger(R.integer.page_size_applinks))
                            .setSort(Maps.asMap("modifiedDate", -1))
                            .setSkip(0)
                            .setToSchemas(schemas)
                            .setLinkTypes(linkTypes);

                    ModelResult result = Aircandi.getInstance().getEntityManager().loadEntitiesForEntity(mEntity.id, null, cursor, null);

                    return result;
                }

                @Override
                protected void onPostExecute(Object modelResult) {
                    final ModelResult result = (ModelResult) modelResult;
                    mBusy.hideBusy(false);
                    if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
                        mApplinks = (List<Entity>) result.data;
                        doApplinksBuilder();
                    }
                    else {
                        Errors.handleError(PlaceEdit.this, result.serviceResponse);
                    }
                }

            }.execute();
        }
    }

    public void doApplinksBuilder() {

		/* Serialize the applinks */
        Bundle extras = new Bundle();
        if (mApplinks.size() > 0) {
            final List<String> applinkStrings = new ArrayList<String>();
            for (Entity applink : mApplinks) {
                applinkStrings.add(Json.objectToJson(applink, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE));
            }
            extras.putStringArrayList(Constants.EXTRA_ENTITIES, (ArrayList<String>) applinkStrings);
        }

        extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_APPLINK);
        Aircandi.dispatch.route(this, Route.APPLINKS_EDIT, mEntity, null, extras);
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
            ((Place) mEntity).provider.aircandi = Aircandi.getInstance().getCurrentUser().id;
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

    ;

    // --------------------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------------------
    // Misc
    // --------------------------------------------------------------------------------------------

    @Override
    protected int getLayoutId() {
        return (mLayoutResId != null && mLayoutResId != 0) ? mLayoutResId : R.layout.place_edit;
    }
}