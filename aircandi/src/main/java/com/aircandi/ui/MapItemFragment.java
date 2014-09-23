package com.aircandi.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aircandi.components.Logger;
import com.aircandi.utilities.UI;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

public class MapItemFragment extends MapFragment implements GoogleMap.OnMarkerDragListener {

	protected GoogleMap      mMap;
	protected ClusterManager<MyClusterItem> mClusterManager;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);
		mMap = getMap();
		mClusterManager = new ClusterManager<MyClusterItem>(getActivity(), mMap);

		// Check if we were successful in obtaining the map.
		if (checkReady()) {
			setUpMap();
		}
		return root;
	}

	private boolean checkReady() {
		if (mMap == null) {
			UI.showToastNotification("map not ready", Toast.LENGTH_SHORT);
			return false;
		}
		return true;
	}

	private void setUpMap() {

		mMap.setMyLocationEnabled(true);
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.setLocationSource(null);
		mMap.setOnMarkerDragListener(this);

		UiSettings uiSettings = mMap.getUiSettings();

		uiSettings.setZoomControlsEnabled(true);
		uiSettings.setMyLocationButtonEnabled(true);
		uiSettings.setAllGesturesEnabled(true);
		uiSettings.setCompassEnabled(true);

	}

	@Override
	public void onMarkerDragStart(Marker marker) {

	}

	@Override
	public void onMarkerDrag(Marker marker) {

	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		// TODO Auto-generated method stub
		LatLng dragPosition = marker.getPosition();
		double dragLat = dragPosition.latitude;
		double dragLong = dragPosition.longitude;
		Logger.i(this, "on drag end :" + dragLat + " dragLong :" + dragLong);
		UI.showToastNotification("Marker dragged: "+ dragLat + " dragLong :" + dragLong, Toast.LENGTH_LONG);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public class MyClusterItem implements ClusterItem {
		private final LatLng mPosition;

		public MyClusterItem(double lat, double lng) {
			mPosition = new LatLng(lat, lng);
		}

		@Override
		public LatLng getPosition() {
			return mPosition;
		}
	}
}
