package com.aircandi.ui;

import android.app.Fragment;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Place;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.utilities.Errors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapForm extends BaseEntityForm {

	private Fragment mFragment;

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

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					/* Show busy */
					mBusy.showBusy(BusyAction.Loading);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("AsyncMarkMapLocation");
					ModelResult result = new ModelResult();
					if (((Place) mEntity).fuzzy) {
						String address = ((Place) mEntity).getAddressString(true);
						if (!TextUtils.isEmpty(address)) {
							try {
								Geocoder geocoder = new Geocoder(Patch.applicationContext, Locale.getDefault());
								List<Address> addresses = geocoder.getFromLocationName(address, 1);
								if (addresses != null && addresses.size() > 0) {
									Address geolookup = addresses.get(0);
									if (geolookup.hasLatitude() && geolookup.hasLongitude()) {
										mEntity.location.lat = geolookup.getLatitude();
										mEntity.location.lng = geolookup.getLongitude();
										mEntity.location.accuracy = 25;
										mEntity.fuzzy = false;
									}
								}
							}
							catch (IOException exception) {
								result.serviceResponse.responseCode = ResponseCode.FAILED;
								result.serviceResponse.exception = exception;
								result.serviceResponse.errorResponse = Errors.getErrorResponse(Patch.applicationContext, result.serviceResponse);
							}
						}
					}
					return result;
				}

				@Override
				protected void onPostExecute(Object modelResult) {
					if (isFinishing()) return;

					final ModelResult result = (ModelResult) modelResult;
					mBusy.hideBusy(false);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

						List<Entity> entities = new ArrayList<Entity>();
						entities.add(mEntity);
						((MapListFragment) mFragment)
								.setEntities(entities)
								.setZoomLevel(MapListFragment.ZOOM_NEARBY)
								.draw();
					}
					else {
						Errors.handleError(MapForm.this, result.serviceResponse);
					}
				}
			}.execute();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void setCurrentFragment(String fragmentType) {
		mFragment = new MapListFragment();
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mFragment)
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