package com.aircandi.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.MapManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.utilities.Errors;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class MapForm extends BaseEntityForm {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;
		mNextFragmentTag = Constants.FRAGMENT_TYPE_MAP;
	}

	@Override
	public void draw(View view) {
		mFirstDraw = false;
		if (!TextUtils.isEmpty(mEntity.name)) {
			setActivityTitle(mEntity.name);
		}

		if (mEntity.getLocation() != null) {
			final String address = ((Place) mEntity).getAddressString(true);
			if (((Place) mEntity).fuzzy && !TextUtils.isEmpty(address)) {

				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mBusy.showBusy(BusyAction.Loading);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncMarkMapLocation");
						ModelResult result = LocationManager.getInstance().getLocationFromAddress(address);
						return result;
					}

					@Override
					protected void onPostExecute(Object modelResult) {
						if (isFinishing()) return;

						final ModelResult result = (ModelResult) modelResult;
						mBusy.hideBusy(false);

						if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

							AirLocation location = (AirLocation) result.data;
							mEntity.location = location;
							mEntity.fuzzy = false;

							List<Entity> entities = new ArrayList<Entity>();
							entities.add(mEntity);
							((MapListFragment) mCurrentFragment)
									.setEntities(entities)
									.setZoomLevel(MapManager.ZOOM_SCALE_NEARBY)
									.draw();
						}
						else {
							Errors.handleError(MapForm.this, result.serviceResponse);
						}
					}
				}.execute();
			}
			else {
				List<Entity> entities = new ArrayList<Entity>();
				entities.add(mEntity);
				((MapListFragment) mCurrentFragment)
						.setEntities(entities)
						.setZoomLevel(MapManager.ZOOM_SCALE_NEARBY)
						.draw();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {
		((MapListFragment) mCurrentFragment).onProcessingFinished();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void setCurrentFragment(String fragmentType) {
		mCurrentFragment = new MapListFragment();
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.navigate) {
			AirLocation location = mEntity.getLocation();
			String address = ((Place) mEntity).getAddressString(true);

			if (!((Place) mEntity).fuzzy) {
				address = null;
			}

			AndroidManager.getInstance().callMapNavigation(this
					, location.lat.doubleValue()
					, location.lng.doubleValue()
					, address
					, mEntity.name);
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.map_form;
	}
}