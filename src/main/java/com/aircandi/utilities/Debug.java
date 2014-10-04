package com.aircandi.utilities;

import android.os.AsyncTask;
import android.widget.Toast;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Document;
import com.aircandi.objects.Log;
import com.aircandi.objects.Place;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Debug {

	public static void insertBeacon() {

		final Document document = new Document();
		if (Patch.getInstance().getCurrentPlace() != null) {

			Place place = (Place) Patch.getInstance().getCurrentPlace();
			Beacon beacon = place.getBeaconFromLink(Constants.TYPE_LINK_PROXIMITY, true);

			if (beacon != null) {

				document.name = place.name;
				document.type = "beacon";
				document.data = new HashMap<String, Object>();
				document.data.put("bssid", beacon.bssid);
				document.data.put("ssid", beacon.ssid);

				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncInsertReport");
						ModelResult result = Patch.getInstance().getEntityManager().insertDocument(document);
						return result;
					}

					@Override
					protected void onPostExecute(Object response) {
						final ModelResult result = (ModelResult) response;

						if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
							UI.showToastNotification("Beacon saved", Toast.LENGTH_SHORT);
						}
					}
				}.execute();
			}
			else {
				UI.showToastNotification("The place doesn't have any beacons", Toast.LENGTH_SHORT);
			}
		}
	}

	public static void insertLog(final String category, final String name, final String label, final Number value, final Object logSet) {

		if (((List) logSet).size() == 0) return;

		final Log log = new Log();

		log.category = category;
		log.name = name;
		log.label = label;
		log.value = value;
		log.data = new HashMap<String, Object>();
		log.data.put("detail", ((ArrayList) logSet).clone());

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncInsertReport");
				ModelResult result = Patch.getInstance().getEntityManager().insertLog(log);
				return result;
			}

		}.execute();
	}
}