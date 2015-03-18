package com.aircandi.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.MapManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataReadyEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkSpecType;
import com.aircandi.objects.Patch;
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

		mLinkProfile = LinkSpecType.LINKS_FOR_PATCH;
		mCurrentFragment = new MapListFragment();
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	@Override
	public void draw(View view) {
		mFirstDraw = false;

		if ((mEntity instanceof Place || mEntity instanceof Patch) && mEntity.getLocation() != null) {
			if (mEntity instanceof Place) {
				final String address = ((Place) mEntity).getAddressString(true);
				if (((Place) mEntity).fuzzy && !TextUtils.isEmpty(address)) {

					new AsyncTask() {

						@Override
						protected void onPreExecute() {
							mUiController.getBusyController().show(BusyAction.Refreshing_Empty);
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
							mUiController.getBusyController().hide(false);

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
					}.executeOnExecutor(Constants.EXECUTOR);
					return;
				}
			}

			List<Entity> entities = new ArrayList<Entity>();
			entities.add(mEntity);
			((MapListFragment) mCurrentFragment)
					.setEntities(entities)
					.setZoomLevel(MapManager.ZOOM_SCALE_NEARBY)
					.draw();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataReady(DataReadyEvent event) {
		super.onDataReady(event);
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.map_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
}